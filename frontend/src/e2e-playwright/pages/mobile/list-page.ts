// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RawElementDEPRECATED } from 'e2e-playwright/utils/element'
import { UUID } from 'lib-common/types'
import { Page } from 'playwright'

export default class MobileListPage {
  constructor(private readonly page: Page) {}

  async selectChild(childId: UUID) {
    const elem = new RawElementDEPRECATED(
      this.page,
      `[data-qa="child-${childId}"]`
    )
    return elem.click()
  }

  async gotoMessages() {
    const elem = new RawElementDEPRECATED(
      this.page,
      `[data-qa="bottomnav-messages"]`
    )
    return elem.click()
  }

  async getAttendanceCounts() {
    const tabs = ['coming', 'present', 'departed', 'absent']
    const tabToDataQa = (t: string) => `[data-qa="${t}-tab"] [data-qa="count"]`

    const counts: Promise<[string, number]>[] = tabs.map((tab) =>
      new RawElementDEPRECATED(this.page, tabToDataQa(tab)).innerText.then(
        (val) => [tab, Number(val)]
      )
    )
    const total: Promise<[string, number]> = new RawElementDEPRECATED(
      this.page,
      `[data-qa="coming-tab"] [data-qa="total"]`
    ).innerText.then((val) => ['total', Number(val)])

    return Object.fromEntries(await Promise.all([...counts, total]))
  }

  #groupSelectorButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="group-selector-button"]'
  )
  private selectedGroupElement = (id: string) =>
    new RawElementDEPRECATED(this.page, `[data-qa="selected-group--${id}"]`)
  private groupChipElement = (id: string) =>
    new RawElementDEPRECATED(this.page, `[data-qa="group--${id}"]`)

  async selectGroup(id: string) {
    await this.#groupSelectorButton.click()
    await this.groupChipElement(id).click()
    await this.selectedGroupElement(id).waitUntilVisible()
  }
}
