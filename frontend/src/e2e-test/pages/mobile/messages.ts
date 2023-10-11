// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilTrue } from '../../utils'
import { Page } from '../../utils/page'

export default class MobileMessagesPage {
  constructor(private readonly page: Page) {}

  messagesContainer = this.page.findByDataQa('messages-page-content-area')
  noAccountInfo = this.page.findByDataQa('info-no-account-access')
  threads = this.page.findAllByDataQa('message-preview')
  newMessage = this.page.findByDataQa('new-message-btn')

  async getThreadTitle(index: number) {
    const titles = this.page.findAllByDataQa('message-preview-title')
    return titles.nth(index).text
  }

  async assertThreadsExist() {
    await waitUntilTrue(async () => (await this.threads.count()) > 0)
  }

  async openFirstThread() {
    await this.threads.first().click()
  }
}
