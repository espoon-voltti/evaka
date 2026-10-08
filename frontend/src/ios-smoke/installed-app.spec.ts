// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mkdirSync, writeFileSync } from 'node:fs'

import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'

import {
  createFamilyWithMessaging,
  createFamilyWithPlacement,
  testAdult
} from './support/fixtures'
import { formatBox, painted } from './support/paint'
import type { ScreenBox } from './support/paint'
import { ensureSimulatorBooted, terminateApp } from './support/simulator'
import {
  formatMeasurement,
  measureViewport,
  scrollCalendar,
  selectors,
  triggerStaleViewport,
  waitForKeyboardClosed
} from './support/stale-viewport'
import type { Rect, ViewportMeasurement } from './support/stale-viewport'
import { launchWebClip, openInWebClip, seedWebClip } from './support/webclip'
import { Session, sleep, startAppium } from './support/webdriver'

const resultsDir = 'ios-smoke-results'
const minPaintedRatio = 0.95
const tolerancePx = 2
const maxStaleViewportLoops = 3

let stopAppium: () => void
let session: Session

beforeAll(
  async () => {
    const udid = ensureSimulatorBooted()
    terminateApp(udid, 'com.apple.webapp')
    seedWebClip(udid)
    stopAppium = await startAppium(resultsDir)
    // No browserName or app: the session starts on the home screen and the
    // web clip is opened by tapping its icon
    session = await Session.create({
      platformName: 'iOS',
      'appium:automationName': 'XCUITest',
      'appium:udid': udid,
      'appium:fullContextList': true,
      'appium:additionalWebviewBundleIds': ['com.apple.webapp', '*'],
      'appium:includeSafariInWebviews': true,
      'appium:webviewConnectTimeout': 20000,
      'appium:newCommandTimeout': 180,
      'appium:wdaLaunchTimeout': 240000,
      // WebDriverAgent screenshots are HEIC by default, which blurs the edges
      // that the paint check counts
      'appium:settings[screenshotQuality]': 0
    })
    await session.setScriptTimeout(30000)
    await launchWebClip(session)
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

const close = (a: number | undefined, b: number | undefined) =>
  a !== undefined && b !== undefined && Math.abs(a - b) <= tolerancePx

// The tests run the app as a real home screen web clip, where iOS sets
// `navigator.standalone` and the app lays itself out as an app shell. The
// Playwright suite runs Chromium only, which does not reproduce the iOS
// WebKit layout bugs these tests are for.
describe('Citizen app installed on the iOS home screen', () => {
  /**
   * A button is reachable when it is fully inside the viewport, nothing is on
   * top of it in hit testing, and it is actually painted on screen. The last
   * check is the one that matters: iOS WebKit can paint a fixed element
   * clipped by a scrolling ancestor while hit testing and geometry still
   * report it as visible (PR #9975).
   */
  async function expectReachable(dataQa: string) {
    const selector = `[data-qa="${dataQa}"]`
    await session.waitForDisplayed(selector)
    const geometry = await session.execute((sel: string) => {
      const el = document.querySelector(sel)!
      const rect = el.getBoundingClientRect()
      const hit = document.elementFromPoint(
        rect.left + rect.width / 2,
        rect.top + rect.height / 2
      )
      return {
        inViewport:
          rect.left >= 0 &&
          rect.top >= 0 &&
          rect.right <= window.innerWidth &&
          rect.bottom <= window.innerHeight,
        coveredBy:
          hit === null
            ? 'nothing'
            : el.contains(hit)
              ? null
              : `${hit.tagName.toLowerCase()} in [data-qa="${hit.closest('[data-qa]')?.getAttribute('data-qa')}"]`,
        rect: `${rect.left},${rect.top} ${rect.width}x${rect.height}`,
        viewport: `${window.innerWidth}x${window.innerHeight}`
      }
    }, selector)
    expect(
      geometry.inViewport,
      `${selector} is not fully inside the viewport (rect ${geometry.rect}, viewport ${geometry.viewport})`
    ).toBe(true)
    expect(
      geometry.coveredBy,
      `${selector} is covered by ${geometry.coveredBy} at its centre point`
    ).toBeNull()
    const { box, ratio } = await painted(session, selector, {
      minRatio: minPaintedRatio
    })
    expect(
      ratio,
      `${selector} is not painted on screen: ${Math.round(ratio * 100)}% of its ${geometry.rect} area is visible, painted box ${formatBox(box)} (something is drawn over it or it is clipped)`
    ).toBeGreaterThanOrEqual(minPaintedRatio)
  }

  it('calendar modal buttons are reachable', async () => {
    await createFamilyWithPlacement()
    await openInWebClip(session, testAdult, '/calendar')
    await waitForCalendar()
    expect(
      await session.execute(
        () => (navigator as Navigator & { standalone?: boolean }).standalone
      ),
      'The clip does not run as a standalone app'
    ).toBe(true)

    for (const action of [
      'calendar-action-reservations',
      'calendar-action-absences'
    ]) {
      await session.click('[data-qa="open-calendar-actions-modal"]')
      await session.click(`[data-qa="${action}"]`)
      await expectReachable('modal-cancelBtn')
      await expectReachable('modal-okBtn')
      await session.click('[data-qa="modal-cancelBtn"]')
      await session.waitForGone('[data-qa="modal-cancelBtn"]')
    }
  })

  interface Snapshot {
    m: ViewportMeasurement
    header: ScreenBox | undefined
    nav: ScreenBox | undefined
  }

  async function snapshot(statusBarHeight: number): Promise<Snapshot> {
    const options = { ignoreAbove: statusBarHeight }
    return {
      m: await measureViewport(session),
      header: (await painted(session, selectors.header, options)).box,
      nav: (await painted(session, selectors.nav, options)).box
    }
  }

  const formatSnapshot = ({ m, header, nav }: Snapshot) =>
    `${formatMeasurement(m)}; painted on screen: header ${formatBox(header)}, nav ${formatBox(nav)}`

  /**
   * Where the header and the bottom navigation are laid out and painted must
   * not depend on the visual viewport: compared with the clean state before
   * the loops
   */
  function expectAnchored(when: string, clean: Snapshot, now: Snapshot) {
    const { m } = now
    const sameRect = (a: Rect | null, b: Rect | null) =>
      !!a && !!b && close(a.top, b.top) && close(a.bottom, b.bottom)
    const samePainted = (a: ScreenBox | undefined, b: ScreenBox | undefined) =>
      close(a?.top, b?.top) && close(a?.height, b?.height)
    const failures = [
      !(m.header && close(m.header.top, 0)) &&
        `the header is laid out at ${m.header?.top ?? 'missing'}, expected 0`,
      !sameRect(m.header, clean.m.header) &&
        'the header rect differs from the clean state',
      !(m.nav && close(m.nav.bottom, m.ih)) &&
        `the bottom navigation ends at ${m.nav?.bottom ?? 'missing'}, expected innerHeight ${m.ih}`,
      !sameRect(m.nav, clean.m.nav) &&
        'the bottom navigation rect differs from the clean state',
      !samePainted(now.header, clean.header) &&
        'the header is painted elsewhere than in the clean state',
      !samePainted(now.nav, clean.nav) &&
        'the bottom navigation is painted elsewhere than in the clean state',
      m.scrollY !== 0 && `the document is scrolled (scrollY ${m.scrollY})`
    ].filter((failure): failure is string => typeof failure === 'string')
    expect(
      failures,
      `With a stale visual viewport ${when}: ${failures.join('; ')}.\n  Now:   ${formatSnapshot(now)}\n  Clean: ${formatSnapshot(clean)}`
    ).toEqual([])
  }

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
  // `position: fixed` against it (WebKit bugs 254861 and 297779). The app
  // shell layout of PR #9798 keeps the header and the navigation in place
  // anyway. This test drives the simulator into the stale state and checks
  // that.
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
      // The page starts below the status bar in portrait
      const statusBarHeight = await session.execute(
        () => Math.max(screen.width, screen.height) - window.innerHeight
      )
      const clean = await snapshot(statusBarHeight)
      console.warn(`Clean state: ${formatSnapshot(clean)}`)
      expect(
        clean.m.header &&
          close(clean.header?.top, clean.m.header.top + statusBarHeight),
        `The header is painted at ${formatBox(clean.header)}, expected its layout top ${clean.m.header?.top} plus the status bar ${statusBarHeight}: the screen coordinates used for native taps are off`
      ).toBe(true)

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

      expectAnchored(
        'after rotating back',
        clean,
        await snapshot(statusBarHeight)
      )
      await scrollCalendar(session)
      expectAnchored(
        'after scrolling the calendar',
        clean,
        await snapshot(statusBarHeight)
      )
    },
    10 * 60 * 1000
  )
})
