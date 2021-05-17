// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { UUID } from 'lib-common/types'

export default class UnitsPage {
  constructor(private readonly page: Page) {}

  async navigateToUnit(id: UUID) {
    await this.page.click(`[data-qa="unit-row"][data-id="${id}"] a`)
    await this.page.waitForSelector('[data-qa="unit-name"]', {
      state: 'visible'
    })
  }
}
