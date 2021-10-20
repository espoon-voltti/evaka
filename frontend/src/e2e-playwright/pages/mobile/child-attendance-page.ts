// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { AbsenceType } from 'lib-common/generated/api-types/daycare'
import { waitUntilEqual, waitUntilTrue } from '../../utils'

export default class ChildAttendancePage {
  constructor(private readonly page: Page) {}

  #presentTab = this.page.locator('[data-qa="present-tab"]')
  #markPresentButton = this.page.locator('[data-qa="mark-present-btn"]')
  #childLink = (n: number) => this.page.locator('[data-qa="child-name"]').nth(n)
  #markDepartedLink = this.page.locator('[data-qa="mark-departed-link"]')
  #markDepartedButton = this.page.locator('[data-qa="mark-departed-btn"]')
  #markDepartedWithAbsenceButton = this.page.locator(
    '[data-qa="mark-departed-with-absence-btn"]'
  )
  #noChildrenIndicator = this.page
    .locator('[data-qa="no-children-indicator"]')
    .first()
  #childStatusLabel = this.page.locator('[data-qa="child-status"]')
  #nonAbsenceActions = this.page.locator('[data-qa="non-absence-actions"]')

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

  async selectMarkDepartedButton() {
    await this.#markDepartedButton.click()
  }

  async selectMarkDepartedWithAbsenceButton() {
    await this.#markDepartedWithAbsenceButton.click()
  }

  async selectMarkAbsentByType(type: AbsenceType) {
    await this.#markAbsentByTypeButton(type).click()
  }

  async assertMarkAbsenceTypeButtonsNotShown() {
    await waitUntilTrue(() => this.#nonAbsenceActions.isVisible())
  }

  async assertMarkAbsenceTypeButtonsAreShown(type: AbsenceType) {
    await waitUntilTrue(() => this.#markAbsentByTypeButton(type).isVisible())
  }

  async assertNoChildrenPresentIndicatorIsShown() {
    await waitUntilTrue(() => this.#noChildrenIndicator.isVisible())
  }

  async assertChildStatusLabelIsShown(expectedText: string) {
    await waitUntilEqual(() => this.#childStatusLabel.innerText(), expectedText)
  }
}
