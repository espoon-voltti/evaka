// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'node:assert/strict'

import config from '../../../e2e-test/config'
import { createFamilyWithMessaging, testAdult } from '../../support/fixtures'
import { waitForCalendar } from '../../support/installed-app'
import { navigate } from '../../support/navigate'
import { waitForKeyboardClosed } from '../../support/orientation'
import { formatBox, paintedBox } from '../../support/reachable'
import type { ScreenBox } from '../../support/reachable'
import {
  formatMeasurement,
  measureViewport,
  scrollCalendar,
  selectors,
  triggerStaleViewport
} from '../../support/stale-viewport'
import type { Rect, ViewportMeasurement } from '../../support/stale-viewport'
import { tap } from '../../support/tap'
import { launchWebClip, openInWebClip } from '../../support/webclip'

const tolerancePx = 2
const maxLoops = 3
const testTimeoutMs = 10 * 60 * 1000

interface Painted {
  header: ScreenBox | undefined
  nav: ScreenBox | undefined
  fab: ScreenBox | undefined
}

interface Snapshot {
  m: ViewportMeasurement
  painted: Painted
}

const close = (a: number | undefined, b: number | undefined) =>
  a !== undefined && b !== undefined && Math.abs(a - b) <= tolerancePx

async function snapshot(statusBarHeight: number): Promise<Snapshot> {
  const options = { ignoreAbove: statusBarHeight }
  return {
    m: await measureViewport(),
    painted: {
      header: await paintedBox(selectors.header, options),
      nav: await paintedBox(selectors.nav, options),
      fab: await paintedBox(selectors.fab, options)
    }
  }
}

const formatSnapshot = ({ m, painted }: Snapshot) =>
  `${formatMeasurement(m)}; painted on screen: header ${formatBox(painted.header)}, nav ${formatBox(painted.nav)}, floating button ${formatBox(painted.fab)}`

// The push notification suggestion is a banner above the scroll area that
// leaves no room for the reply editor in landscape
async function dismissPushSuggestion(userId: string) {
  await navigate(`${config.enduserUrl}/offline.html`)
  await browser.execute((name: string) => {
    document.cookie = `${name}=true; max-age=86400; path=/; SameSite=Strict`
  }, `evaka-pwa-push-suggestion-dismissal-${userId}`)
}

async function sendNewMessage() {
  await tap('new-message-btn-mobile')
  await $('[data-qa="message-editor"]').waitForDisplayed()
  await $('[data-qa="select-recipient"] input[type="text"]').click()
  const option = $('[data-qa="select-recipient"] [data-qa="option"]')
  await option.waitForDisplayed()
  await option.click()
  await $('[data-qa="input-title"]').setValue('Viewport test')
  await $('[data-qa="input-content"]').setValue('A thread to reply to')
  await tap('send-message-btn')
  await $('[data-qa="message-editor"]').waitForDisplayed({ reverse: true })
  await $(selectors.threadListItem).waitForExist()
  await waitForKeyboardClosed()
}

/**
 * Where the header, the bottom navigation and the floating button are laid
 * out and painted must not depend on the visual viewport: compared with the
 * clean state before the loops
 */
function expectAnchored(when: string, clean: Snapshot, now: Snapshot) {
  const { m, painted } = now
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
    !sameRect(m.fab, clean.m.fab) &&
      'the floating calendar button rect differs from the clean state',
    !samePainted(painted.header, clean.painted.header) &&
      'the header is painted elsewhere than in the clean state',
    !samePainted(painted.nav, clean.painted.nav) &&
      'the bottom navigation is painted elsewhere than in the clean state',
    !samePainted(painted.fab, clean.painted.fab) &&
      'the floating calendar button is painted elsewhere than in the clean state',
    m.scrollY !== 0 && `the document is scrolled (scrollY ${m.scrollY})`
  ].filter((failure): failure is string => typeof failure === 'string')

  assert.deepEqual(
    failures,
    [],
    `With a stale visual viewport ${when}: ${failures.join('; ')}.\n  Now:   ${formatSnapshot(now)}\n  Clean: ${formatSnapshot(clean)}`
  )
}

// iOS WebKit can leave the visual viewport stale in a home screen app after
// the software keyboard closes and the device rotates, and it resolves
// `position: fixed` against it (WebKit bugs 254861 and 297779). The app shell
// layout of PR #9798 keeps the header and the navigation in place anyway. This
// test drives the simulator into the stale state and checks that.
describe('Stale visual viewport in the home screen web clip', () => {
  before(async () => {
    await launchWebClip()
  })

  it('header, navigation and floating button stay in place', async function () {
    this.timeout(testTimeoutMs)
    await createFamilyWithMessaging()
    await dismissPushSuggestion(testAdult.id)
    await openInWebClip(testAdult, '/messages')
    await $('[data-qa="new-message-btn-mobile"]').waitForDisplayed()
    assert.equal(
      await $('[data-qa="push-suggestion"]').isExisting(),
      false,
      'The push notification suggestion is shown although it was dismissed'
    )
    await sendNewMessage()

    await tap('nav-calendar-mobile')
    await waitForCalendar()
    await browser.pause(1000)
    // The page starts below the status bar in portrait
    const statusBarHeight = await browser.execute(
      () => Math.max(screen.width, screen.height) - window.innerHeight
    )
    const clean = await snapshot(statusBarHeight)
    console.warn(`Clean state: ${formatSnapshot(clean)}`)
    assert.ok(
      clean.m.header &&
        close(clean.painted.header?.top, clean.m.header.top + statusBarHeight),
      `The header is painted at ${formatBox(clean.painted.header)}, expected its layout top ${clean.m.header?.top} plus the status bar ${statusBarHeight}: the screen coordinates used for native taps are off`
    )

    const { reached, loops } = await triggerStaleViewport(maxLoops)
    const measured = loops
      .map((m, i) => `loop ${i + 1}: ${formatMeasurement(m)}`)
      .join('\n  ')
    console.warn(
      `Stale viewport ${reached ? 'reached' : 'not reached'}:\n  ${measured}`
    )
    assert.ok(
      reached,
      `The WebKit precondition was not reached: the visual viewport did not go stale in ${maxLoops} loops, so the layout was not tested.\n  ${measured}`
    )

    expectAnchored(
      'after rotating back',
      clean,
      await snapshot(statusBarHeight)
    )
    await scrollCalendar()
    expectAnchored(
      'after scrolling the calendar',
      clean,
      await snapshot(statusBarHeight)
    )
  })
})
