// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

export default class MobileMessagesPage {
  constructor(private readonly page: Page) {}

  async messagesExist() {
    return this.page.$$eval(
      '[data-qa^="message-preview"]',
      (threads) => threads.length > 0
    )
  }

  async openThread() {
    const elem = this.page.locator(`[data-qa^="message-preview"]`)
    return elem.click()
  }
}
