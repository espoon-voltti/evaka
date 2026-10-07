// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createFamilyWithMessaging, testAdult } from '../support/fixtures'
import { openAsInstalledApp } from '../support/installed-app'
import { expectReachable } from '../support/reachable'

describe('Message editor in the installed app', () => {
  beforeEach(async () => {
    await createFamilyWithMessaging()
    await openAsInstalledApp(testAdult, '/messages')
  })

  it('send button is reachable', async () => {
    const newMessage = $('[data-qa="new-message-btn-mobile"]')
    await newMessage.waitForDisplayed()
    await newMessage.click()
    await $('[data-qa="message-editor"]').waitForDisplayed()
    await expectReachable('send-message-btn', 'message-editor')
  })
})
