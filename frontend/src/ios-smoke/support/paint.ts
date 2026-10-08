// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { decodePng } from './png'
import type { Session } from './webdriver'
import { sleep } from './webdriver'

const markerAttribute = 'data-ios-smoke-paint'
const paintTimeoutMs = 5000
// Rapid screenshot requests have stalled WebDriverAgent for minutes
const paintRetryIntervalMs = 500

export interface ScreenBox {
  left: number
  top: number
  width: number
  height: number
}

export interface PaintResult {
  /** Where the element is drawn, in screen points, or undefined if nowhere */
  box: ScreenBox | undefined
  /** How much of the element's layout area is drawn, 0..1 */
  ratio: number
  /** The screen size in points */
  screen: { width: number; height: number }
}

export const formatBox = (box: ScreenBox | undefined) =>
  box
    ? `${Math.round(box.left)},${Math.round(box.top)} ${Math.round(box.width)}x${Math.round(box.height)} (bottom ${Math.round(box.top + box.height)})`
    : 'not painted'

/**
 * Where the element is really drawn on the screen, measured from a simulator
 * screenshot: the element is coloured magenta and the bounding box of the
 * magenta pixels is taken. Unlike `getBoundingClientRect()` this does not
 * trust the browser's idea of where the viewport is, and unlike
 * `elementFromPoint()` it notices when iOS WebKit paints a fixed element
 * clipped by a scrolling ancestor.
 *
 * iOS 26 tints the status bar with the colour at the top edge of the page,
 * so where the status bar is outside the page its height is passed as
 * `ignoreAbove`.
 */
export async function painted(
  session: Session,
  cssSelector: string,
  {
    ignoreAbove = 0,
    minRatio = 0
  }: { ignoreAbove?: number; minRatio?: number } = {}
): Promise<PaintResult> {
  const size = await session.executeAsync(
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
  if ('error' in size)
    throw new Error(`Cannot colour ${cssSelector}: ${size.error}`)
  const { width, height, dpr } = size
  const expectedArea = Math.round(width * dpr) * Math.round(height * dpr)
  let result: PaintResult = {
    box: undefined,
    ratio: 0,
    screen: { width: 0, height: 0 }
  }
  try {
    const deadline = Date.now() + paintTimeoutMs
    for (let attempt = 0; ; attempt++) {
      if (attempt > 0) await sleep(paintRetryIntervalMs)
      const { magenta, screen } = measureMagenta(
        await session.screenshot(),
        Math.round(ignoreAbove * dpr)
      )
      result = {
        screen: { width: screen.width / dpr, height: screen.height / dpr },
        box: magenta && {
          left: magenta.minX / dpr,
          top: magenta.minY / dpr,
          width: magenta.width / dpr,
          height: magenta.height / dpr
        },
        ratio:
          magenta && expectedArea > 0
            ? Math.min(
                magenta.count / expectedArea,
                magenta.width / Math.round(width * dpr),
                magenta.height / Math.round(height * dpr)
              )
            : 0
      }
      if ((result.box && result.ratio >= minRatio) || Date.now() > deadline) {
        return result
      }
    }
  } finally {
    await session.execute((attribute: string) => {
      document
        .querySelectorAll(`[${attribute}]`)
        .forEach((node) =>
          node.tagName === 'STYLE'
            ? node.remove()
            : node.removeAttribute(attribute)
        )
    }, markerAttribute)
    // Screenshots can lag behind painting: the next measurement must not see
    // this element still magenta
    await sleep(paintRetryIntervalMs)
  }
}

interface Magenta {
  count: number
  minX: number
  minY: number
  width: number
  height: number
}

// The screen coordinates of the web viewport are unknown, so the painted
// area is measured on the whole screen below row `fromRow`
function measureMagenta(screenshot: Buffer, fromRow: number) {
  const { width, height, pixels, channels } = decodePng(screenshot)
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
  const magenta: Magenta | undefined =
    count === 0
      ? undefined
      : { count, minX, minY, width: maxX - minX + 1, height: maxY - minY + 1 }
  return { magenta, screen: { width, height } }
}
