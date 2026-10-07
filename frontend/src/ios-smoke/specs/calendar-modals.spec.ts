// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type LocalDate from 'lib-common/local-date'

import { createFamilyWithPlacement, testAdult } from '../support/fixtures'
import { openAsInstalledApp, waitForCalendar } from '../support/installed-app'
import { expectReachable } from '../support/reachable'
import { tap } from '../support/tap'

// The installed app lays the page out as an app shell where only the content
// area scrolls (see citizen-frontend/App.tsx). A modal must still cover the
// whole screen there, so its footer buttons must not end up under the bottom
// navigation.
describe('Calendar modals in the installed app', () => {
  let today: LocalDate

  beforeEach(async () => {
    ;({ today } = await createFamilyWithPlacement())
    await openAsInstalledApp(testAdult, '/calendar')
    await waitForCalendar()
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

  // The unit is closed at weekends, and a closed day has no absence button
  it('day view buttons are reachable', async () => {
    await tap(`mobile-calendar-day-${today.addBusinessDays(5).formatIso()}`)
    await expectReachable('create-absence', 'calendar-dayview')
  })
})
