// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { eq } from 'lodash'
import { differenceInSeconds } from 'date-fns'
import config from 'e2e-test-common/config'

/**
 * Returns a promise that is resolved after the given amount of milliseconds
 *
 * @param ms
 */
export async function delay(ms: number): Promise<void> {
  return new Promise<void>((resolve) => setTimeout(resolve, ms))
}

const WAIT_TIMEOUT_SECONDS = config.playwright.developmentMode ? 5 : 30
const WAIT_LOOP_INTERVAL_MS = 100

/**
 * Waits until the given function returns a promise which resolves to the given expected value.
 */
export async function waitUntilEqual<T>(f: () => Promise<T>, expected: T) {
  const startTimestamp = new Date()

  while (!eq(await f(), expected)) {
    await delay(WAIT_LOOP_INTERVAL_MS)
    const now = new Date()
    if (differenceInSeconds(now, startTimestamp) > WAIT_TIMEOUT_SECONDS) {
      throw new Error('Wait timeout')
    }
  }
}

/**
 * Waits until the given function returns a promise which resolves to true
 */
export async function waitUntilTrue(f: () => Promise<boolean>) {
  await waitUntilEqual(f, true)
}

/**
 * Waits until the given function returns a promise which resolves to false
 */
export async function waitUntilFalse(f: () => Promise<boolean>) {
  await waitUntilEqual(f, false)
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
