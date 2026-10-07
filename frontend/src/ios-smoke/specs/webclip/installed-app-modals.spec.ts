// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'node:assert/strict'

import { createFamilyWithPlacement, testAdult } from '../../support/fixtures'
import { waitForCalendar } from '../../support/installed-app'
import { expectReachable } from '../../support/reachable'
import { tap } from '../../support/tap'
import { launchWebClip, openInWebClip } from '../../support/webclip'

const maxStatusBarHeight = 70

// Same checks as calendar-modals.spec.ts, but in the app opened from a home
// screen icon, where iOS itself puts the page into standalone mode
describe('Calendar modals in the home screen web clip', () => {
  before(async () => {
    await launchWebClip()
    // Right after the tap the dev bundle is still loading and the screen can
    // be blank
    await browser.waitUntil(
      () => browser.execute(() => !!document.querySelector('#app [data-qa]')),
      { timeout: 30000, timeoutMsg: 'The app did not render in the web clip' }
    )
  })

  beforeEach(async () => {
    await createFamilyWithPlacement()
    await openInWebClip(testAdult, '/calendar')
    await waitForCalendar()
  })

  it('runs standalone without the forceStandalone test config', async () => {
    const state = await browser.execute(() => ({
      standalone: (navigator as Navigator & { standalone?: boolean })
        .standalone,
      dataStandalone: document.documentElement.hasAttribute('data-standalone'),
      storedTestConfig: window.localStorage.getItem('evaka.testConfig'),
      innerHeight: window.innerHeight,
      screenHeight: window.screen.height
    }))
    assert.equal(state.standalone, true, 'navigator.standalone is not true')
    assert.deepEqual(
      JSON.parse(state.storedTestConfig ?? '{}'),
      { automatedTest: true },
      'Unexpected test config in localStorage'
    )
    assert.ok(state.dataStandalone, 'The app did not set html[data-standalone]')
    // The page starts under the translucent status bar, yet iOS still leaves
    // the status bar height out of innerHeight (874 - 812 on iPhone 17). A
    // Safari style toolbar at the top would take about 200 points more.
    assert.ok(
      state.screenHeight - state.innerHeight <= maxStatusBarHeight,
      `The page does not fill the screen (innerHeight ${state.innerHeight}, screen height ${state.screenHeight}): is a Safari style toolbar shown?`
    )
  })

  it('reservation modal buttons are reachable', async () => {
    await tap('open-calendar-actions-modal')
    await tap('calendar-action-reservations')
    await expectReachable('modal-cancelBtn')
    await expectReachable('modal-okBtn')
  })

  it('absence modal buttons are reachable', async () => {
    await tap('open-calendar-actions-modal')
    await tap('calendar-action-absences')
    await expectReachable('modal-cancelBtn')
    await expectReachable('modal-okBtn')
  })
})
