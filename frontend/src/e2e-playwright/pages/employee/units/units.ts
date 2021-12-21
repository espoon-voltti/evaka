// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { Page } from '../../../utils/page'
import { UnitEditor } from './unit'

export default class UnitsPage {
  constructor(private readonly page: Page) {}

  #createNewUnitButton = this.page.find('[data-qa="create-new-unit"]')

  async navigateToUnit(id: UUID) {
    await this.page.find(`[data-qa="unit-row"][data-id="${id}"] a`).click()
    await this.page.find('[data-qa="unit-name"]').waitUntilVisible()
  }

  async openNewUnitEditor() {
    await this.#createNewUnitButton.click()
    return new UnitEditor(this.page)
  }
}
