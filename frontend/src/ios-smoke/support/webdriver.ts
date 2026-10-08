// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { spawn } from 'node:child_process'
import { mkdirSync } from 'node:fs'
import path from 'node:path'

export const appiumHome = path.resolve('.appium')
const appiumPort = 4723
const serverUrl = `http://127.0.0.1:${appiumPort}`
const serverStartTimeoutMs = 60000

export const sleep = (ms: number) =>
  new Promise<void>((resolve) => setTimeout(resolve, ms))

export async function waitUntil<T>(
  condition: () => Promise<T>,
  {
    timeout = 15000,
    interval = 500,
    message
  }: { timeout?: number; interval?: number; message: string }
): Promise<Exclude<T, false | null | undefined>> {
  const deadline = Date.now() + timeout
  for (;;) {
    const value = await condition()
    if (value) return value as Exclude<T, false | null | undefined>
    if (Date.now() > deadline) throw new Error(message)
    await sleep(interval)
  }
}

interface WebDriverResponse {
  value: unknown
}

async function request(
  method: 'GET' | 'POST' | 'DELETE',
  urlPath: string,
  body?: unknown
): Promise<unknown> {
  const response = await fetch(serverUrl + urlPath, {
    method,
    headers: { 'Content-Type': 'application/json' },
    ...(body === undefined ? {} : { body: JSON.stringify(body) })
  })
  const { value } = (await response.json()) as WebDriverResponse
  if (!response.ok) {
    const error = value as { error?: string; message?: string }
    throw new Error(
      `${method} ${urlPath} failed: ${error.error ?? response.status} ${error.message?.split('\n')[0] ?? ''}`
    )
  }
  return value
}

/**
 * Runs the Appium server installed in `.appium` (see `yarn ios-smoke:setup`)
 * until the returned function is called
 */
export async function startAppium(logDir: string): Promise<() => void> {
  mkdirSync(logDir, { recursive: true })
  const server = spawn(
    path.join(appiumHome, 'node_modules/.bin/appium'),
    [
      'server',
      '--port',
      String(appiumPort),
      '--log',
      path.join(logDir, 'appium.log'),
      '--log-timestamp'
    ],
    { env: { ...process.env, APPIUM_HOME: appiumHome }, stdio: 'ignore' }
  )
  await waitUntil(
    () =>
      fetch(`${serverUrl}/status`).then(
        (response) => response.ok,
        () => false
      ),
    {
      timeout: serverStartTimeoutMs,
      message: `Appium did not start on port ${appiumPort}: run yarn ios-smoke:setup`
    }
  )
  return () => server.kill()
}

export interface Context {
  id: string
  url?: string
}

export type Point = [number, number]

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type PageFunction<T> = (...args: any[]) => T

/** The W3C WebDriver commands the tests need, on one Appium session */
export class Session {
  private constructor(private readonly base: string) {}

  static async create(capabilities: Record<string, unknown>) {
    const { sessionId } = (await request('POST', '/session', {
      capabilities: { alwaysMatch: capabilities }
    })) as { sessionId: string }
    return new Session(`/session/${sessionId}`)
  }

  private call(
    method: 'GET' | 'POST' | 'DELETE',
    urlPath = '',
    body?: unknown
  ) {
    return request(method, this.base + urlPath, body)
  }

  delete() {
    return this.call('DELETE')
  }

  setScriptTimeout(ms: number) {
    return this.call('POST', '/timeouts', { script: ms })
  }

  /** Runs the function in the page of the current webview context */
  async execute<T>(fn: PageFunction<T>, ...args: unknown[]): Promise<T> {
    return (await this.call('POST', '/execute/sync', {
      script: `return (${fn.toString()}).apply(null, arguments)`,
      args
    })) as T
  }

  /** Like `execute`, but the function's promise is awaited in the page */
  async executeAsync<T>(fn: PageFunction<Promise<T>>, ...args: unknown[]) {
    return (await this.call('POST', '/execute/async', {
      script: `const done = arguments[arguments.length - 1]; (${fn.toString()}).apply(null, Array.prototype.slice.call(arguments, 0, -1)).then(done, (e) => done({ error: String(e) }))`,
      args
    })) as T | { error: string }
  }

  /** An Appium `mobile:` command, which runs in the native layer */
  mobile(command: string, args: Record<string, unknown> = {}) {
    return this.call('POST', '/execute/sync', {
      script: `mobile: ${command}`,
      args: [args]
    })
  }

  tap([x, y]: Point) {
    return this.mobile('tap', { x, y })
  }

  /** A touch drag from one screen point to another, as a finger would do it */
  drag(from: Point, to: Point, ms: number) {
    return this.call('POST', '/actions', {
      actions: [
        {
          type: 'pointer',
          id: 'finger',
          parameters: { pointerType: 'touch' },
          actions: [
            { type: 'pointerMove', duration: 0, x: from[0], y: from[1] },
            { type: 'pointerDown', button: 0 },
            { type: 'pointerMove', duration: ms, x: to[0], y: to[1] },
            { type: 'pointerUp', button: 0 }
          ]
        }
      ]
    })
  }

  async contexts() {
    return (await this.call('GET', '/contexts')) as (string | Context)[]
  }

  switchContext(name: string) {
    return this.call('POST', '/context', { name })
  }

  navigate(url: string) {
    return this.call('POST', '/url', { url })
  }

  setOrientation(orientation: 'LANDSCAPE' | 'PORTRAIT') {
    return this.call('POST', '/orientation', { orientation })
  }

  async screenshot() {
    return Buffer.from(
      (await this.call('GET', '/screenshot')) as string,
      'base64'
    )
  }

  /** The id of the first matching element, or undefined */
  async findElement(
    value: string,
    using: 'css selector' | 'accessibility id' = 'css selector'
  ) {
    try {
      const element = (await this.call('POST', '/element', {
        using,
        value
      })) as Record<string, string>
      return Object.values(element)[0]
    } catch (e) {
      if (String(e).includes('no such element')) return undefined
      throw e
    }
  }

  async isDisplayed(elementId: string) {
    return (await this.call('GET', `/element/${elementId}/displayed`)) === true
  }

  clickElement(elementId: string) {
    return this.call('POST', `/element/${elementId}/click`)
  }

  /** Clicks the element when it is displayed. A synthetic click in a webview. */
  click(selector: string) {
    return this.withDisplayed(selector, (elementId) =>
      this.clickElement(elementId)
    )
  }

  type(selector: string, text: string) {
    return this.withDisplayed(selector, (elementId) =>
      this.call('POST', `/element/${elementId}/value`, { text })
    )
  }

  waitForDisplayed(selector: string, timeout = 15000) {
    return this.withDisplayed(selector, () => Promise.resolve(), timeout)
  }

  /**
   * Finds the element, waits until it is displayed and runs the action on
   * it. The page may re-render between these steps, so a stale element
   * reference starts over.
   */
  private async withDisplayed(
    selector: string,
    action: (elementId: string) => Promise<unknown>,
    timeout = 15000
  ) {
    return waitUntil(
      async () => {
        const elementId = await this.findElement(selector)
        if (!elementId) return undefined
        try {
          const displayed = await this.call(
            'GET',
            `/element/${elementId}/displayed`
          )
          if (displayed !== true) return undefined
          await action(elementId)
          return elementId
        } catch (e) {
          if (String(e).includes('stale element reference')) return undefined
          throw e
        }
      },
      { timeout, message: `${selector} is not displayed` }
    )
  }

  waitForGone(selector: string, timeout = 15000) {
    return waitUntil(async () => !(await this.findElement(selector)), {
      timeout,
      message: `${selector} did not disappear`
    })
  }
}
