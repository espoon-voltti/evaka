// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RawElement } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class MobileChildPage {
  constructor(private readonly page: Page) {}

  #markAbsentBeforehandLink = new RawElement(
    this.page,
    '[data-qa="mark-absent-beforehand"]'
  )

  async markFutureAbsences() {
    return this.#markAbsentBeforehandLink.click()
  }
}
