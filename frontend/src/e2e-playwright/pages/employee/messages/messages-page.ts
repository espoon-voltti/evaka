// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from 'e2e-playwright/utils'
import { RawElement, RawTextInput } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class MessagesPage {
  constructor(private readonly page: Page) {}

  #newMessageButton = new RawElement(this.page, '[data-qa="new-message-btn"]')
  #sendMessageButton = new RawElement(this.page, '[data-qa="send-message-btn"]')
  #receiverSelection = new RawElement(this.page, '[data-qa="select-receiver"]')
  #inputTitle = new RawTextInput(this.page, '[data-qa="input-title"]')
  #inputContent = new RawTextInput(this.page, '[data-qa="input-content"]')
  #sentMessagesBoxRow = new RawTextInput(
    this.page,
    '[data-qa="message-box-row-SENT"]'
  )

  async getReceivedMessageCount() {
    return this.page.$$eval(
      '[data-qa="received-message-row"]',
      (rows) => rows.length
    )
  }

  async isEditorVisible() {
    return this.page.$$eval(
      '[data-qa="input-content"]',
      (contentInput) => contentInput.length > 0
    )
  }

  async existsSentMessage() {
    return this.page.$$eval(
      '[data-qa="sent-message-row"]',
      (sentMessages) => sentMessages.length > 0
    )
  }

  async sendNewMessage(title: string, content: string) {
    await this.#newMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), true)
    await this.#inputTitle.fill(title)
    await this.#inputContent.fill(content)
    await this.#receiverSelection.click()
    await this.page.keyboard.press('Enter')
    await this.#sendMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), false)

    await this.#sentMessagesBoxRow.click()
    await waitUntilEqual(() => this.existsSentMessage(), true)
  }
}
