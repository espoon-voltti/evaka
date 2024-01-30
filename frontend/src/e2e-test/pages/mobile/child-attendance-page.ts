// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { Page, TextInput } from '../../utils/page'

export default class ChildAttendancePage {
  constructor(private readonly page: Page) {}

  #presentTab = this.page.find('[data-qa="present-tab"]')
  #markPresentButton = this.page.find('[data-qa="mark-present-btn"]')
  #childLink = (n: number) => this.page.findAll('[data-qa="child-name"]').nth(n)
  #markDepartedLink = this.page.find('[data-qa="mark-departed-link"]')
  markDepartedButton = this.page.find('[data-qa="mark-departed-btn"]')
  #markAbsentButton = this.page.find('[data-qa="mark-absent-btn"]')
  #noChildrenIndicator = this.page
    .findAll('[data-qa="no-children-indicator"]')
    .first()
  #childStatusLabel = this.page.find('[data-qa="child-status"]')
  #setTimeInput = new TextInput(this.page.find('[data-qa="set-time"]'))
  groupNote = this.page.findAllByDataQa('group-note')

  setTimeInfo = this.page.findByDataQa('set-time-info')

  #markAbsentByTypeButton = (type: AbsenceType) =>
    this.page.find(`[data-qa="mark-absent-${type}"]`)

  markAbsentByCategoryAndTypeButton = (
    category: AbsenceCategory,
    type: AbsenceType | 'NO_ABSENCE'
  ) =>
    this.page
      .findByDataQa(`absence-${category}`)
      .findByDataQa(`mark-absent-${type}`)

  async selectMarkPresent() {
    await this.#markPresentButton.click()
  }

  async assertMarkPresentButtonDisabled(disabled: boolean) {
    await waitUntilEqual(() => this.#markPresentButton.disabled, disabled)
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

  async selectMarkAbsentButton() {
    await this.#markAbsentButton.click()
  }

  async assertNoChildrenPresentIndicatorIsShown() {
    await waitUntilTrue(() => this.#noChildrenIndicator.visible)
  }

  async assertChildStatusLabelIsShown(expectedText: string) {
    await this.#childStatusLabel.assertTextEquals(expectedText)
  }

  // time format: "09:46"
  async setTime(time: string) {
    await this.#setTimeInput.click()
    await this.#setTimeInput.fill(time)
  }
}
