// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import { Page } from '../../utils/page'

export default class MobileNav {
  constructor(private readonly page: Page) {}

  readonly #groupSelectorButton = this.page.find(
    '[data-qa="group-selector-button"]'
  )

  private groupWithId(id: UUID) {
    return this.page.find(`[data-qa="group--${id}"]`)
  }

  children = this.page.find('[data-qa="bottomnav-children"]')
  staff = this.page.find('[data-qa="bottomnav-staff"]')
  messages = this.page.find('[data-qa="bottomnav-messages"]')
  settings = this.page.find('[data-qa="bottomnav-settings"]')

  get selectedGroupName() {
    return this.#groupSelectorButton.text
  }

  async selectGroup(id: UUID) {
    await this.#groupSelectorButton.click()
    await this.groupWithId(id).click()
  }
}
