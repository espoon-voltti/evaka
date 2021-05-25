// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RawElement } from 'e2e-playwright/utils/element'
import { UUID } from 'lib-common/types'
import { Page } from 'playwright'

export default class MobileListPage {
  constructor(private readonly page: Page) {}

  async selectChild(childId: UUID) {
    const elem = new RawElement(this.page, `[data-qa="child-${childId}"]`)
    return elem.click()
  }
}
