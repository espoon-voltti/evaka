// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../../utils'
import { Page, Select, TextInput, Element } from '../../../utils/page'

export class VasuTemplatesListPage {
  addTemplateButton: Element
  nameInput: TextInput
  selectType: Select
  okButton: Element
  templateTable: Element
  constructor(readonly page: Page) {
    this.addTemplateButton = page.findByDataQa('add-button')
    this.nameInput = new TextInput(page.findByDataQa('template-name'))
    this.selectType = new Select(page.findByDataQa('select-type'))
    this.okButton = page.findByDataQa('modal-okBtn')
    this.templateTable = page.findByDataQa('template-table')
  }

  templateRows = this.page.findAll('[data-qa="template-row"]')

  async assertTemplateRowCount(expected: number) {
    await this.templateTable.waitUntilVisible()
    await waitUntilEqual(() => this.templateRows.count(), expected)
  }

  async assertTemplate(nth: number, expectedName: string) {
    await this.templateRows
      .nth(nth)
      .find('[data-qa="template-name"]')
      .assertTextEquals(expectedName)
  }
}
