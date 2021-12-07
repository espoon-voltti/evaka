// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { Page } from '../../../utils/page'

export default class UnitsPage {
  constructor(private readonly page: Page) {}

  async navigateToUnit(id: UUID) {
    await this.page.find(`[data-qa="unit-row"][data-id="${id}"] a`).click()
    await this.page.find('[data-qa="unit-name"]').waitUntilVisible()
  }
}
