// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { differenceInSeconds } from 'date-fns'
import isEqual from 'lodash/isEqual'
import { BaseError } from 'make-error-cause'

import config from '../config'

/**
 * Returns a promise that is resolved after the given amount of milliseconds
 *
 * @param ms
 */
export async function delay(ms: number): Promise<void> {
  return new Promise<void>((resolve) => setTimeout(resolve, ms))
}

const WAIT_TIMEOUT_SECONDS = config.playwright.headless ? 30 : 5
const WAIT_LOOP_INTERVAL_MS = 100

export class WaitTimeout extends BaseError {}

/**
 * Waits until the given async function returns a value that passes the given condition.
 *
 * The assertion function should otherwise have the same semantics as the condition function, but should throw an
 * exception instead of returning false. The only purpose of the assertion function is to give better error messages:
 *   "expected foo, got bar"
 *   vs
 *   "WaitTimeout"
 */
export async function waitForCondition<T, O extends T>(
  f: () => Promise<T>,
  condition: (value: T) => value is O,
  assertion: (value: T) => void
): Promise<O>
export async function waitForCondition<T>(
  f: () => Promise<T>,
  condition: (value: T) => boolean,
  assertion: (value: T) => void
): Promise<T>
export async function waitForCondition<T>(
  f: () => Promise<T>,
  condition: (value: T) => boolean,
  assertion: (value: T) => void
): Promise<T> {
  const startTimestamp = new Date()

  let value = await f()
  while (!condition(value)) {
    await delay(WAIT_LOOP_INTERVAL_MS)
    const now = new Date()
    if (differenceInSeconds(now, startTimestamp) > WAIT_TIMEOUT_SECONDS) {
      assertion(value)
      // fallback in case condition/assertion had different semantics for some reason
      // and assertion(...) did not throw
      throw new WaitTimeout()
    }
    value = await f()
  }
  return value
}

export async function waitUntilDefined<T>(
  f: () => Promise<T>
): Promise<NonNullable<T>> {
  return waitForCondition(
    f,
    (value): value is NonNullable<T> => value != null,
    (value) => {
      expect(value).toBeDefined()
    }
  )
}

/**
 * Waits until the given function returns a promise which resolves to the given expected value.
 */
export async function waitUntilEqual<T>(
  f: () => Promise<T>,
  expected: T
): Promise<T> {
  return waitForCondition(
    f,
    (value) => isEqual(value, expected),
    (value) => expect(value).toEqual(expected)
  )
}

/**
 * Waits until the given function returns a promise which resolves to other than the given expected value.
 */
export async function waitUntilNotEqual<T>(
  f: () => Promise<T>,
  expected: T
): Promise<T> {
  return waitForCondition(
    f,
    (value) => !isEqual(value, expected),
    (value) => expect(value).not.toEqual(expected)
  )
}

/**
 * Waits until the given function returns a promise which resolves to true
 */
export async function waitUntilTrue(f: () => Promise<boolean>) {
  return waitForCondition(
    f,
    (value) => value,
    (value) => expect(value).toStrictEqual(true)
  )
}

/**
 * Waits until the given function returns a promise which resolves to false
 */
export async function waitUntilFalse(f: () => Promise<boolean>) {
  return waitForCondition(
    f,
    (value) => !value,
    (value) => expect(value).toStrictEqual(false)
  )
}

/**
 * Bounding box of an element.
 *
 * Roughly equivalent to {@type DOMRect}.
 */
export class BoundingBox {
  readonly x: number
  readonly y: number
  readonly width: number
  readonly height: number

  constructor(value: { x: number; y: number; width: number; height: number }) {
    this.x = value.x
    this.y = value.y
    this.width = value.width
    this.height = value.height
  }

  get left(): number {
    return this.x
  }

  get right(): number {
    return this.x + this.width
  }

  get top(): number {
    return this.y
  }

  get bottom(): number {
    return this.y + this.height
  }

  contains(other: BoundingBox): boolean {
    const h = this.left <= other.left && other.right <= this.right
    const v = this.top <= other.top && other.bottom <= this.bottom
    return h && v
  }
}

/**
 * Converts the given string into a valid single-quoted CSS string by escaping problematic characters and wrapping the content in single quotes.
 *
 * See https://developer.mozilla.org/en-US/docs/Web/CSS/string
 */
export function toCssString(text: string): string {
  return [
    "'",
    ...Array.from(text).map((codepoint) => {
      switch (codepoint) {
        case '\\':
          return '\\\\'
        case "'":
          return "\\'"
        case '\n':
          return '\\A'
        default:
          return codepoint
      }
    }),
    "'"
  ].join('')
}
