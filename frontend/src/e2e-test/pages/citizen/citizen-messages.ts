// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilTrue } from '../../utils'
import { Element, MultiSelect, Page, TextInput } from '../../utils/page'

export class MockStrongAuthPage {
  constructor(private readonly page: Page) {}

  async login(ssn: string) {
    const checkbox = this.page.find(`[id="${ssn}"]`)
    await checkbox.click()
    const submit = this.page.find('button')
    await submit.click()
    return new CitizenMessagesPage(this.page)
  }
}
export default class CitizenMessagesPage {
  constructor(private readonly page: Page) {}

  replyButtonTag = 'message-reply-editor-btn'

  #messageReplyContent = new TextInput(
    this.page.find('[data-qa="message-reply-content"]')
  )
  #threadListItem = this.page.find('[data-qa="thread-list-item"]')
  #threadTitle = this.page.find('[data-qa="thread-reader-title"]')
  #redactedThreadTitle = this.page.find(
    '[data-qa="redacted-thread-reader-title"]'
  )
  #strongAuthLink = this.page.find('[data-qa="strong-auth-link"]')
  #inboxEmpty = this.page.find('[data-qa="inbox-empty"][data-loading="false"]')
  #threadContent = this.page.findAll('[data-qa="thread-reader-content"]')
  #threadUrgent = this.page.findByDataQa('thread-reader').findByDataQa('urgent')
  #openReplyEditorButton = this.page.find(`[data-qa="${this.replyButtonTag}"]`)
  #sendReplyButton = this.page.find('[data-qa="message-send-btn"]')
  newMessageButton = this.page.findAllByDataQa('new-message-btn').first()
  #messageEditor = this.page.findByDataQa('message-editor')

  discardMessageButton = this.page.find('[data-qa="message-discard-btn"]')

  async createNewMessage(): Promise<CitizenMessageEditor> {
    await this.newMessageButton.click()
    const editor = new CitizenMessageEditor(this.#messageEditor)
    await editor.waitUntilVisible()
    return editor
  }

  async getMessageCount() {
    return this.#threadContent.count()
  }

  async assertInboxIsEmpty() {
    await this.#inboxEmpty.waitUntilVisible()
  }

  async assertThreadContent(message: {
    title: string
    content: string
    urgent?: boolean
    sensitive?: boolean
  }) {
    await this.#threadListItem.click()
    await this.#threadTitle.assertTextEquals(
      message.title + (message.sensitive ? ' (Arkaluontoinen viestiketju)' : '')
    )
    await this.#threadContent.only().assertTextEquals(message.content)
    if (message.urgent ?? false) {
      await this.#threadUrgent.waitUntilVisible()
    } else {
      await this.#threadUrgent.waitUntilHidden()
    }
  }
  async assertThreadIsRedacted() {
    await this.#threadListItem.click()
    await this.#redactedThreadTitle.waitUntilVisible()
  }
  async openStrongAuthPage() {
    await this.#strongAuthLink.click()
    return new MockStrongAuthPage(this.page)
  }

  getThreadAttachmentCount(): Promise<number> {
    return this.page.findAll('[data-qa="attachment"]').count()
  }

  async openFirstThread() {
    await this.#threadListItem.click()
  }

  async openFirstThreadReplyEditor() {
    await this.#threadListItem.click()
    await this.#openReplyEditorButton.click()
  }

  async discardReplyEditor() {
    await this.discardMessageButton.click()
  }

  async fillReplyContent(content: string) {
    await this.#messageReplyContent.fill(content)
  }

  async assertReplyContentIsEmpty() {
    return this.#messageReplyContent.assertTextEquals('')
  }

  async replyToFirstThread(content: string) {
    await this.#threadListItem.click()
    await this.#openReplyEditorButton.click()
    await this.#messageReplyContent.fill(content)
    await this.#sendReplyButton.click()
    // the content is cleared and the button is disabled once the reply has been sent
    await waitUntilTrue(() => this.#sendReplyButton.disabled)
  }

  async deleteFirstThread() {
    await this.#threadListItem.findByDataQa('delete-thread-btn').click()
  }

  async confirmThreadDeletion() {
    await this.page.findByDataQa('modal').findByDataQa('modal-okBtn').click()
  }

  async sendNewMessage(
    title: string,
    content: string,
    childIds: string[],
    recipients: string[]
  ) {
    const editor = await this.createNewMessage()
    if (childIds.length > 0) {
      await editor.selectChildren(childIds)
    }
    await editor.selectRecipients(recipients)
    await editor.fillMessage(title, content)
    await editor.sendMessage()
  }
}

export class CitizenMessageEditor extends Element {
  readonly #recipientSelection = new MultiSelect(
    this.findByDataQa('select-recipient')
  )
  readonly title = new TextInput(this.findByDataQa('input-title'))
  readonly content = new TextInput(this.findByDataQa('input-content'))
  readonly #sendMessage = this.findByDataQa('send-message-btn')

  secondaryRecipient(name: string) {
    return this.find(`[data-qa="secondary-recipient"]`, { hasText: name })
  }

  async selectChildren(childIds: string[]) {
    for (const childId of childIds) {
      await this.findByDataQa(`child-${childId}`).click()
    }
  }
  async assertChildrenSelectable(childIds: string[]) {
    for (const childId of childIds) {
      await this.findByDataQa(`child-${childId}`).waitUntilVisible()
    }

    await this.findAllByDataQa('relevant-child').assertCount(childIds.length)
  }

  async selectRecipients(recipients: string[]) {
    await this.#recipientSelection.click()
    for (const recipient of recipients) {
      await this.#recipientSelection.fillAndSelectFirst(recipient)
    }
    await this.#recipientSelection.click()
  }

  async assertNoRecipients() {
    await this.#recipientSelection.click()
    await this.#recipientSelection.assertNoOptions()
  }

  async fillMessage(title: string, content: string) {
    await this.title.fill(title)
    await this.content.fill(content)
  }

  async sendMessage() {
    await this.#sendMessage.click()
    await this.waitUntilHidden()
  }
}
