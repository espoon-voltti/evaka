// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from 'e2e-playwright/utils'
import { waitUntilTrue } from 'e2e-playwright/utils'
import { RawElement, RawTextInput } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class CitizenMessagesPage {
  constructor(private readonly page: Page) {}

  #messageReplyContent = new RawTextInput(
    this.page,
    '[data-qa="message-reply-content"]'
  )
  #threadListItem = new RawElement(this.page, '[data-qa="thread-list-item"]')
  #threadTitle = new RawElement(this.page, '[data-qa="thread-reader-title"]')
  #threadContent = new RawElement(
    this.page,
    '[data-qa="thread-reader-content"]'
  )
  #sendReplyButton = new RawElement(this.page, '[data-qa="message-send-btn"]')
  #newMessageButton = new RawElement(this.page, '[data-qa="new-message-btn"]')
  #sendMessageButton = new RawElement(this.page, '[data-qa="send-message-btn"]')
  #receiverSelection = new RawElement(this.page, '[data-qa="select-receiver"]')
  #inputTitle = new RawTextInput(this.page, '[data-qa="input-title"]')
  #inputContent = new RawTextInput(this.page, '[data-qa="input-content"]')

  async getMessageCount() {
    return this.page.$$eval(
      '[data-qa="thread-reader-content"]',
      (messages) => messages.length
    )
  }

  async assertThreadContent(title: string, content: string) {
    await this.#threadListItem.click()
    await waitUntilEqual(() => this.#threadTitle.innerText, title)
    await waitUntilEqual(() => this.#threadContent.innerText, content)
  }

  async getThreadCount() {
    return this.page.$$eval(
      '[data-qa="thread-list-item"]',
      (messages) => messages.length
    )}

  async replyToFirstThread(content: string) {
    await this.#threadListItem.click()
    await this.#messageReplyContent.fill(content)
    await this.#sendReplyButton.click()
  }

  async isEditorVisible() {
    return this.page.$$eval(
      '[data-qa="input-content"]',
      (contentInput) => contentInput.length > 0
    )
  }

  async sendNewMessage(title: string, content: string, receivers: string[]) {
    await this.#newMessageButton.click()
    await waitUntilTrue(() => this.isEditorVisible())
    await this.#inputTitle.fill(title)
    await this.#inputContent.fill(content)
    await this.#receiverSelection.click()
    for (const receiver of receivers) {
      await this.page.click(`text="${receiver}"`)
    }
    await this.#sendMessageButton.click()
    await waitUntilTrue(() => this.getThreadCount().then((count) => count > 0))
  }
}
