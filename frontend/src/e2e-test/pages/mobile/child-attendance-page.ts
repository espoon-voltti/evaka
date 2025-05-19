// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import type { Page, Element, ElementCollection } from '../../utils/page'
import { TextInput } from '../../utils/page'

export default class ChildAttendancePage {
  #presentTab: Element
  #markPresentButton: Element
  #markDepartedLink: Element
  markDepartedButton: Element
  #markAbsentButton: Element
  #setTimeInput: TextInput
  stickyNotes: ElementCollection
  dailyNote: Element
  groupNotes: ElementCollection
  setTimeInfo: Element
  #noChildrenIndicator: Element
  constructor(private readonly page: Page) {
    this.#presentTab = page.findByDataQa('present-tab')
    this.#markPresentButton = page.findByDataQa('mark-present-btn')
    this.#markDepartedLink = page.findByDataQa('mark-departed-link')
    this.markDepartedButton = page.findByDataQa('mark-departed-btn')
    this.#markAbsentButton = page.findByDataQa('mark-absent-btn')
    this.#setTimeInput = new TextInput(page.findByDataQa('set-time'))
    this.stickyNotes = page.findAllByDataQa('sticky-note')
    this.dailyNote = page.findByDataQa('daily-note')
    this.groupNotes = page.findAllByDataQa('group-note')
    this.setTimeInfo = page.findByDataQa('set-time-info')
    this.#noChildrenIndicator = page
      .findAll('[data-qa="no-children-indicator"]')
      .first()
  }

  #childLink = (n: number) => this.page.findAll('[data-qa="child-name"]').nth(n)

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

  // time format: "09:46"
  async setTime(time: string) {
    await this.#setTimeInput.click()
    await this.#setTimeInput.fill(time)
  }
}
