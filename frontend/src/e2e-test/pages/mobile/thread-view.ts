// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput } from '../../utils/page'

export default class ThreadViewPage {
  constructor(private readonly page: Page) {}

  goBack = this.page.findByDataQa('go-back')
  replyButton = this.page.findByDataQa('message-reply-editor-btn')
  replyContent = new TextInput(this.page.findByDataQa('message-reply-content'))
  sendReplyButton = this.page.findByDataQa('message-send-btn')
  discardReplyButton = this.page.findByDataQa('message-discard-btn')

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
