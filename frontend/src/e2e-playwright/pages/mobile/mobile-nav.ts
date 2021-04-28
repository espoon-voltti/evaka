// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RawElement } from 'e2e-playwright/utils/element'
import { UUID } from 'lib-common/types'
import { Page } from 'playwright'

export default class MobileNav {
  constructor(private readonly page: Page) {}

  readonly #groupSelectorButton = new RawElement(
    this.page,
    '[data-qa="group-selector-button"]'
  )

  private groupWithId(id: UUID) {
    return new RawElement(this.page, `[data-qa="group--${id}"]`)
  }

  readonly #children = new RawElement(
    this.page,
    '[data-qa="bottomnav-children"]'
  )
  readonly #staff = new RawElement(this.page, '[data-qa="bottomnav-staff"]')

  readonly #staffCountBubble = new RawElement(
    this.page,
    '[data-qa="staff-count-bubble"]'
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

  get staffCount() {
    return this.#staffCountBubble.innerText
  }
}
