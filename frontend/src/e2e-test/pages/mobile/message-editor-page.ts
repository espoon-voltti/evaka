// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page, Element } from '../../utils/page'

export default class MessageEditorPage {
  noRecipientsInfo: Element
  constructor(readonly page: Page) {
    this.noRecipientsInfo = page.findByDataQa('info-no-recipients')
  }
}
