// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'e2e-playwright/utils/page'
import { UUID } from 'lib-common/types'

export default class MobileListPage {
  constructor(private readonly page: Page) {}

  unreadMessagesIndicator = this.page.find(
    `[data-qa="unread-messages-indicator"]`
  )

  #comingChildrenTab = this.page.find('[data-qa="coming-tab"]')
  #presentChildrenTab = this.page.find('[data-qa="present-tab"]')
  #departedChildrenTab = this.page.find('[data-qa="departed-tab"]')

  #childRow = (childId: UUID) => this.page.find(`[data-qa="child-${childId}"]`)

  async readChildGroupName(childId: UUID) {
    const elem = this.page.find(`[data-qa="child-group-name-${childId}"]`)
    return elem.innerText
  }

  async selectChild(childId: UUID) {
    await this.#childRow(childId).click()
  }

  async openChildNotes(childId: UUID) {
    await this.#childRow(childId)
      .find('[data-qa="link-child-daycare-daily-note"]')
      .click()
  }

  async assertChildNoteDoesntExist(childId: UUID) {
    await this.#childRow(childId).waitUntilVisible()
    await this.#childRow(childId)
      .find('[data-qa="link-child-daycare-daily-note"]')
      .waitUntilHidden()
  }

  async gotoMessages() {
    const elem = this.page.find(`[data-qa="bottomnav-messages"]`)
    return elem.click()
  }

  async getAttendanceCounts() {
    const tabs = ['coming', 'present', 'departed', 'absent']
    const tabToDataQa = (t: string) => `[data-qa="${t}-tab"] [data-qa="count"]`

    const counts: Promise<[string, number]>[] = tabs.map((tab) =>
      this.page
        .find(tabToDataQa(tab))
        .innerText.then((val) => [tab, Number(val)])
    )
    const total: Promise<[string, number]> = this.page
      .find(`[data-qa="coming-tab"] [data-qa="total"]`)
      .innerText.then((val) => ['total', Number(val)])

    return Object.fromEntries(await Promise.all([...counts, total]))
  }

  #groupSelectorButton = this.page.find('[data-qa="group-selector-button"]')

  private selectedGroupElement = (id: string) =>
    this.page.find(`[data-qa="selected-group--${id}"]`)
  private groupChipElement = (id: string) =>
    this.page.find(`[data-qa="group--${id}"]`)

  async selectGroup(id: string) {
    await this.#groupSelectorButton.click()
    await this.groupChipElement(id).click()
    await this.selectedGroupElement(id).waitUntilVisible()
  }

  async selectComingChildren() {
    await this.#comingChildrenTab.click()
  }

  async selectPresentChildren() {
    await this.#presentChildrenTab.click()
  }

  async selectDepartedChildren() {
    await this.#departedChildrenTab.click()
  }
}
