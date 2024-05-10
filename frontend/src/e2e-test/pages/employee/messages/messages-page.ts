// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../../utils'
import {
  FileInput,
  TextInput,
  Page,
  Checkbox,
  Combobox,
  TreeDropdown,
  Element
} from '../../../utils/page'

export default class MessagesPage {
  constructor(private readonly page: Page) {}

  async openSentMessages(nth = 0) {
    await this.page.findAllByDataQa('message-box-row-sent').nth(nth).click()
    return new SentMessagesPage(this.page)
  }

  newMessageButton = this.page.find('[data-qa="new-message-btn"]')
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
  #emptyInboxText = this.page.findByDataQa('empty-inbox-text')
  #unitAccount = this.page.find('[data-qa="unit-accounts"]')
  unitReceived = this.#unitAccount.find('[data-qa="message-box-row-received"]')

  async getReceivedMessageCount() {
    return await this.page.findAll('[data-qa="received-message-row"]').count()
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

  async openMessageEditor() {
    await this.newMessageButton.click()
    return this.getMessageEditor()
  }

  // In some scenarios the message editor is automatically opened when the page is loaded
  getMessageEditor() {
    return new MessageEditor(this.page.findByDataQa('message-editor'))
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

  async close() {
    return this.page.close()
  }
}

export class SentMessagesPage {
  constructor(private readonly page: Page) {}

  sentMessages = this.page.findAllByDataQa('sent-message-row')

  async assertMessageParticipants(nth: number, participants: string) {
    await this.sentMessages
      .nth(nth)
      .findByDataQa('participants')
      .assertTextEquals(participants)
  }

  async openMessage(nth: number) {
    await this.sentMessages.nth(nth).click()
    return new SentMessagePage(this.page)
  }
}

export class SentMessagePage {
  constructor(private readonly page: Page) {}

  async assertMessageRecipients(recipients: string) {
    await this.page.findByDataQa('recipient-names').assertTextEquals(recipients)
  }
}

export class MessageEditor extends Element {
  closeButton = this.findByDataQa('close-message-editor-btn')

  inputTitle = new TextInput(this.findByDataQa('input-title'))
  inputContent = new TextInput(this.findByDataQa('input-content'))
  senderSelection = new Combobox(this.findByDataQa('select-sender'))
  receiverSelection = new TreeDropdown(this.findByDataQa('select-receiver'))
  urgent = new Checkbox(this.findByDataQa('checkbox-urgent'))
  sensitive = new Checkbox(this.findByDataQa('checkbox-sensitive'))
  messageTypeMessage = new Checkbox(
    this.findByDataQa('radio-message-type-message')
  )
  messageTypeBulletin = new Checkbox(
    this.findByDataQa('radio-message-type-bulletin')
  )
  fileUpload = this.findByDataQa('upload-message-attachment')
  sendButton = this.findByDataQa('send-message-btn')
  discardButton = this.findByDataQa('discard-draft-btn')

  filtersButton = this.findByDataQa('filters-btn')
  filtersButtonCount = this.findAllByDataQa('filters-btn').count()
  yearsOfBirthSelection = new TreeDropdown(
    this.findByDataQa('select-years-of-birth')
  )
  serviceNeedSelection = new TreeDropdown(
    this.findByDataQa('select-service-needs')
  )
  shiftcare = new Checkbox(this.findByDataQa('checkbox-shiftcare'))
  intermittent = new Checkbox(
    this.findByDataQa('checkbox-intermittent-shiftcare')
  )
  familyDaycare = new Checkbox(this.findByDataQa('checkbox-family-daycare'))

  async sendNewMessage(message: {
    title: string
    content: string
    urgent?: boolean
    sensitive?: boolean
    attachmentCount?: number
    sender?: string
    receivers?: string[]
    confirmManyRecipients?: boolean
    yearsOfBirth?: number[]
    serviceNeedOptionIds?: string[]
    shiftcare?: boolean
    familyDaycare?: boolean
  }) {
    const attachmentCount = message.attachmentCount ?? 0

    await this.inputTitle.fill(message.title)
    await this.inputContent.fill(message.content)

    if (message.sender) {
      await this.senderSelection.fillAndSelectFirst(message.sender)
    }

    if (message.receivers) {
      await this.receiverSelection.open()
      await this.receiverSelection.expandAll()
      for (const receiver of message.receivers) {
        await this.receiverSelection.option(receiver).check()
      }
      await this.receiverSelection.close()
    } else {
      await this.receiverSelection.open()
      await this.receiverSelection.firstOption().check()
      await this.receiverSelection.close()
    }
    if (message.urgent ?? false) {
      await this.urgent.check()
    }
    if (message.sensitive ?? false) {
      await this.sensitive.check()
    }

    if (
      message.yearsOfBirth ||
      message.serviceNeedOptionIds ||
      message.shiftcare ||
      message.familyDaycare
    ) {
      await this.filtersButton.click()
    }

    if (message.yearsOfBirth) {
      await this.yearsOfBirthSelection.open()
      for (const year of message.yearsOfBirth) {
        await this.yearsOfBirthSelection.option(String(year)).check()
      }
      await this.yearsOfBirthSelection.close()
    }

    if (message.serviceNeedOptionIds) {
      await this.serviceNeedSelection.open()
      for (const serviceNeedOption of message.serviceNeedOptionIds) {
        await this.serviceNeedSelection.option(serviceNeedOption).check()
      }
      await this.serviceNeedSelection.close()
    }

    if (message.shiftcare ?? false) {
      await this.shiftcare.check()
    }

    if (message.familyDaycare ?? false) {
      await this.familyDaycare.check()
    }

    if (attachmentCount > 0) {
      for (let i = 1; i <= attachmentCount; i++) {
        await this.addAttachment()
        await waitUntilEqual(
          () => this.fileUpload.findAllByDataQa('file-download-button').count(),
          i
        )
      }
    }

    await this.sendButton.click()
    if (message.confirmManyRecipients) {
      await this.findByDataQa('modal-okBtn').click()
    }
    await this.waitUntilHidden()
  }

  async addAttachment() {
    const testFileName = 'test_file.png'
    const testFilePath = `src/e2e-test/assets/${testFileName}`
    await new FileInput(
      this.fileUpload.find('[data-qa="btn-upload-file"]')
    ).setInputFiles(testFilePath)
  }

  async assertSimpleViewVisible() {
    await this.inputTitle.waitUntilVisible()
    await this.messageTypeMessage.waitUntilHidden()
    await this.messageTypeBulletin.waitUntilHidden()
    await this.urgent.waitUntilHidden()
    await this.fileUpload.waitUntilHidden()
  }

  async draftNewMessage(title: string, content: string) {
    await this.inputTitle.fill(title)
    await this.receiverSelection.open()
    await this.receiverSelection.firstOption().click()
    await this.receiverSelection.close()
    await this.inputContent.fill(content)
    await waitUntilEqual(() => this.getEditorState(), 'clean')
  }

  async getEditorState() {
    return this.getAttribute('data-status')
  }

  async assertReceiver(receiverName: string) {
    return waitUntilEqual(() => this.receiverSelection.text, receiverName)
  }

  async assertTitle(title: string) {
    return waitUntilEqual(() => this.inputTitle.inputValue, title)
  }

  async assertFiltersVisible() {
    await this.yearsOfBirthSelection.waitUntilVisible()
    await this.serviceNeedSelection.waitUntilVisible()
    await this.shiftcare.waitUntilVisible()
    await this.familyDaycare.waitUntilVisible()
  }
}
