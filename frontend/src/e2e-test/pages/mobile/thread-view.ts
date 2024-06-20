// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput, Element } from '../../utils/page'

export default class ThreadViewPage {
  goBack: Element
  replyButton: Element
  replyContent: TextInput
  sendReplyButton: Element
  discardReplyButton: Element
  constructor(private readonly page: Page) {
    this.goBack = page.findByDataQa('go-back')
    this.replyButton = page.findByDataQa('message-reply-editor-btn')
    this.replyContent = new TextInput(
      page.findByDataQa('message-reply-content')
    )
    this.sendReplyButton = page.findByDataQa('message-send-btn')
    this.discardReplyButton = page.findByDataQa('message-discard-btn')
  }

  singleMessageContents = this.page.findAll(
    '[data-qa="single-message-content"]'
  )
  singleMessageSenderName = this.page.findAll(
    '[data-qa="single-message-sender-name"]'
  )

  async getMessageContent(index: number): Promise<string> {
    return this.singleMessageContents.nth(index).text
  }

  async getMessageSender(index: number): Promise<string> {
    return this.singleMessageSenderName.nth(index).text
  }
}
