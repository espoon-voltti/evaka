// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { Page, TextInput, Element, ElementCollection } from '../../utils/page'

export default class ChildAttendancePage {
  #presentTab: Element
  #markPresentButton: Element
  #markDepartedLink: Element
  markDepartedButton: Element
  #markAbsentButton: Element
  #childStatusLabel: Element
  #setTimeInput: TextInput
  groupNote: ElementCollection
  setTimeInfo: Element
  constructor(private readonly page: Page) {
    this.#presentTab = page.findByDataQa('present-tab')
    this.#markPresentButton = page.findByDataQa('mark-present-btn')
    this.#markDepartedLink = page.findByDataQa('mark-departed-link')
    this.markDepartedButton = page.findByDataQa('mark-departed-btn')
    this.#markAbsentButton = page.findByDataQa('mark-absent-btn')
    this.#childStatusLabel = page.findByDataQa('child-status')
    this.#setTimeInput = new TextInput(page.findByDataQa('set-time'))
    this.groupNote = page.findAllByDataQa('group-note')
    this.setTimeInfo = page.findByDataQa('set-time-info')
  }

  #childLink = (n: number) => this.page.findAll('[data-qa="child-name"]').nth(n)
  #noChildrenIndicator = this.page
    .findAll('[data-qa="no-children-indicator"]')
    .first()

  #markAbsentByTypeButton = (type: AbsenceType) =>
    this.page.findByDataQa(`mark-absent-${type}`)

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
