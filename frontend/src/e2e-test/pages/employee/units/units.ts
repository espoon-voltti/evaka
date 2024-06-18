// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import config from '../../../config'
import { waitUntilEqual } from '../../../utils'
import {
  Checkbox,
  Element,
  MultiSelect,
  Page,
  TextInput
} from '../../../utils/page'

import { UnitEditor, UnitPage } from './unit'

export default class UnitsPage {
  #createNewUnitButton: Element
  #unitNameFilter: TextInput
  providerTypesSelect: MultiSelect
  careTypesSelect: MultiSelect
  #showClosedUnits: Checkbox
  #table: Element
  constructor(private readonly page: Page) {
    this.#createNewUnitButton = page.findByDataQa('create-new-unit')
    this.#unitNameFilter = new TextInput(page.findByDataQa('unit-name-filter'))
    this.providerTypesSelect = new MultiSelect(
      page.findByDataQa('provider-types-select')
    )
    this.careTypesSelect = new MultiSelect(
      page.findByDataQa('care-types-select')
    )
    this.#showClosedUnits = new Checkbox(page.findByDataQa('include-closed'))
    this.#table = page.findByDataQa('table-of-units')
  }

  static async open(page: Page) {
    await page.goto(config.employeeUrl + '/units')
    return new UnitsPage(page)
  }

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

  #rows = this.#table.findAll('[data-qa="unit-row"]')

  async assertRowCount(expectedCount: number) {
    await waitUntilEqual(() => this.#rows.count(), expectedCount)
  }

  unitRow(id: UUID) {
    return new UnitRow(
      this.page,
      this.page.findByDataQa(`unit-row"][data-id="${id}`)
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
  constructor(
    private page: Page,
    root: Element
  ) {
    super(root)
  }

  #cols = this.findAll('td')
  #name = this.#cols.nth(0)
  #visitingAddress = this.#cols.nth(2)
  #link = this.find('a')

  async assertFields(fields: { name?: string; visitingAddress?: string }) {
    if (fields.name !== undefined) {
      await this.#name.assertTextEquals(fields.name)
    }
    if (fields.visitingAddress !== undefined) {
      await this.#visitingAddress.assertTextEquals(fields.visitingAddress)
    }
  }

  async openUnit() {
    await this.#link.click()
    const unitPage = new UnitPage(this.page)
    await unitPage.waitUntilLoaded()
    return unitPage
  }
}
