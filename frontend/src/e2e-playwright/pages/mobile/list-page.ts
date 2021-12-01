// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { Page } from 'playwright'

export default class MobileListPage {
  constructor(private readonly page: Page) {}

  async selectChild(childId: UUID) {
    const elem = this.page.locator(`[data-qa="child-${childId}"]`)
    await elem.click()
  }

  async gotoMessages() {
    const elem = this.page.locator(`[data-qa="bottomnav-messages"]`)
    await elem.click()
  }
}
