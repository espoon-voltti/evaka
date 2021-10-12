// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import { waitUntilFalse } from '../../utils'

export default class ChildAttendancePage {
  constructor(private readonly page: Page) {}

  #presentTab = this.page.locator('[data-qa="present-tab"]')
  #markPresentButton = this.page.locator('[data-qa="mark-present-btn"]')
  #childLink = (n: number) => this.page.locator('[data-qa="child-name"]').nth(n)
  #markDepartedLink = this.page.locator('[data-qa="mark-departed-link"]')

  #markAbsentByTypeButton = (type: AbsenceType) =>
    this.page.locator(`[data-qa="mark-absent-${type}"]`)

  async selectMarkPresent() {
    await this.#markPresentButton.click()
  }

  async selectPresentTab() {
    await this.#presentTab.click()
  }

  async selectChildLink(nth: number) {
    await this.#childLink(nth).click()
  }

  async selectMarkDepartedLink() {
    await this.#markDepartedLink.click()
  }

  async selectMarkAbsentByType(type: AbsenceType) {
    await this.#markAbsentByTypeButton(type).click()
  }

  async assertMarkAbsentByTypeButtonDoesNotExist(type: AbsenceType) {
    await waitUntilFalse(() => this.#markAbsentByTypeButton(type).isVisible())
  }
}
