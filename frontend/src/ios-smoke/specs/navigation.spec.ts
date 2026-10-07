// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../e2e-test/config'
import { createFamilyWithPlacement, testAdult } from '../support/fixtures'
import { openAsInstalledApp, waitForCalendar } from '../support/installed-app'
import { expectReachable } from '../support/reachable'

describe('Bottom navigation in the installed app', () => {
  beforeEach(async () => {
    await createFamilyWithPlacement()
    await openAsInstalledApp(testAdult, '/calendar')
    await waitForCalendar()
  })

  it('navigation buttons are reachable', async () => {
    await expectReachable('nav-calendar-mobile')
    await expectReachable('nav-messages-mobile')
    await expectReachable('nav-children-mobile')
    await expectReachable('sub-nav-menu-mobile')
  })

  it('messages link navigates to messages', async () => {
    await $('[data-qa="nav-messages-mobile"]').click()
    await browser.waitUntil(
      async () =>
        (await browser.getUrl()).startsWith(`${config.enduserUrl}/messages`),
      { timeoutMsg: 'Did not navigate to /messages' }
    )
  })
})
