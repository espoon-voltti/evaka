// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'

export default class CitizenMessagesPage {
  readonly bulletins = Selector('[data-qa="bulletin-list-item"]')
  readonly message = (index: number) => this.bulletins.nth(index)

  readonly messageReaderTitle = Selector('[data-qa="message-reader-title"]')
  readonly messageReaderSender = Selector('[data-qa="message-reader-sender"]')
  readonly messageReaderContent = Selector('[data-qa="message-reader-content"]')
}
