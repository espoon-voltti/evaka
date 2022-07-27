// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page } from '../../utils/page'
import { TextInput } from '../../utils/page'

export default class ThreadViewPage {
  constructor(private readonly page: Page) {}

  #inputContent = new TextInput(
    this.page.find('[data-qa="thread-reply-input"]')
  )
  #sendReplyButton = this.page.find('[data-qa="thread-reply-button"]')
  #singleMessages = this.page.findAll('[data-qa="single-message"]')
  #singleMessageContents = this.page.findAll(
    '[data-qa="single-message-content"]'
  )
  #singleMessageSenderName = this.page.findAll(
    '[data-qa="single-message-sender-name"]'
  )

  async countMessages(): Promise<number> {
    return this.#singleMessages.count()
  }

  async getMessageContent(index: number): Promise<string> {
    return this.#singleMessageContents.nth(index).innerText
  }

  async getMessageSender(index: number): Promise<string> {
    return this.#singleMessageSenderName.nth(index).innerText
  }

  async replyThread(content: string) {
    await this.#inputContent.fill(content)
    await this.#sendReplyButton.click()
  }
}
