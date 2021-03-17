import { eq } from 'lodash'
import { differenceInSeconds } from 'date-fns'
import { Page } from 'playwright'

/**
 * Returns a promise that is resolved after the given amount of milliseconds
 *
 * @param ms
 */
export async function delay(ms: number): Promise<void> {
  return new Promise<void>((resolve) => setTimeout(resolve, ms))
}

const WAIT_TIMEOUT_SECONDS = 30
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

export class ElementSelector {
  constructor(protected page: Page, protected selector: string) {}

  /**
   * Returns the client-side bounding box of this element.
   *
   * If the element does not exist on the page, an error is thrown
   */
  get boundingBox(): Promise<BoundingBox> {
    return this.page
      .$eval(this.selector, (el) => {
        // DOMRect doesn't serialize nicely, so extract the individual fields
        const rect = el.getBoundingClientRect()
        const { x, y, width, height } = rect
        return { x, y, width, height }
      })
      .then((box) => new BoundingBox(box))
  }

  get visible(): Promise<boolean> {
    return this.page.isVisible(this.selector)
  }

  async click(): Promise<void> {
    await this.page.click(this.selector)
  }
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
