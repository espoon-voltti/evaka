// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../utils/page'

export default class MobileChildMessagesPage {
  newMessageButton: Element
  backButton: Element

  constructor(page: Page) {
    this.newMessageButton = page.findByDataQa('new-message-button')
    this.backButton = page.findByDataQa('child-name-back-button')
  }
}
