// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mkdirSync, writeFileSync } from 'node:fs'
import path from 'node:path'

import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'

import {
  createFamilyWithMessaging,
  createFamilyWithPlacement,
  testAdult
} from './support/fixtures'
import { formatBox, painted } from './support/paint'
import type { PaintResult } from './support/paint'
import { ensureSimulatorBooted } from './support/simulator'
import {
  formatMeasurement,
  scrollCalendar,
  selectors,
  triggerStaleViewport,
  waitForKeyboardClosed
} from './support/stale-viewport'
import { launchWebClip, openInWebClip, seedWebClip } from './support/webclip'
import { Session, appiumHome, sleep, startAppium } from './support/webdriver'

const resultsDir = 'ios-smoke-results'
const minPaintedRatio = 0.95
const tolerancePx = 2
const maxStaleViewportLoops = 3

let stopAppium: () => void
let session: Session
// The page starts below the status bar in portrait
let statusBarHeight: number

beforeAll(
  async () => {
    const udid = ensureSimulatorBooted()
    seedWebClip(udid)
    stopAppium = await startAppium(resultsDir)
    // No browserName or app: the session starts on the home screen and the
    // web clip is opened by tapping its icon
    session = await Session.create({
      platformName: 'iOS',
      'appium:automationName': 'XCUITest',
      'appium:udid': udid,
      'appium:usePreinstalledWDA': true,
      'appium:prebuiltWDAPath': path.join(
        appiumHome,
        'wda/WebDriverAgentRunner-Runner.app'
      ),
      'appium:fullContextList': true,
      'appium:additionalWebviewBundleIds': ['com.apple.webapp', '*'],
      'appium:includeSafariInWebviews': true,
      'appium:webviewConnectTimeout': 20000,
      'appium:newCommandTimeout': 180,
      // WebDriverAgent screenshots are HEIC by default, which blurs the edges
      // that the paint check counts
      'appium:settings[screenshotQuality]': 0
    })
    await session.setScriptTimeout(30000)
    await launchWebClip(session)
    statusBarHeight = await session.execute(
      () => Math.max(screen.width, screen.height) - window.innerHeight
    )
  },
  10 * 60 * 1000
)

afterAll(async () => {
  await session?.delete().catch(() => undefined)
  stopAppium?.()
})

afterEach(async ({ task }) => {
  if (task.result?.state === 'fail' && session) {
    mkdirSync(resultsDir, { recursive: true })
    const file = `${resultsDir}/${task.name.replace(/[^a-z0-9]+/gi, '-')}.png`
    writeFileSync(file, await session.screenshot())
  }
})

const waitForCalendar = () =>
  session.waitForDisplayed('[data-qa="calendar-page"][data-isloading="false"]')

const close = (a: number, b: number) => Math.abs(a - b) <= tolerancePx

/**
 * What the user sees: where the element is drawn on the screen, measured from
 * a screenshot (see support/paint.ts)
 */
const onScreen = (selector: string, minRatio = 0) =>
  painted(session, selector, { ignoreAbove: statusBarHeight, minRatio })

const drawnBox = (name: string, { box }: PaintResult) => {
  expect(box, `${name} is not drawn on the screen at all`).toBeDefined()
  return box!
}

/** The header must sit right below the status bar, across the screen */
async function expectHeaderBelowStatusBar(when: string) {
  const result = await onScreen(selectors.header)
  const box = drawnBox('the header', result)
  expect(
    close(box.top, statusBarHeight) && close(box.width, result.screen.width),
    `${when} the header is drawn at ${formatBox(box)}, expected it right below the status bar (${statusBarHeight}) across the ${result.screen.width} wide screen`
  ).toBe(true)
}

/** The bottom navigation must sit at the bottom edge of the screen */
async function expectNavigationAtScreenBottom(when: string) {
  const result = await onScreen(selectors.nav)
  const box = drawnBox('the bottom navigation', result)
  const { width, height } = result.screen
  expect(
    close(box.top + box.height, height) && close(box.width, width),
    `${when} the bottom navigation is drawn at ${formatBox(box)}, expected it at the bottom of the ${width}x${height} screen`
  ).toBe(true)
}

/**
 * The button must be drawn whole on the screen and nothing may be on top of
 * it. iOS WebKit can draw a fixed modal clipped by the app's scrolling area
 * while geometry and hit testing still report the button as visible (PR
 * #9975), so the drawn area is what counts.
 */
async function expectButtonOnScreen(dataQa: string) {
  const selector = `[data-qa="${dataQa}"]`
  await session.waitForDisplayed(selector)
  const { box, ratio } = await onScreen(selector, minPaintedRatio)
  expect(
    ratio,
    `${selector} is not drawn whole on the screen: ${Math.round(ratio * 100)}% of it is visible, drawn area ${formatBox(box)} (clipped or something drawn over it)`
  ).toBeGreaterThanOrEqual(minPaintedRatio)
  const coveredBy = await session.execute((sel: string) => {
    const el = document.querySelector(sel)!
    const rect = el.getBoundingClientRect()
    const hit = document.elementFromPoint(
      rect.left + rect.width / 2,
      rect.top + rect.height / 2
    )
    return hit === null
      ? 'nothing'
      : el.contains(hit)
        ? null
        : `${hit.tagName.toLowerCase()} in [data-qa="${hit.closest('[data-qa]')?.getAttribute('data-qa')}"]`
  }, selector)
  expect(
    coveredBy,
    `${selector} is covered by ${coveredBy} at its centre point`
  ).toBeNull()
}

// The tests run the app as a real home screen web clip, where iOS sets
// `navigator.standalone` and the app lays itself out as an app shell. The
// Playwright suite runs Chromium only, which does not reproduce the iOS
// WebKit layout bugs these tests are for.
describe('Citizen app installed on the iOS home screen', () => {
  it('calendar modal buttons are drawn whole on the screen', async () => {
    await createFamilyWithPlacement()
    await openInWebClip(session, testAdult, '/calendar')
    await waitForCalendar()
    expect(
      await session.execute(
        () => (navigator as Navigator & { standalone?: boolean }).standalone
      ),
      'The clip does not run as a standalone app'
    ).toBe(true)
    await expectHeaderBelowStatusBar('on the calendar')
    await expectNavigationAtScreenBottom('on the calendar')

    for (const action of [
      'calendar-action-reservations',
      'calendar-action-absences'
    ]) {
      await session.click('[data-qa="open-calendar-actions-modal"]')
      await session.click(`[data-qa="${action}"]`)
      await expectButtonOnScreen('modal-cancelBtn')
      await expectButtonOnScreen('modal-okBtn')
      await session.click('[data-qa="modal-cancelBtn"]')
      await session.waitForGone('[data-qa="modal-cancelBtn"]')
    }
  })

  async function sendNewMessage() {
    await session.click('[data-qa="new-message-btn-mobile"]')
    await session.waitForDisplayed('[data-qa="message-editor"]')
    await session.click('[data-qa="select-recipient"] input[type="text"]')
    await session.click('[data-qa="select-recipient"] [data-qa="option"]')
    await session.type('[data-qa="input-title"]', 'Viewport test')
    await session.type('[data-qa="input-content"]', 'A thread to reply to')
    await session.click('[data-qa="send-message-btn"]')
    await session.waitForGone('[data-qa="message-editor"]')
    await session.waitForDisplayed(selectors.threadListItem)
    await waitForKeyboardClosed(session)
  }

  // iOS WebKit can leave the visual viewport stale in a home screen app after
  // the software keyboard closes and the device rotates, and it resolves
  // `position: fixed` against it (WebKit bugs 254861 and 297779): the user
  // saw the header and the bottom navigation in the middle of the screen.
  // The app shell layout of PR #9798 keeps them in place anyway. This test
  // drives the simulator into the stale state and checks that.
  it(
    'header and bottom navigation stay in place with a stale visual viewport',
    async () => {
      await createFamilyWithMessaging()
      // The push notification suggestion is a banner above the scroll area
      // that leaves no room for the reply editor in landscape
      await openInWebClip(session, testAdult, '/messages', {
        cookies: [`evaka-pwa-push-suggestion-dismissal-${testAdult.id}=true`]
      })
      await session.waitForDisplayed('[data-qa="new-message-btn-mobile"]')
      await sendNewMessage()

      await session.click(selectors.navCalendar)
      await waitForCalendar()
      await sleep(1000)
      await expectHeaderBelowStatusBar('before the stale viewport')
      await expectNavigationAtScreenBottom('before the stale viewport')

      const { reached, loops } = await triggerStaleViewport(
        session,
        maxStaleViewportLoops
      )
      const measured = loops
        .map((m, i) => `loop ${i + 1}: ${formatMeasurement(m)}`)
        .join('\n  ')
      expect(
        reached,
        `The WebKit precondition was not reached: the visual viewport did not go stale in ${maxStaleViewportLoops} loops, so the layout was not tested.\n  ${measured}`
      ).toBe(true)

      await expectHeaderBelowStatusBar(
        'with a stale viewport after rotating back'
      )
      await expectNavigationAtScreenBottom(
        'with a stale viewport after rotating back'
      )
      await scrollCalendar(session)
      await expectHeaderBelowStatusBar(
        'with a stale viewport after scrolling the calendar'
      )
      await expectNavigationAtScreenBottom(
        'with a stale viewport after scrolling the calendar'
      )
    },
    10 * 60 * 1000
  )
})
