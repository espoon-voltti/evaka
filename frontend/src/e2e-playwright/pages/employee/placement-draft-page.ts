// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../utils/page'

export class PlacementDraftPage {
  constructor(private page: Page) {}

  #restrictedDetailsWarning = this.page.find(
    '[data-qa="restricted-details-warning"]'
  )

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="placement-draft-page"][data-isloading="false"]')
      .waitUntilVisible()
    await this.page
      .find('[data-qa="placement-item"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async assertRestrictedDetailsWarning() {
    await this.#restrictedDetailsWarning.waitUntilVisible()
  }
}
