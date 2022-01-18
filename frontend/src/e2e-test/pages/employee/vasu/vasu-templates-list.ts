// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../../utils'
import { Page, Select, TextInput } from '../../../utils/page'

export class VasuTemplatesListPage {
  constructor(readonly page: Page) {}

  addTemplateButton = this.page.find('[data-qa="add-button"]')
  nameInput = new TextInput(this.page.find('[data-qa="template-name"]'))
  selectType = new Select(this.page.find('[data-qa="select-type"]'))
  okButton = this.page.find('[data-qa="modal-okBtn"]')
  templateRows = this.page.findAll('[data-qa="template-row"]')
  templateTable = this.page.find('[data-qa="template-table"]')

  async assertTemplateRowCount(expected: number) {
    await this.templateTable.waitUntilVisible()
    await waitUntilEqual(() => this.templateRows.count(), expected)
  }

  async assertTemplate(nth: number, expectedName: string) {
    await waitUntilEqual(
      () =>
        this.templateRows.nth(nth).find('[data-qa="template-name"]')
          .textContent,
      expectedName
    )
  }
}
