// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../../utils/page'

export class VasuTemplateEditPage {
  saveButton: Element
  constructor(readonly page: Page) {
    this.saveButton = page.findByDataQa('save-template')
  }
}
