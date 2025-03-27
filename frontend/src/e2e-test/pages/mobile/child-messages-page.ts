// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element, ElementCollection } from '../../utils/page'

import { ReceivedThreadPreview } from './messages'

export default class MobileChildMessagesPage {
  newMessageButton: Element
  backButton: Element
  threads: ElementCollection

  constructor(page: Page) {
    this.newMessageButton = page.findByDataQa('new-message-button')
    this.backButton = page.findByDataQa('child-name-back-button')
    this.threads = page.findAllByDataQa('message-preview')
  }

  thread(nth: number) {
    return new ReceivedThreadPreview(this.threads.nth(nth))
  }
}
