// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { Checkbox, Element, Page, TextInput } from '../../../utils/page'
import { UnitEditor, UnitPage } from './unit'
import config from '../../../../e2e-test-common/config'
import { waitUntilEqual } from '../../../utils'

export default class UnitsPage {
  constructor(private readonly page: Page) {}

  #createNewUnitButton = this.page.find('[data-qa="create-new-unit"]')

  static async open(page: Page) {
    await page.goto(config.employeeUrl + '/units')
    return new UnitsPage(page)
  }

  #unitNameFilter = new TextInput(
    this.page.find('[data-qa="unit-name-filter"]')
  )

  #showClosedUnits = new Checkbox(this.page.find('[data-qa="include-closed"]'))

  async filterByName(text: string) {
    await this.#unitNameFilter.fill(text)
  }

  async showClosedUnits(show: boolean) {
    if (show) {
      await this.#showClosedUnits.check()
    } else {
      await this.#showClosedUnits.uncheck()
    }
  }

  #table = this.page.find('[data-qa="table-of-units"]')
  #rows = this.#table.findAll('[data-qa="unit-row"]')

  async assertRowCount(expectedCount: number) {
    await waitUntilEqual(() => this.#rows.count(), expectedCount)
  }

  unitRow(id: UUID) {
    return new UnitRow(
      this.page,
      this.page.find(`[data-qa="unit-row"][data-id="${id}"]`)
    )
  }

  nthUnitRow(nth: number) {
    return new UnitRow(this.page, this.#rows.nth(nth))
  }

  async openNewUnitEditor() {
    await this.#createNewUnitButton.click()
    return new UnitEditor(this.page)
  }
}

export class UnitRow extends Element {
  constructor(private page: Page, root: Element) {
    super(root)
  }

  #cols = this.findAll('td')
  #name = this.#cols.nth(0)
  #visitingAddress = this.#cols.nth(2)
  #link = this.find('a')

  async assertFields(fields: { name?: string; visitingAddress?: string }) {
    if (fields.name !== undefined) {
      await waitUntilEqual(() => this.#name.innerText, fields.name)
    }
    if (fields.visitingAddress !== undefined) {
      await waitUntilEqual(
        () => this.#visitingAddress.innerText,
        fields.visitingAddress
      )
    }
  }

  async openUnit() {
    await this.#link.click()
    const unitPage = new UnitPage(this.page)
    await unitPage.waitUntilLoaded()
    return unitPage
  }
}
