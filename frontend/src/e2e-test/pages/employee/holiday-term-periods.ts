// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

import {
  Checkbox,
  DatePicker,
  Page,
  Radio,
  TextInput,
  ElementCollection,
  Element
} from '../../utils/page'

export class HolidayAndTermPeriodsPage {
  #periodRows: ElementCollection
  #questionnaireRows: ElementCollection
  #preschoolTermRows: ElementCollection
  #clubTermRows: ElementCollection
  confirmCheckbox: Checkbox
  #addTermBreakButton: Element
  #termBreakRows: ElementCollection
  constructor(private readonly page: Page) {
    this.#periodRows = page.findAllByDataQa('holiday-period-row')
    this.#questionnaireRows = page.findAllByDataQa('questionnaire-row')
    this.#preschoolTermRows = page.findAllByDataQa('preschool-term-row')
    this.#clubTermRows = page.findAllByDataQa('club-term-row')
    this.confirmCheckbox = new Checkbox(page.findByDataQa('confirm-checkbox'))
    this.#addTermBreakButton = page.findByDataQa('add-term-break-button')
    this.#termBreakRows = page.findAllByDataQa('term-break')
  }

  get visiblePeriods(): Promise<string[]> {
    return this.page.findAllByDataQa('holiday-period').allTexts()
  }

  get visibleClubTermPeriods(): Promise<string[]> {
    return this.page.findAllByDataQa('term').allTexts()
  }

  get visiblePreschoolTermPeriods(): Promise<string[]> {
    return this.page.findAllByDataQa('finnish-preschool').allTexts()
  }

  get visibleExtendedTermStartDates(): Promise<string[]> {
    return this.page.findAllByDataQa('extended-term-start').allTexts()
  }

  get visibleApplicationPeriodStartDates(): Promise<string[]> {
    return this.page.findAllByDataQa('application-period-start').allTexts()
  }

  async visibleTermBreakByDate(date: LocalDate): Promise<string[]> {
    return this.page
      .findAllByDataQa(`term-break-${date.formatIso()}`)
      .allTexts()
  }

  get visibleQuestionnaires(): Promise<string[]> {
    return this.#questionnaireRows.allTexts()
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

  async clickAddPreschoolTermButton() {
    return this.page.findByDataQa('add-preschool-term-button').click()
  }

  async clickAddClubTermButton() {
    return this.page.findByDataQa('add-club-term-button').click()
  }

  #periodInputs = {
    start: new DatePicker(
      this.page.findByDataQa('period').findAll('input').first()
    ),
    end: new DatePicker(
      this.page.findByDataQa('period').findAll('input').last()
    ),
    reservationDeadline: new DatePicker(
      this.page.findByDataQa('input-reservation-deadline')
    )
  }

  async fillHolidayPeriodForm(params: {
    start?: string
    end?: string
    reservationDeadline?: string
  }) {
    for (const [key, val] of Object.entries(params)) {
      if (val !== undefined) {
        await this.#periodInputs[key as keyof typeof params].fill(val)
      }
    }
  }

  #questionnaireInputs = {
    activeStart: new DatePicker(this.page.findByDataQa('input-start')),
    activeEnd: new DatePicker(this.page.findByDataQa('input-end')),
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
    activeStart?: string
    activeEnd?: string
    fixedPeriodOptions?: string
    fixedPeriodOptionLabel?: string
  }) {
    for (const [key, val] of Object.entries(params)) {
      if (val !== undefined) {
        if (key === 'period') {
          await new Radio(this.page.findByDataQa(`period-${val}`)).click()
        } else {
          await this.#questionnaireInputs[
            key as keyof Omit<typeof params, 'period'>
          ].fill(val)
        }
      }
    }
  }

  async fillPreschoolTermForm(params: {
    finnishPreschoolStart?: string
    finnishPreschoolEnd?: string
    extendedTermStart?: string
    applicationPeriodStart?: string
    termBreaks?: FiniteDateRange[]
  }) {
    const { termBreaks, ...baseInputs } = params
    for (const [key, val] of Object.entries(baseInputs)) {
      if (val !== undefined) {
        await this.#preschoolTermInputs[key as keyof typeof baseInputs].fill(
          val
        )
      }
    }

    if (termBreaks && termBreaks.length > 0) {
      for (const [i, termBreak] of termBreaks.entries()) {
        await this.#addTermBreakButton.click()
        const startInput = new DatePicker(
          this.#termBreakRows
            .nth(i)
            .findByDataQa(`term-break-input`)
            .findAll('input')
            .first()
        )
        const endInput = new DatePicker(
          this.#termBreakRows
            .nth(i)
            .findByDataQa(`term-break-input`)
            .findAll('input')
            .last()
        )
        await startInput.fill(termBreak.start.format())
        await endInput.fill(termBreak.end.format())
      }
    }
  }

  #preschoolTermInputs = {
    finnishPreschoolStart: new DatePicker(
      this.page.findByDataQa('finnish-preschool').findAll('input').first()
    ),
    finnishPreschoolEnd: new DatePicker(
      this.page.findByDataQa('finnish-preschool').findAll('input').last()
    ),
    extendedTermStart: new DatePicker(
      this.page.findByDataQa('input-extended-term-start')
    ),
    applicationPeriodStart: new DatePicker(
      this.page.findByDataQa('input-application-period-start')
    )
  }

  async removeTermBreakEntry(nth: number) {
    await this.#termBreakRows
      .nth(nth)
      .findByDataQa(`remove-term-break-button`)
      .click()
  }

  async editTermBreakInput(nth: number, range: FiniteDateRange) {
    const startInput = new DatePicker(
      this.#termBreakRows
        .nth(nth)
        .findByDataQa(`term-break-input`)
        .findAll('input')
        .first()
    )
    const endInput = new DatePicker(
      this.#termBreakRows
        .nth(nth)
        .findByDataQa(`term-break-input`)
        .findAll('input')
        .last()
    )
    await startInput.fill(range.start.format())
    await endInput.fill(range.end.format())
  }

  async editPreschoolTerm(nth: number) {
    return this.#preschoolTermRows.nth(nth).findByDataQa('btn-edit').click()
  }

  async deletePreschoolTerm(nth: number) {
    return await this.#preschoolTermRows
      .nth(nth)
      .findByDataQa('btn-delete')
      .click()
  }

  async confirmPreschoolTermModal() {
    await this.page.findByDataQa('modal').findByDataQa('modal-okBtn').click()
  }

  async fillClubTermForm(params: {
    termStart?: string
    termEnd?: string
    applicationPeriodStart?: string
    termBreaks?: FiniteDateRange[]
  }) {
    const { termBreaks, ...baseInputs } = params
    for (const [key, val] of Object.entries(baseInputs)) {
      if (val !== undefined) {
        await this.#clubTermInputs[key as keyof typeof baseInputs].fill(val)
      }
    }

    if (termBreaks && termBreaks.length > 0) {
      for (const [i, termBreak] of termBreaks.entries()) {
        await this.#addTermBreakButton.click()
        const startInput = new DatePicker(
          this.#termBreakRows
            .nth(i)
            .findByDataQa(`term-break-input`)
            .findAll('input')
            .first()
        )
        const endInput = new DatePicker(
          this.#termBreakRows
            .nth(i)
            .findByDataQa(`term-break-input`)
            .findAll('input')
            .last()
        )
        await startInput.fill(termBreak.start.format())
        await endInput.fill(termBreak.end.format())
      }
    }
  }

  #clubTermInputs = {
    termStart: new DatePicker(
      this.page.findByDataQa('term').findAll('input').first()
    ),
    termEnd: new DatePicker(
      this.page.findByDataQa('term').findAll('input').last()
    ),
    applicationPeriodStart: new DatePicker(
      this.page.findByDataQa('input-application-period-start')
    )
  }

  async editClubTerm(nth: number) {
    return this.#clubTermRows.nth(nth).findByDataQa('btn-edit').click()
  }

  async deleteClubTerm(nth: number) {
    return await this.#clubTermRows.nth(nth).findByDataQa('btn-delete').click()
  }

  async confirmClubTermModal() {
    await this.page.findByDataQa('modal').findByDataQa('modal-okBtn').click()
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
