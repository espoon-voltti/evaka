// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { Page, TextInput } from '../../utils/page'

export default class CitizenMessagesPage {
  constructor(private readonly page: Page) {}

  replyButtonTag = 'message-reply-editor-btn'

  #messageReplyContent = new TextInput(
    this.page.find('[data-qa="message-reply-content"]')
  )
  #threadListItem = this.page.find('[data-qa="thread-list-item"]')
  #threadTitle = this.page.find('[data-qa="thread-reader-title"]')
  #inboxEmpty = this.page.find('[data-qa="inbox-empty"]')
  #threadContent = this.page.findAll('[data-qa="thread-reader-content"]')
  #threadUrgent = this.page.findByDataQa('thread-reader').findByDataQa('urgent')
  #openReplyEditorButton = this.page.find(`[data-qa="${this.replyButtonTag}"]`)
  #sendReplyButton = this.page.find('[data-qa="message-send-btn"]')
  #newMessageButton = this.page.findAllByDataQa('new-message-btn').first()
  #sendMessageButton = this.page.find('[data-qa="send-message-btn"]')
  #receiverSelection = this.page.find('[data-qa="select-receiver"]')
  #inputTitle = new TextInput(this.page.find('[data-qa="input-title"]'))
  #inputContent = new TextInput(this.page.find('[data-qa="input-content"]'))

  discardMessageButton = this.page.find('[data-qa="message-discard-btn"]')

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
  }) {
    await this.#threadListItem.click()
    await waitUntilEqual(() => this.#threadTitle.innerText, message.title)
    await waitUntilEqual(
      () => this.#threadContent.elem().innerText,
      message.content
    )
    if (message.urgent ?? false) {
      await this.#threadUrgent.waitUntilVisible()
    } else {
      await this.#threadUrgent.waitUntilHidden()
    }
  }

  getThreadAttachmentCount(): Promise<number> {
    return this.page.findAll('[data-qa="attachment"]').count()
  }

  async getThreadCount() {
    return this.page.findAll('[data-qa="thread-list-item"]').count()
  }

  async isEditorVisible() {
    return this.page.find('[data-qa="input-content"]').visible
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
    return waitUntilEqual(() => this.#messageReplyContent.textContent, '')
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

  async clickNewMessage() {
    await this.#newMessageButton.click()
    await waitUntilTrue(() => this.isEditorVisible())
  }

  async selectNewMessageRecipients(recipients: string[]) {
    await this.#receiverSelection.click()
    for (const recipient of recipients) {
      await this.page.findTextExact(recipient).click()
    }
    await this.#receiverSelection.click()
  }

  async typeMessage(title: string, content: string) {
    await this.#inputTitle.fill(title)
    await this.#inputContent.fill(content)
  }

  async clickSendMessage() {
    await this.#sendMessageButton.click()
  }

  async selectMessageChildren(childIds: string[]) {
    for (const childId of childIds) {
      await this.page.findByDataQa(`child-${childId}`).click()
    }
  }

  async assertChildrenSelectable(childIds: string[]) {
    for (const childId of childIds) {
      await this.page.findByDataQa(`child-${childId}`).waitUntilVisible()
    }
  }

  async sendNewMessage(title: string, content: string, recipients: string[]) {
    await this.clickNewMessage()
    await this.typeMessage(title, content)
    await this.selectNewMessageRecipients(recipients)
    await this.clickSendMessage()
    await waitUntilTrue(() => this.getThreadCount().then((count) => count > 0))
  }
}
