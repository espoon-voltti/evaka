// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import { Page } from '../../utils/page'

export default class MobileNav {
  constructor(private readonly page: Page) {}

  readonly #groupSelectorButton = this.page.findByDataQa(
    'group-selector-button'
  )

  private groupWithId(id: UUID) {
    return this.page.findByDataQa(`group--${id}`)
  }

  children = this.page.findByDataQa('bottomnav-children')
  staff = this.page.findByDataQa('bottomnav-staff')
  messages = this.page.findByDataQa('bottomnav-messages')
  settings = this.page.findByDataQa('bottomnav-settings')

  get selectedGroupName() {
    return this.#groupSelectorButton.text
  }

  async selectGroup(id: UUID) {
    await this.#groupSelectorButton.click()
    await this.groupWithId(id).click()
  }
}
