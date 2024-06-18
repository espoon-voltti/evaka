// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilTrue } from '../../utils'
import { Element, Page, ElementCollection } from '../../utils/page'

import MobileMessageEditor from './message-editor'

export default class MobileMessagesPage {
  messagesContainer: Element
  noAccountInfo: Element
  receivedTab: Element
  sentTab: Element
  draftsTab: Element
  threads: ElementCollection
  titles: ElementCollection
  newMessage: Element
  constructor(private readonly page: Page) {
    this.messagesContainer = page.findByDataQa('messages-page-content-area')
    this.noAccountInfo = page.findByDataQa('info-no-account-access')
    this.receivedTab = page.findByDataQa('received-tab')
    this.sentTab = page.findByDataQa('sent-tab')
    this.draftsTab = page.findByDataQa('drafts-tab')
    this.threads = page.findAllByDataQa('message-preview')
    this.titles = page.findAllByDataQa('message-preview-title')
    this.newMessage = page.findByDataQa('new-message-btn')
  }

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
  messages: ElementCollection
  constructor(private readonly page: Page) {
    this.messages = page.findAllByDataQa('sent-message-preview')
  }

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
  topBarTitle: Element
  content: Element
  constructor(private readonly page: Page) {
    this.topBarTitle = page.findByDataQa('top-bar-title')
    this.content = page.findByDataQa('single-message-content')
  }
}

export class DraftsTab {
  list: Element
  constructor(private readonly page: Page) {
    this.list = page.findByDataQa('draft-list')
  }

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
