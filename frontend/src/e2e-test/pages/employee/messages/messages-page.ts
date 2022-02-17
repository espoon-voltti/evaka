// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual, waitUntilTrue } from '../../../utils'
import { FileInput, TextInput, Page } from '../../../utils/page'

export default class MessagesPage {
  constructor(private readonly page: Page) {}

  #newMessageButton = this.page.find('[data-qa="new-message-btn"]')
  #sendMessageButton = this.page.find('[data-qa="send-message-btn"]')
  #closeMessageEditorButton = this.page.find(
    '[data-qa="close-message-editor-btn"]'
  )
  #discardMessageButton = this.page.find('[data-qa="discard-draft-btn"]')
  #receiverSelection = this.page.find('[data-qa="select-receiver"]')
  #inputTitle = new TextInput(this.page.find('[data-qa="input-title"]'))
  #inputContent = new TextInput(this.page.find('[data-qa="input-content"]'))
  #fileUpload = this.page.find('[data-qa="upload-message-attachment"]')
  #personalAccount = this.page.find('[data-qa="personal-account"]')
  #firstSentMessagesBoxRow = this.page
    .findAll('[data-qa="message-box-row-SENT"]')
    .first()
  #draftMessagesBoxRow = new TextInput(
    this.#personalAccount.find('[data-qa="message-box-row-DRAFTS"]')
  )
  #receivedMessage = this.page.find('[data-qa="received-message-row"]')
  #draftMessage = this.page.find('[data-qa="draft-message-row"]')
  #messageContent = (index = 0) =>
    this.page.find(`[data-qa="message-content"][data-index="${index}"]`)

  #openReplyEditorButton = this.page.find(
    `[data-qa="message-reply-editor-btn"]`
  )
  discardMessageButton = this.page.find('[data-qa="message-discard-btn"]')
  #messageReplyContent = new TextInput(
    this.page.find('[data-qa="message-reply-content"]')
  )

  async getReceivedMessageCount() {
    return await this.page.findAll('[data-qa="received-message-row"]').count()
  }

  async isEditorVisible() {
    return (await this.page.findAll('[data-qa="input-content"]').count()) > 0
  }

  async existsSentMessage() {
    return (await this.page.findAll('[data-qa="sent-message-row"]').count()) > 0
  }

  async openInbox(index: number) {
    await this.page.findAll(':text("Saapuneet")').nth(index).click()
  }

  async openReplyEditor() {
    await this.#openReplyEditorButton.click()
  }

  async openFirstThreadReplyEditor() {
    await this.#receivedMessage.click()
    await this.#openReplyEditorButton.click()
  }

  async discardReplyEditor() {
    await this.discardMessageButton.click()
  }

  async fillReplyContent(content: string) {
    await this.#messageReplyContent.fill(content)
  }

  async assertReplyContentIsEmpty() {
    return waitUntilEqual(() => this.#messageReplyContent.textContent, '')
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
              .findAll('[data-qa="file-download-button"]')
              .count(),
          i
        )
      }
    }
    await this.#sendMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), false)

    await this.#firstSentMessagesBoxRow.click()
    await waitUntilTrue(() => this.existsSentMessage())
  }

  async addAttachment() {
    const testFileName = 'test_file.png'
    const testFilePath = `src/e2e-test/assets/${testFileName}`
    await new FileInput(
      this.#fileUpload.find('[data-qa="btn-upload-file"]')
    ).setInputFiles(testFilePath)
  }

  async getEditorState() {
    return this.page
      .find('[data-qa="message-editor"]')
      .getAttribute('data-status')
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
