// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import { Page, Element } from '../../utils/page'

export default class MobileNav {
  #groupSelectorButton: Element
  children: Element
  staff: Element
  messages: Element
  settings: Element
  constructor(private readonly page: Page) {
    this.#groupSelectorButton = page.findByDataQa('group-selector-button')
    this.children = page.findByDataQa('bottomnav-children')
    this.staff = page.findByDataQa('bottomnav-staff')
    this.messages = page.findByDataQa('bottomnav-messages')
    this.settings = page.findByDataQa('bottomnav-settings')
  }

  private groupWithId(id: UUID) {
    return this.page.findByDataQa(`group--${id}`)
  }

  get selectedGroupName() {
    return this.#groupSelectorButton.text
  }

  async selectGroup(id: UUID) {
    await this.#groupSelectorButton.click()
    await this.groupWithId(id).click()
  }
}
