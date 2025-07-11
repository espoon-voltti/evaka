// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { MessageType } from 'lib-common/generated/api-types/messaging'

import { waitUntilEqual } from '../../../utils'
import type { Page, ElementCollection } from '../../../utils/page'
import {
  TextInput,
  Checkbox,
  Combobox,
  TreeDropdown,
  Element,
  Select,
  FileUpload
} from '../../../utils/page'

export default class MessagesPage {
  newMessageButton: Element
  #personalAccount: Element
  #messageCopiesInbox: Element
  receivedMessage: Element
  #draftMessage: Element
  #openReplyEditorButton: Element
  sendReplyButton: Element
  discardMessageButton: Element
  #messageReplyContent: TextInput
  #emptyInboxText: Element
  #unitAccount: Element
  #draftMessagesBoxRow: TextInput
  unitReceived: Element
  markUnreadButton: Element
  constructor(private readonly page: Page) {
    this.newMessageButton = page.findByDataQa('new-message-btn')
    this.#personalAccount = page.findByDataQa('personal-account')
    this.#messageCopiesInbox = page.findByDataQa('message-box-row-copies')
    this.receivedMessage = page.findByDataQa('received-message-row')
    this.#draftMessage = page.findByDataQa('draft-message-row')
    this.#openReplyEditorButton = page.findByDataQa(`message-reply-editor-btn`)
    this.sendReplyButton = page.findByDataQa('message-send-btn')
    this.discardMessageButton = page.findByDataQa('message-discard-btn')
    this.#messageReplyContent = new TextInput(
      page.findByDataQa('message-reply-content')
    )
    this.#emptyInboxText = page.findByDataQa('empty-inbox-text')
    this.#unitAccount = page.findByDataQa('unit-accounts')
    this.#draftMessagesBoxRow = new TextInput(
      this.#personalAccount.findByDataQa('message-box-row-drafts')
    )
    this.unitReceived = this.#unitAccount.findByDataQa(
      'message-box-row-received'
    )
    this.markUnreadButton = page.findByDataQa('mark-unread-btn')
  }

  async openSentMessages(nth = 0) {
    await this.page.findAllByDataQa('message-box-row-sent').nth(nth).click()
    return new SentMessagesPage(this.page)
  }

  async openFolder(name: string) {
    await this.page.findByDataQa(`message-box-row-${name}`).click()
    return new FolderMessagesPage(this.page)
  }

  #messageContent = (index = 0) =>
    this.page.findByDataQa(`message-content"][data-index="${index}`)

  async getReceivedMessageCount() {
    return await this.page.findAll('[data-qa="received-message-row"]').count()
  }

  async getThreadAttachmentCount(): Promise<number> {
    return this.page.findAll('[data-qa="attachment"]').count()
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

  async openFirstThread() {
    await this.receivedMessage.click()
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
    await this.receivedMessage.findByDataQa('thread-menu').click()
    await this.receivedMessage.findByDataQa('menu-item-archive').click()
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
  sentMessages: ElementCollection
  constructor(private readonly page: Page) {
    this.sentMessages = page.findAllByDataQa('sent-message-row')
  }

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

export class FolderMessagesPage {
  messages: ElementCollection
  constructor(private readonly page: Page) {
    this.messages = page.findAllByDataQa('folder-message-row')
  }

  async moveFirstThread(folderName: string) {
    await this.messages.first().findByDataQa('thread-menu').click()
    await this.messages.first().findByDataQa('menu-item-change-folder').click()
    const modal = this.page.findByDataQa('change-folder-modal')
    const select = new Select(modal.findByDataQa('folder-select'))
    await select.selectOption({ label: folderName })
    await modal.findByDataQa('modal-okBtn').click()
  }

  async openFirstThread() {
    await this.messages.first().click()
  }
  async openFirstThreadReplyEditor() {
    await this.messages.first().click()
    await this.page.findByDataQa('message-reply-editor-btn').click()
  }
  async assertHasNoMarkUnreadButton() {
    await this.page.findByDataQa('mark-unread-btn').waitUntilHidden()
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
  folderSelection = new Select(this.findByDataQa('select-folder'))
  senderSelection = new Combobox(this.findByDataQa('select-sender'))
  recipientSelection = new TreeDropdown(this.findByDataQa('select-recipient'))
  urgent = new Checkbox(this.findByDataQa('checkbox-urgent'))
  sensitive = new Checkbox(this.findByDataQa('checkbox-sensitive'))
  messageTypeMessage = new Checkbox(
    this.findByDataQa('radio-message-type-message')
  )
  messageTypeBulletin = new Checkbox(
    this.findByDataQa('radio-message-type-bulletin')
  )
  fileUpload = new FileUpload(this.findByDataQa('upload-message-attachment'))
  sendButton = this.findByDataQa('send-message-btn')
  discardButton = this.findByDataQa('discard-draft-btn')

  filtersButton = this.findByDataQa('filters-btn')
  filtersButtonCount = this.findAllByDataQa('filters-btn').count()
  yearsOfBirthSelection = new TreeDropdown(
    this.findByDataQa('select-years-of-birth')
  )
  shiftcare = new Checkbox(this.findByDataQa('checkbox-shiftcare'))
  intermittent = new Checkbox(
    this.findByDataQa('checkbox-intermittent-shiftcare')
  )
  familyDaycare = new Checkbox(this.findByDataQa('checkbox-family-daycare'))

  async sendNewMessage(message: {
    title: string
    content: string
    type?: MessageType
    urgent?: boolean
    sensitive?: boolean
    attachmentCount?: number
    sender?: string
    recipientKeys?: string[]
    confirmManyRecipients?: boolean
    yearsOfBirth?: number[]
    shiftcare?: boolean
    familyDaycare?: boolean
  }) {
    const attachmentCount = message.attachmentCount ?? 0

    await this.inputTitle.fill(message.title)
    await this.inputContent.fill(message.content)

    if (message.sender) {
      await this.senderSelection.fillAndSelectFirst(message.sender)
    }

    if (message.recipientKeys) {
      await this.recipientSelection.open()
      await this.recipientSelection.expandAll()
      for (const recipient of message.recipientKeys) {
        await this.recipientSelection.option(recipient).check()
      }
      await this.recipientSelection.close()
    } else {
      await this.recipientSelection.open()
      await this.recipientSelection.firstOption().check()
      await this.recipientSelection.close()
    }
    if (message.type === 'BULLETIN') {
      await this.messageTypeBulletin.check()
    }
    if (message.urgent ?? false) {
      await this.urgent.check()
    }
    if (message.sensitive ?? false) {
      await this.sensitive.check()
    }

    if (message.yearsOfBirth || message.shiftcare || message.familyDaycare) {
      await this.filtersButton.click()
    }

    if (message.yearsOfBirth) {
      await this.yearsOfBirthSelection.open()
      for (const year of message.yearsOfBirth) {
        await this.yearsOfBirthSelection.option(String(year)).check()
      }
      await this.yearsOfBirthSelection.close()
    }

    if (message.shiftcare ?? false) {
      await this.shiftcare.check()
    }

    if (message.familyDaycare ?? false) {
      await this.familyDaycare.check()
    }

    if (attachmentCount > 0) {
      const testFileName = 'test_file.odt'
      const testFilePath = `src/e2e-test/assets/${testFileName}`

      for (let i = 1; i <= attachmentCount; i++) {
        await this.fileUpload.upload(testFilePath)
      }
    }

    await this.sendButton.click()
    if (message.confirmManyRecipients) {
      await this.findByDataQa('modal-okBtn').click()
    }
    await this.waitUntilHidden()
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
    await this.recipientSelection.open()
    await this.recipientSelection.firstOption().click()
    await this.recipientSelection.close()
    await this.inputContent.fill(content)
    await waitUntilEqual(() => this.getEditorState(), 'clean')
  }

  async getEditorState() {
    return this.getAttribute('data-status')
  }

  async assertRecipient(recipientName: string) {
    return waitUntilEqual(() => this.recipientSelection.text, recipientName)
  }

  async assertTitle(title: string) {
    return waitUntilEqual(() => this.inputTitle.inputValue, title)
  }

  async assertFiltersVisible() {
    await this.yearsOfBirthSelection.waitUntilVisible()
    await this.shiftcare.waitUntilVisible()
    await this.familyDaycare.waitUntilVisible()
  }
}
