// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilTrue } from '../../utils'
import { Element, Page } from '../../utils/page'

import MobileMessageEditor from './message-editor'

export default class MobileMessagesPage {
  constructor(private readonly page: Page) {}

  messagesContainer = this.page.findByDataQa('messages-page-content-area')
  noAccountInfo = this.page.findByDataQa('info-no-account-access')

  receivedTab = this.page.findByDataQa('received-tab')
  sentTab = this.page.findByDataQa('sent-tab')
  draftsTab = this.page.findByDataQa('drafts-tab')

  threads = this.page.findAllByDataQa('message-preview')
  titles = this.page.findAllByDataQa('message-preview-title')
  newMessage = this.page.findByDataQa('new-message-btn')

  async getThreadTitle(index: number) {
    return this.titles.nth(index).text
  }

  async assertThreadsExist() {
    await waitUntilTrue(async () => (await this.threads.count()) > 0)
  }

  thread(nth: number) {
    return new ReceivedThreadPreview(this.threads.nth(nth))
  }

  async openSentTab() {
    await this.sentTab.click()
    return new SentTab(this.page)
  }

  async openDraftsTab() {
    await this.draftsTab.click()
    return new DraftsTab(this.page)
  }
}

export class ReceivedThreadPreview extends Element {
  draftIndicator = this.findByDataQa('draft-indicator')
}

export class SentTab {
  constructor(private readonly page: Page) {}

  messages = this.page.findAllByDataQa('sent-message-preview')

  message(nth: number) {
    return new SentMessagePreview(this.page, this.messages.nth(nth))
  }
}

export class SentMessagePreview extends Element {
  constructor(
    private readonly page: Page,
    el: Element
  ) {
    super(el)
  }

  title = this.findByDataQa('message-preview-title')

  async openMessage() {
    await this.click()
    return new SentMessage(this.page)
  }
}

export class SentMessage {
  constructor(private readonly page: Page) {}

  topBarTitle = this.page.findByDataQa('top-bar-title')
  content = this.page.findByDataQa('single-message-content')
}

export class DraftsTab {
  constructor(private readonly page: Page) {}

  list = this.page.findByDataQa('draft-list')
  messages = this.list.findAllByDataQa('draft-message-preview')

  message(nth: number) {
    return new DraftMessagePreview(this.page, this.messages.nth(nth))
  }
}

class DraftMessagePreview extends Element {
  constructor(
    private readonly page: Page,
    el: Element
  ) {
    super(el)
  }

  title = this.findByDataQa('message-preview-title')

  async editDraft() {
    await this.click()
    return new MobileMessageEditor(this.page)
  }
}
