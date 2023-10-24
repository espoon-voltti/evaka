// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual, waitUntilTrue } from '../../../utils'
import {
  FileInput,
  TextInput,
  Page,
  Checkbox,
  Combobox,
  TreeDropdown
} from '../../../utils/page'

export default class MessagesPage {
  constructor(private readonly page: Page) {}

  newMessageButton = this.page.find('[data-qa="new-message-btn"]')
  sendMessageButton = this.page.find('[data-qa="send-message-btn"]')
  #closeMessageEditorButton = this.page.find(
    '[data-qa="close-message-editor-btn"]'
  )
  #discardMessageButton = this.page.find('[data-qa="discard-draft-btn"]')
  #senderSelection = new Combobox(this.page.findByDataQa('select-sender'))
  #receiverSelection = new TreeDropdown(
    this.page.find('[data-qa="select-receiver"]')
  )
  inputTitle = new TextInput(this.page.find('[data-qa="input-title"]'))
  inputContent = new TextInput(this.page.find('[data-qa="input-content"]'))
  #fileUpload = this.page.find('[data-qa="upload-message-attachment"]')
  #personalAccount = this.page.find('[data-qa="personal-account"]')
  #draftMessagesBoxRow = new TextInput(
    this.#personalAccount.find('[data-qa="message-box-row-drafts"]')
  )
  #messageCopiesInbox = this.page.findByDataQa('message-box-row-copies')
  receivedMessage = this.page.find('[data-qa="received-message-row"]')
  #draftMessage = this.page.find('[data-qa="draft-message-row"]')
  #messageContent = (index = 0) =>
    this.page.find(`[data-qa="message-content"][data-index="${index}"]`)

  #openReplyEditorButton = this.page.find(
    `[data-qa="message-reply-editor-btn"]`
  )
  sendReplyButton = this.page.find('[data-qa="message-send-btn"]')
  discardMessageButton = this.page.find('[data-qa="message-discard-btn"]')
  #messageReplyContent = new TextInput(
    this.page.find('[data-qa="message-reply-content"]')
  )
  #urgent = new Checkbox(this.page.findByDataQa('checkbox-urgent'))
  #sensitive = new Checkbox(this.page.findByDataQa('checkbox-sensitive'))
  #messageTypeMessage = new Checkbox(
    this.page.findByDataQa('radio-message-type-message')
  )
  #messageTypeBulletin = new Checkbox(
    this.page.findByDataQa('radio-message-type-bulletin')
  )
  #emptyInboxText = this.page.findByDataQa('empty-inbox-text')

  async getReceivedMessageCount() {
    return await this.page.findAll('[data-qa="received-message-row"]').count()
  }

  async isEditorVisible() {
    return (await this.page.findAll('[data-qa="input-content"]').count()) > 0
  }

  async existsSentMessage() {
    return (await this.page.findAll('[data-qa="sent-message-row"]').count()) > 0
  }

  async assertSimpleViewVisible() {
    await this.inputTitle.waitUntilVisible()
    await this.#messageTypeMessage.waitUntilHidden()
    await this.#messageTypeBulletin.waitUntilHidden()
    await this.#urgent.waitUntilHidden()
    await this.#fileUpload.waitUntilHidden()
  }

  async assertMessageIsSentForParticipants(nth: number, participants: string) {
    await this.page.findAll('[data-qa="message-box-row-sent"]').first().click()
    await this.page
      .findAllByDataQa('sent-message-row')
      .nth(nth)
      .findByDataQa('participants')
      .assertTextEquals(participants)
  }

  async assertReceivedMessageParticipantsContains(nth: number, str: string) {
    await this.page
      .findAllByDataQa('received-message-row')
      .nth(nth)
      .find('[data-qa="participants"]', { hasText: str })
      .waitUntilVisible()
  }

  async openInbox(index: number) {
    await this.page.findAll(':text("Saapuneet")').nth(index).click()
  }

  async openReplyEditor() {
    await this.#openReplyEditorButton.click()
  }

  async openFirstThreadReplyEditor() {
    await this.receivedMessage.click()
    await this.#openReplyEditorButton.click()
  }

  async discardReplyEditor() {
    await this.discardMessageButton.click()
  }

  async fillReplyContent(content: string) {
    await this.#messageReplyContent.fill(content)
  }

  async assertReplyContentIsEmpty() {
    await this.#messageReplyContent.assertTextEquals('')
  }

  async sendNewMessage(message: {
    title: string
    content: string
    urgent?: boolean
    sensitive?: boolean
    attachmentCount?: number
    sender?: string
    receiver?: string
  }) {
    const attachmentCount = message.attachmentCount ?? 0

    await this.newMessageButton.click()
    await waitUntilTrue(() => this.isEditorVisible())
    await this.inputTitle.fill(message.title)
    await this.inputContent.fill(message.content)

    if (message.sender) {
      await this.#senderSelection.fillAndSelectFirst(message.sender)
    }

    if (message.receiver) {
      await this.#receiverSelection.open()
      await this.#receiverSelection.expandAll()
      await this.#receiverSelection.option(message.receiver).check()
      await this.#receiverSelection.close()
    } else {
      await this.#receiverSelection.open()
      await this.#receiverSelection.firstOption().check()
      await this.#receiverSelection.close()
    }
    if (message.urgent ?? false) {
      await this.#urgent.check()
    }
    if (message.sensitive ?? false) {
      await this.#sensitive.check()
    }

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
    await this.sendMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), false)
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
    await this.newMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), true)
    await this.inputTitle.fill(title)
    await this.inputContent.fill(content)
    await this.#receiverSelection.open()
    await this.#receiverSelection.firstOption().click()
    await this.#receiverSelection.close()
    await this.page.keyboard.press('Enter')
    await waitUntilEqual(() => this.getEditorState(), 'clean')
  }

  async sendEditedMessage() {
    await this.sendMessageButton.click()
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

  async deleteFirstThread() {
    await this.receivedMessage.findByDataQa('delete-thread-btn').click()
  }

  async assertMessageContent(index: number, content: string) {
    await this.#messageContent(index).assertTextEquals(content)
  }

  async assertDraftContent(title: string, content: string) {
    await this.#draftMessagesBoxRow.click()
    await this.#draftMessage
      .find('[data-qa="thread-list-item-title"]')
      .assertTextEquals(title)
    await this.#draftMessage
      .find('[data-qa="thread-list-item-content"]')
      .assertTextEquals(content)
  }

  async assertNoDrafts() {
    await this.#draftMessagesBoxRow.click()
    await this.#emptyInboxText.waitUntilVisible()
  }

  async assertCopyContent(title: string, content: string) {
    await this.#messageCopiesInbox.click()
    await this.page
      .findByDataQa('thread-list-item-title')
      .assertTextEquals(title)
    await this.page
      .findByDataQa('thread-list-item-content')
      .assertTextEquals(content)
  }

  async assertNoCopies() {
    await this.#messageCopiesInbox.click()
    await this.#emptyInboxText.waitUntilVisible()
  }

  async undoMessage() {
    await this.page
      .findByDataQa('undo-message-toast')
      .findByDataQa('cancel-message')
      .click()
  }

  async assertReceiver(receiverName: string) {
    return waitUntilEqual(() => this.#receiverSelection.text, receiverName)
  }

  async assertTitle(title: string) {
    return waitUntilEqual(() => this.inputTitle.inputValue, title)
  }

  async close() {
    return this.page.close()
  }
}
