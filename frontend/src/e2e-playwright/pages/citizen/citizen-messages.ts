// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from 'e2e-playwright/utils'
import { waitUntilTrue } from 'e2e-playwright/utils'
import {
  RawElementDEPRECATED,
  RawTextInput
} from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class CitizenMessagesPage {
  constructor(private readonly page: Page) {}
  replyButtonTag = 'message-reply-editor-btn'

  #messageReplyContent = new RawTextInput(
    this.page,
    '[data-qa="message-reply-content"]'
  )
  #threadListItem = new RawElementDEPRECATED(
    this.page,
    '[data-qa="thread-list-item"]'
  )
  #threadTitle = new RawElementDEPRECATED(
    this.page,
    '[data-qa="thread-reader-title"]'
  )
  #threadContent = new RawElementDEPRECATED(
    this.page,
    '[data-qa="thread-reader-content"]'
  )
  #openReplyEditorButton = new RawElementDEPRECATED(
    this.page,
    `[data-qa="${this.replyButtonTag}"]`
  )
  #sendReplyButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="message-send-btn"]'
  )
  #newMessageButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="new-message-btn"]'
  )
  #sendMessageButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="send-message-btn"]'
  )
  #receiverSelection = new RawElementDEPRECATED(
    this.page,
    '[data-qa="select-receiver"]'
  )
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

  getThreadAttachmentCount(): Promise<number> {
    return this.page.locator('[data-qa="attachment"]').count()
  }

  async getThreadCount() {
    return this.page.locator('[data-qa="thread-list-item"]').count()
  }

  async isEditorVisible() {
    return this.page.locator('[data-qa="input-content"]').isVisible()
  }

  async replyToFirstThread(content: string) {
    await this.#threadListItem.click()
    await this.#openReplyEditorButton.click()
    await this.#messageReplyContent.fill(content)
    await this.#sendReplyButton.click()
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
