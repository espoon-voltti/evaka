// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'node:assert/strict'

import {
  expectAppShellLayout,
  expectMobileMenuInsideAppShell,
  expectModalsOutsideAppShell
} from '../support/app-shell'
import {
  createFamilyWithMessaging,
  createFamilyWithPlacement,
  testAdult
} from '../support/fixtures'
import { openAsInstalledApp, waitForCalendar } from '../support/installed-app'
import { tap } from '../support/tap'

// iOS can leave the visual viewport stale in the installed app after the
// software keyboard closes or the device rotates, and viewport anchored
// elements then drift (PR #9798). These tests check the layout that avoids
// viewport anchoring; webclip/stale-viewport.spec.ts reproduces the bug.
describe('App shell layout in the installed app', () => {
  it('calendar keeps the header and the navigation out of the scrolling content', async () => {
    await createFamilyWithPlacement()
    await openAsInstalledApp(testAdult, '/calendar')
    await waitForCalendar()
    await expectAppShellLayout({ longPage: true })
  })

  it('messages page and the mobile menu are anchored to the shell', async () => {
    await createFamilyWithMessaging()
    await openAsInstalledApp(testAdult, '/messages')
    await $('[data-qa="new-message-btn-mobile"]').waitForDisplayed()
    await expectAppShellLayout()
    await tap('sub-nav-menu-mobile')
    await expectMobileMenuInsideAppShell()
  })

  it('calendar actions modal is rendered outside the shell', async () => {
    await createFamilyWithPlacement()
    await openAsInstalledApp(testAdult, '/calendar')
    await waitForCalendar()
    await tap('open-calendar-actions-modal')
    await $('[data-qa="calendar-action-reservations"]').waitForDisplayed()
    const dialogs = await expectModalsOutsideAppShell()
    assert.ok(dialogs > 0, 'No open dialog found')
  })
})
