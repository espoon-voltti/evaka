// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'node:assert/strict'

import { decodePng } from './png'

const markerAttribute = 'data-ios-smoke-paint'
const minPaintedRatio = 0.95
const paintTimeoutMs = 5000
// Rapid screenshot requests have stalled WebDriverAgent for minutes
const paintRetryIntervalMs = 500

const selector = (dataQa: string) => `[data-qa="${dataQa}"]`

/**
 * A button is reachable when it is fully inside the viewport, nothing is on
 * top of it in hit testing, and it is actually painted on screen. The last
 * check is needed because iOS WebKit can paint a fixed element clipped by a
 * scrolling ancestor while hit testing and geometry still report it as
 * visible (PR #9840).
 */
export async function expectReachable(dataQa: string, within?: string) {
  const fullSelector = within
    ? `${selector(within)} ${selector(dataQa)}`
    : selector(dataQa)
  await $(fullSelector).waitForDisplayed()
  await expectInViewportAndOnTop(fullSelector)
  await expectPainted(fullSelector)
}

async function expectInViewportAndOnTop(fullSelector: string) {
  const result = await browser.execute((sel: string) => {
    const el = document.querySelector(sel)
    if (!el) return { found: false }
    const rect = el.getBoundingClientRect()
    const hit = document.elementFromPoint(
      rect.left + rect.width / 2,
      rect.top + rect.height / 2
    )
    const describe = (node: Element) => {
      const dataQa = node.closest('[data-qa]')?.getAttribute('data-qa')
      return `${node.tagName.toLowerCase()}${dataQa ? ` in [data-qa="${dataQa}"]` : ''}`
    }
    return {
      found: true,
      inViewport:
        rect.left >= 0 &&
        rect.top >= 0 &&
        rect.right <= window.innerWidth &&
        rect.bottom <= window.innerHeight,
      coveredBy:
        hit === null ? 'nothing' : el.contains(hit) ? null : describe(hit),
      rect: `${rect.left},${rect.top} ${rect.width}x${rect.height}`,
      viewport: `${window.innerWidth}x${window.innerHeight}`
    }
  }, fullSelector)
  assert.ok(result.found, `${fullSelector} not found`)
  assert.ok(
    result.inViewport,
    `${fullSelector} is not fully inside the viewport (rect ${result.rect}, viewport ${result.viewport})`
  )
  assert.equal(
    result.coveredBy,
    null,
    `${fullSelector} is covered by ${result.coveredBy} at its centre point`
  )
}

async function expectPainted(fullSelector: string) {
  const { painted, rect, dpr } = await measurePainted(
    fullSelector,
    (painted, rect, dpr) => paintedRatio(painted, rect, dpr) >= minPaintedRatio
  )
  const ratio = paintedRatio(painted, rect, dpr)
  assert.ok(
    ratio >= minPaintedRatio,
    `${fullSelector} is not painted on screen: ${Math.round(ratio * 100)}% of its ${Math.round(rect.width)}x${Math.round(rect.height)} area is visible, painted box ${Math.round(painted.width / dpr)}x${Math.round(painted.height / dpr)} (something is drawn over it or it is clipped)`
  )
}

export interface ScreenBox {
  left: number
  top: number
  width: number
  height: number
}

/**
 * Where the element is really drawn on the screen, in screen points from the
 * top left corner of the screenshot, or undefined if no part of it is drawn.
 * Unlike `getBoundingClientRect()` this does not trust the browser's idea of
 * where the viewport is.
 *
 * iOS 26 tints the status bar with the colour at the top or bottom edge of the
 * page, so a magenta element can turn the status bar magenta too. Where the
 * status bar is outside the page, pass its height as `ignoreAbove`.
 */
export async function paintedBox(
  cssSelector: string,
  { ignoreAbove = 0 }: { ignoreAbove?: number } = {}
): Promise<ScreenBox | undefined> {
  const { painted, dpr } = await measurePainted(
    cssSelector,
    (painted) => painted.count > 0,
    ignoreAbove
  )
  return painted.count === 0
    ? undefined
    : {
        left: painted.minX / dpr,
        top: painted.minY / dpr,
        width: painted.width / dpr,
        height: painted.height / dpr
      }
}

export const formatBox = (box: ScreenBox | undefined) =>
  box
    ? `${Math.round(box.left)},${Math.round(box.top)} ${Math.round(box.width)}x${Math.round(box.height)} (bottom ${Math.round(box.top + box.height)})`
    : 'not painted'

interface ElementSize {
  width: number
  height: number
}

const paintedRatio = (painted: Magenta, rect: ElementSize, dpr: number) => {
  const expectedWidth = Math.round(rect.width * dpr)
  const expectedHeight = Math.round(rect.height * dpr)
  const expectedArea = expectedWidth * expectedHeight
  return expectedArea > 0
    ? Math.min(
        painted.count / expectedArea,
        painted.width / expectedWidth,
        painted.height / expectedHeight
      )
    : 0
}

/**
 * Colours the element magenta and takes screenshots until `isDone` accepts the
 * measurement or the paint timeout passes
 */
async function measurePainted(
  cssSelector: string,
  isDone: (painted: Magenta, rect: ElementSize, dpr: number) => boolean,
  ignoreAbove = 0
) {
  const { width, height, dpr } = await browser.execute(
    async (sel: string, attribute: string) => {
      const style = document.createElement('style')
      style.setAttribute(attribute, '')
      style.textContent = `[${attribute}], [${attribute}] * {
        background: #f0f !important; color: #f0f !important;
        border-color: #f0f !important; fill: #f0f !important;
        stroke: #f0f !important; outline: none !important;
        box-shadow: none !important; text-shadow: none !important;
        border-radius: 0 !important; opacity: 1 !important;
        transition: none !important; filter: none !important;
      }`
      document.head.appendChild(style)
      const el = document.querySelector(sel)
      el?.setAttribute(attribute, '')
      await new Promise((resolve) =>
        requestAnimationFrame(() => requestAnimationFrame(resolve))
      )
      const rect = el?.getBoundingClientRect()
      return {
        width: rect?.width ?? 0,
        height: rect?.height ?? 0,
        dpr: window.devicePixelRatio
      }
    },
    cssSelector,
    markerAttribute
  )
  const rect = { width, height }
  let painted = noMagenta
  try {
    const deadline = Date.now() + paintTimeoutMs
    let attempt = 0
    do {
      if (attempt++ > 0) await browser.pause(paintRetryIntervalMs)
      painted = measureMagenta(
        await browser.takeScreenshot(),
        Math.round(ignoreAbove * dpr)
      )
    } while (!isDone(painted, rect, dpr) && Date.now() < deadline)
  } finally {
    await browser.execute(async (attribute: string) => {
      document
        .querySelectorAll(`[${attribute}]`)
        .forEach((node) =>
          node.tagName === 'STYLE'
            ? node.remove()
            : node.removeAttribute(attribute)
        )
      await new Promise((resolve) =>
        requestAnimationFrame(() => requestAnimationFrame(resolve))
      )
    }, markerAttribute)
    // Screenshots can lag behind painting: the next measurement must not see
    // this element still magenta
    await browser.pause(paintRetryIntervalMs)
  }
  return { painted, rect, dpr }
}

interface Magenta {
  count: number
  minX: number
  minY: number
  width: number
  height: number
}

const noMagenta: Magenta = { count: 0, minX: 0, minY: 0, width: 0, height: 0 }

// The screen coordinates of the web viewport are unknown, so the painted
// area is measured as the bounding box of the magenta pixels on the whole
// screen below row `fromRow`; the box shrinks when the element is clipped
function measureMagenta(screenshotBase64: string, fromRow = 0): Magenta {
  const { width, pixels, channels } = decodePng(
    Buffer.from(screenshotBase64, 'base64')
  )
  let count = 0
  let minX = Infinity
  let maxX = -Infinity
  let minY = Infinity
  let maxY = -Infinity
  for (let i = fromRow * width * channels; i < pixels.length; i += channels) {
    if (pixels[i] > 230 && pixels[i + 1] < 40 && pixels[i + 2] > 230) {
      const pixel = i / channels
      const x = pixel % width
      const y = (pixel - x) / width
      count++
      minX = Math.min(minX, x)
      maxX = Math.max(maxX, x)
      minY = Math.min(minY, y)
      maxY = Math.max(maxY, y)
    }
  }
  return count === 0
    ? noMagenta
    : {
        count,
        minX,
        minY,
        width: maxX - minX + 1,
        height: maxY - minY + 1
      }
}
