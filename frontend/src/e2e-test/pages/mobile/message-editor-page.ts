// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../utils/page'

export default class MessageEditorPage {
  noReceiversInfo: Element
  constructor(readonly page: Page) {
    this.noReceiversInfo = page.findByDataQa('info-no-receivers')
  }
}
