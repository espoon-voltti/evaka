// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type LocalDate from 'lib-common/local-date'

import config from '../../../config'
import type { Page, Element } from '../../../utils/page'
import { DatePicker } from '../../../utils/page'

export class OutOfOfficePage {
  #outOfOffice: Element
  #outOfOfficeEditor: Element
  #addButton: Element
  #saveButton: Element
  #editButton: Element
  #removeButton: Element

  constructor(page: Page) {
    this.#outOfOffice = page.findByDataQa('out-of-office-page')
    this.#outOfOfficeEditor = page.findByDataQa('out-of-office-editor')
    this.#addButton = page.findByDataQa('add-out-of-office')
    this.#saveButton = page.findByDataQa('save-out-of-office')
    this.#editButton = page.findByDataQa('edit-out-of-office')
    this.#removeButton = page.findByDataQa('remove-out-of-office')
  }

  static async open(page: Page) {
    await page.goto(config.employeeUrl + '/out-of-office')
    return new OutOfOfficePage(page)
  }

  async addOutOfOfficePeriod(startDate: LocalDate, endDate: LocalDate) {
    await this.#addButton.click()

    const startInput = new DatePicker(
      this.#outOfOfficeEditor.findAll('input').first()
    )
    const endInput = new DatePicker(
      this.#outOfOfficeEditor.findAll('input').last()
    )

    await startInput.fill(startDate)
    await endInput.fill(endDate)
    await this.#saveButton.click()
  }

  async editStartOfPeriod(newStartDate: LocalDate) {
    await this.#editButton.click()
    const startInput = new DatePicker(
      this.#outOfOfficeEditor.findAll('input').first()
    )
    await startInput.fill(newStartDate)
    await this.#saveButton.click()
  }

  async removeOutOfOfficePeriod() {
    await this.#removeButton.click()
  }

  async assertNoPeriods() {
    await this.#outOfOffice.assertText((t) =>
      t.includes('Ei tulevia poissaoloja')
    )
  }

  async assertPeriodExists(startDate: LocalDate, endDate: LocalDate) {
    const periodText =
      FiniteDateRange.tryCreate(startDate, endDate)?.format() ?? 'error'
    await this.#outOfOffice.assertText(
      (t) => t.includes(periodText) && !t.includes('Ei tulevia poissaoloja')
    )
  }

  async assertPeriodDoesNotExist(startDate: LocalDate, endDate: LocalDate) {
    const periodText =
      FiniteDateRange.tryCreate(startDate, endDate)?.format() ?? 'error'
    await this.#outOfOffice.assertText((t) => !t.includes(periodText))
  }
}
