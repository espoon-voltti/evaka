// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual, waitUntilTrue } from 'e2e-playwright/utils'
import {
  RawElementDEPRECATED,
  RawTextInput
} from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class MessagesPage {
  constructor(private readonly page: Page) {}

  #newMessageButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="new-message-btn"]'
  )
  #sendMessageButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="send-message-btn"]'
  )
  #closeMessageEditorButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="close-message-editor-btn"]'
  )
  #discardMessageButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="discard-draft-btn"]'
  )
  #receiverSelection = new RawElementDEPRECATED(
    this.page,
    '[data-qa="select-receiver"]'
  )
  #inputTitle = new RawTextInput(this.page, '[data-qa="input-title"]')
  #inputContent = new RawTextInput(this.page, '[data-qa="input-content"]')
  #fileUpload = this.page.locator('[data-qa="upload-message-attachment"]')
  #sentMessagesBoxRow = new RawElementDEPRECATED(
    this.page,
    '[data-qa="message-box-row-SENT"]'
  )
  #draftMessagesBoxRow = new RawTextInput(
    this.page,
    '[data-qa="message-box-row-DRAFTS"]'
  )
  #receivedMessage = new RawElementDEPRECATED(
    this.page,
    '[data-qa="received-message-row"]'
  )
  #draftMessage = new RawElementDEPRECATED(
    this.page,
    '[data-qa="draft-message-row"]'
  )
  #messageContent = (index = 0) =>
    new RawElementDEPRECATED(
      this.page,
      `[data-qa="message-content"][data-index="${index}"]`
    )
  #inboxes = this.page.$$('[data-qa="message-box-row-RECEIVED"]')

  async inboxVisible() {
    const inboxes = await this.#inboxes
    return inboxes.length > 0
  }

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

  async openInbox(index: number) {
    await this.page.click(`:nth-match(:text("Saapuneet"), ${index})`)
  }

  async sendNewMessage(title: string, content: string, attachmentCount = 0) {
    await this.#newMessageButton.click()
    await waitUntilTrue(() => this.isEditorVisible())
    await this.#inputTitle.fill(title)
    await this.#inputContent.fill(content)
    await this.#receiverSelection.click()
    await this.page.keyboard.press('Enter')
    if (attachmentCount > 0) {
      for (let i = 1; i <= attachmentCount; i++) {
        await this.addAttachment()
        await waitUntilEqual(
          () =>
            this.#fileUpload
              .locator('[data-qa="file-download-button"]')
              .count(),
          i
        )
      }
    }
    await this.#sendMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), false)

    await this.#sentMessagesBoxRow.click()
    await waitUntilTrue(() => this.existsSentMessage())
  }

  async addAttachment() {
    const testFileName = 'test_file.png'
    const testFilePath = `src/e2e-playwright/assets/${testFileName}`
    await this.#fileUpload
      .locator('[data-qa="btn-upload-file"]')
      .setInputFiles(testFilePath)
  }

  async getEditorState() {
    return new RawElementDEPRECATED(
      this.page,
      '[data-qa="message-editor"]'
    ).getAttribute('data-status')
  }

  async draftNewMessage(title: string, content: string) {
    await this.#newMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), true)
    await this.#inputTitle.fill(title)
    await this.#inputContent.fill(content)
    await this.#receiverSelection.click()
    await this.page.keyboard.press('Enter')
    await waitUntilEqual(() => this.getEditorState(), 'clean')
  }

  async sendEditedMessage() {
    await this.#sendMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), false)
  }

  async closeMessageEditor() {
    await this.#closeMessageEditorButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), false)
  }

  async discardMessage() {
    await this.#discardMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), false)
  }

  async assertMessageContent(index: number, content: string) {
    await this.#receivedMessage.click()
    await waitUntilEqual(() => this.#messageContent(index).innerText, content)
  }

  async assertDraftContent(title: string, content: string) {
    await this.#draftMessagesBoxRow.click()
    await waitUntilEqual(
      () =>
        this.#draftMessage.find('[data-qa="thread-list-item-title"]').innerText,
      title
    )
    await waitUntilEqual(
      () =>
        this.#draftMessage.find('[data-qa="thread-list-item-content"]')
          .innerText,
      content
    )
  }

  async assertNoDrafts() {
    await this.#draftMessagesBoxRow.click()
    await this.#draftMessage.waitUntilHidden()
  }
}
