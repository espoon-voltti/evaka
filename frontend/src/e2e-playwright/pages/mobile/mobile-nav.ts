// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RawElementDEPRECATED } from 'e2e-playwright/utils/element'
import { UUID } from 'lib-common/types'
import { Page } from 'playwright'

export default class MobileNav {
  constructor(private readonly page: Page) {}

  readonly #groupSelectorButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="group-selector-button"]'
  )

  private groupWithId(id: UUID) {
    return new RawElementDEPRECATED(this.page, `[data-qa="group--${id}"]`)
  }

  readonly #children = new RawElementDEPRECATED(
    this.page,
    '[data-qa="bottomnav-children"]'
  )
  readonly #staff = new RawElementDEPRECATED(
    this.page,
    '[data-qa="bottomnav-staff"]'
  )

  get selectedGroupName() {
    return this.#groupSelectorButton.innerText
  }

  async selectGroup(id: UUID) {
    await this.#groupSelectorButton.click()
    await this.groupWithId(id).click()
  }

  async openPage(tab: 'children' | 'staff') {
    switch (tab) {
      case 'children':
        return await this.#children.click()
      case 'staff':
        return await this.#staff.click()
    }
  }
}
