// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

import { DatePicker, DateRangePicker, Page, TextInput } from '../../utils/page'

export class HolidayPeriodsPage {
  constructor(private readonly page: Page) {}

  get visiblePeriods(): Promise<string[]> {
    return this.page.findAllByDataQa('holiday-period').allInnerTexts()
  }

  #periodRows = this.page.findAllByDataQa('holiday-period-row')
  #questionnaireRows = this.page.findAllByDataQa('questionnaire-row')

  get visibleQuestionnaires(): Promise<string[]> {
    return this.#questionnaireRows.allInnerTexts()
  }

  async assertQuestionnaireContainsText(nth: number, texts: string[]) {
    const row = this.#questionnaireRows.nth(nth)
    for (const text of texts) {
      await row.findText(text).waitUntilVisible()
    }
  }

  async clickAddPeriodButton() {
    return this.page.findByDataQa('add-holiday-period-button').click()
  }

  async clickAddQuestionnaireButton() {
    return this.page.findByDataQa('add-questionnaire-button').click()
  }

  #periodInputs = {
    range: new DateRangePicker(this.page.findByDataQa('input-range')),
    reservationDeadline: new DatePicker(
      this.page.findByDataQa('input-reservation-deadline')
    )
  }

  async fillHolidayPeriodForm(params: {
    start?: LocalDate
    end?: LocalDate
    reservationDeadline?: LocalDate
  }) {
    if (params.start) {
      await this.#periodInputs.range.fillStart(params.start)
    }

    if (params.end) {
      await this.#periodInputs.range.fillEnd(params.end)
    }

    if (params.reservationDeadline) {
      await this.#periodInputs.reservationDeadline.fill(
        params.reservationDeadline
      )
    }
  }

  #questionnaireInputs = {
    activeRange: new DateRangePicker(this.page.findByDataQa('range')),
    title: new TextInput(this.page.findByDataQa('input-title-fi')),
    description: new TextInput(this.page.findByDataQa('input-description-fi')),
    descriptionSv: new TextInput(
      this.page.findByDataQa('input-description-sv')
    ),
    descriptionEn: new TextInput(
      this.page.findByDataQa('input-description-en')
    ),
    descriptionLink: new TextInput(
      this.page.findByDataQa('input-description-link-fi')
    ),
    descriptionLinkSv: new TextInput(
      this.page.findByDataQa('input-description-link-sv')
    ),
    descriptionLinkEn: new TextInput(
      this.page.findByDataQa('input-description-link-en')
    ),
    fixedPeriodOptions: new TextInput(
      this.page.findByDataQa('input-fixed-period-options')
    ),
    fixedPeriodOptionLabel: new TextInput(
      this.page.findByDataQa('input-fixed-period-option-label-fi')
    )
  }

  async fillQuestionnaireForm(params: {
    title?: string
    description?: string
    descriptionSv?: string
    descriptionEn?: string
    descriptionLink?: string
    descriptionLinkSv?: string
    descriptionLinkEn?: string
    activeRange?: FiniteDateRange
    fixedPeriodOptions?: string
    fixedPeriodOptionLabel?: string
  }) {
    for (const [key, val] of Object.entries(params)) {
      if (val !== undefined) {
        await this.#questionnaireInputs[
          key as keyof Omit<typeof params, 'period'>
        ].fill(val as string & FiniteDateRange)
      }
    }
  }

  async submit() {
    return this.page.findByDataQa('save-btn').click()
  }

  async editHolidayPeriod(nth: number) {
    return this.#periodRows.nth(nth).findByDataQa('btn-edit').click()
  }

  async deleteHolidayPeriod(nth: number) {
    await this.#periodRows.nth(nth).findByDataQa('btn-delete').click()
    return this.page.findByDataQa('modal-okBtn').click()
  }

  async editQuestionnaire(nth: number) {
    return this.#questionnaireRows.nth(nth).findByDataQa('btn-edit').click()
  }

  async deleteQuestionnaire(nth: number) {
    await this.#questionnaireRows.nth(nth).findByDataQa('btn-delete').click()
    return this.page.findByDataQa('modal-okBtn').click()
  }
}
