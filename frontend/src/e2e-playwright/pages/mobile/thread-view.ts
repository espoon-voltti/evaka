// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

export default class ThreadViewPage {
  constructor(private readonly page: Page) {}

  #inputContent = this.page.locator('[data-qa="thread-reply-input"]')
  #sendReplyButton = this.page.locator('[data-qa="thread-reply-button"]')

  async countMessages(): Promise<number> {
    return this.page.$$eval(
      '[data-qa="single-message"]',
      (threads) => threads.length
    )
  }

  async getMessageContent(index: number): Promise<string> {
    const el = this.page.locator(
      `:nth-match([data-qa="single-message-content"], ${index})`
    )
    return el.innerText()
  }

  async getMessageSender(index: number): Promise<string> {
    const el = this.page.locator(
      `:nth-match([data-qa="single-message-sender-name"], ${index})`
    )
    return el.innerText()
  }

  async replyThread(content: string) {
    await this.#inputContent.fill(content)
    await this.#sendReplyButton.click()
  }
}
