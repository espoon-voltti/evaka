// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../utils/page'

export default class MobileMessagesPage {
  constructor(private readonly page: Page) {}

  async messagesExist() {
    return (await this.page.findAll('[data-qa^="message-preview"]').count()) > 0
  }

  async openThread() {
    await this.page.find(`[data-qa^="message-preview"]`).click()
  }
}
