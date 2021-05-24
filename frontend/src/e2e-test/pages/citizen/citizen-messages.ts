// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'

export default class CitizenMessagesPage {
  readonly threads = Selector('[data-qa="thread-list-item"]')
  readonly thread = (index: number) => this.threads.nth(index)

  readonly messageReaderTitle = Selector('[data-qa="thread-reader-title"]')
  readonly messageReaderSender = Selector('[data-qa="thread-reader-sender"]')
  readonly messageReaderContent = Selector('[data-qa="thread-reader-content"]')
}
