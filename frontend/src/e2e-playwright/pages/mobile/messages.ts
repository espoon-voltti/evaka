// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../utils/page'

export default class MobileMessagesPage {
  constructor(private readonly page: Page) {}

  messagesContainer = this.page.find(`[data-qa="messages-page-content-area"]`)

  async messagesExist() {
    return (await this.page.findAll('[data-qa^="message-preview"]').count()) > 0
  }

  async messagesDontExist() {
    const els = this.page.findAll('[data-qa^="message-preview"]')

    return (await els.count()) === 0
  }

  async openFirstThread() {
    await this.page.find(`[data-qa^="message-preview"]`).click()
  }
}
