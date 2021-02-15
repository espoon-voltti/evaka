// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import { Checkbox } from '../../../utils/helpers'

export default class UnitsPage {
  readonly unitNameFilter = Selector('[data-qa="unit-name-filter"]')
  readonly includeClosedCheckbox = new Checkbox(
    Selector('[data-qa="include-closed"]')
  )
  readonly unitRows = Selector('[data-qa="unit-row"]')
  readonly firstUnitRow = this.unitRows.nth(0)

  async filterByName(name: string) {
    await t.typeText(this.unitNameFilter, name)
  }

  async navigateToNthUnit(n: number) {
    await t.click(this.unitRows.nth(n).find('a').nth(0))
  }

  async showClosedUnits(show: boolean) {
    return show === (await this.includeClosedCheckbox.checked)
      ? undefined
      : this.includeClosedCheckbox.click()
  }
}
