// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type Orientation = 'LANDSCAPE' | 'PORTRAIT'

const orientationTimeoutMs = 6000
const keyboardTimeoutMs = 5000

// Rotation around the screen axis in the WebDriverAgent rotation API. 90 is
// the landscape direction used when the stale visual viewport was reproduced.
const rotationZ: Record<Orientation, number> = { LANDSCAPE: 90, PORTRAIT: 0 }

async function postRotation(z: number) {
  const { protocol = 'http', hostname, port, path = '/' } = browser.options
  const base = `${protocol}://${hostname}:${port}${path.replace(/\/$/, '')}`
  const response = await fetch(
    `${base}/session/${browser.sessionId}/rotation`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ x: 0, y: 0, z })
    }
  )
  if (!response.ok) {
    throw new Error(
      `Rotation to z ${z} failed with HTTP ${response.status}: ${(await response.text()).slice(0, 300)}`
    )
  }
}

/**
 * Rotates the simulator and waits until the page has the new orientation.
 * `setOrientation` has failed with "Unable To Rotate Device" in a simulator
 * whose SpringBoard was restarted; the rotation API is tried next.
 */
export async function rotate(orientation: Orientation) {
  try {
    await browser.setOrientation(orientation)
  } catch (setOrientationError) {
    try {
      await postRotation(rotationZ[orientation])
    } catch (rotationError) {
      throw new Error(
        `Cannot rotate to ${orientation}: setOrientation failed (${String(setOrientationError)}), ${String(rotationError)}`
      )
    }
  }
  const landscape = orientation === 'LANDSCAPE'
  await browser.waitUntil(
    () =>
      browser.execute(
        (expectLandscape: boolean) =>
          window.innerWidth > window.innerHeight === expectLandscape,
        landscape
      ),
    {
      timeout: orientationTimeoutMs,
      interval: 150,
      timeoutMsg: `The page did not switch to ${orientation.toLowerCase()}`
    }
  )
}

/**
 * Waits until no text field has focus and the visual viewport is as tall as
 * the layout viewport again. A stale visual viewport never passes this.
 */
export async function waitForKeyboardClosed() {
  await browser.waitUntil(
    () =>
      browser.execute(() => {
        const viewport = window.visualViewport
        const focused = document.activeElement?.tagName
        return (
          focused !== 'INPUT' &&
          focused !== 'TEXTAREA' &&
          viewport !== null &&
          Math.abs(viewport.height - window.innerHeight) <= 1
        )
      }),
    {
      timeout: keyboardTimeoutMs,
      interval: 200,
      timeoutMsg: 'The software keyboard did not close'
    }
  )
}
