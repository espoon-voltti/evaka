// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import { Page } from '../../utils/page'

export default class MobileListPage {
  constructor(private readonly page: Page) {}

  unreadMessagesIndicator = this.page.findByDataQa('unread-messages-indicator')

  confirmedDaysTab = this.page.findByDataQa('confirmed-days-tab')

  comingChildrenTab = this.page.findByDataQa('coming-tab')
  presentChildrenTab = this.page.findByDataQa('present-tab')
  departedChildrenTab = this.page.findByDataQa('departed-tab')
  absentChildrenTab = this.page.findByDataQa('absent-tab')

  childRow = (childId: UUID) => this.page.findByDataQa(`child-${childId}`)

  async readChildGroupName(childId: UUID) {
    const elem = this.page.findByDataQa(`child-group-name-${childId}`)
    return elem.text
  }

  async assertChildExists(childId: UUID) {
    await this.childRow(childId).waitUntilVisible()
  }

  async selectChild(childId: UUID) {
    await this.childRow(childId).click()
  }

  async openChildNotes(childId: UUID) {
    await this.childRow(childId)
      .findByDataQa('link-child-daycare-daily-note')
      .click()
  }

  async assertChildNoteDoesntExist(childId: UUID) {
    await this.childRow(childId).waitUntilVisible()
    await this.childRow(childId)
      .findByDataQa('link-child-daycare-daily-note')
      .waitUntilHidden()
  }

  async getAttendanceCounts() {
    const tabs = ['coming', 'present', 'departed', 'absent']
    const tabToSelector = (t: string) =>
      `[data-qa="${t}-tab"] [data-qa="count"]`

    const counts: Promise<[string, number]>[] = tabs.map((tab) =>
      this.page.find(tabToSelector(tab)).text.then((val) => [tab, Number(val)])
    )
    const total: Promise<[string, number]> = this.page
      .find(`[data-qa="coming-tab"] [data-qa="total"]`)
      .text.then((val) => ['total', Number(val)])

    return Object.fromEntries(await Promise.all([...counts, total]))
  }

  groupSelectorButton = this.page.findByDataQa('group-selector-button')

  selectedGroupElement = (id: string) =>
    this.page.findByDataQa(`selected-group--${id}`)
  groupChipElement = (id: string) => this.page.findByDataQa(`group--${id}`)

  async selectGroup(id: string) {
    await this.groupSelectorButton.click()
    await this.groupChipElement(id).click()
    await this.selectedGroupElement(id).waitUntilVisible()
  }
}
