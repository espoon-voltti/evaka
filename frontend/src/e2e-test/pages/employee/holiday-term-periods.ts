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
  #periodInputs: {
    start: DatePicker
    end: DatePicker
    reservationsOpenOn: DatePicker
    reservationDeadline: DatePicker
  }
  #questionnaireInputs: {
    activeStart: DatePicker
    activeEnd: DatePicker
    title: TextInput
    description: TextInput
    descriptionSv: TextInput
    descriptionEn: TextInput
    descriptionLink: TextInput
    descriptionLinkSv: TextInput
    descriptionLinkEn: TextInput
    fixedPeriodOptions: TextInput
    fixedPeriodOptionLabel: TextInput
  }
  #preschoolTermInputs: {
    finnishPreschoolStart: DatePicker
    finnishPreschoolEnd: DatePicker
    extendedTermStart: DatePicker
    applicationPeriodStart: DatePicker
  }
  #clubTermInputs: {
    termStart: DatePicker
    termEnd: DatePicker
    applicationPeriodStart: DatePicker
  }
  constructor(private readonly page: Page) {
    this.#periodRows = page.findAllByDataQa('holiday-period-row')
    this.#questionnaireRows = page.findAllByDataQa('questionnaire-row')
    this.#preschoolTermRows = page.findAllByDataQa('preschool-term-row')
    this.#clubTermRows = page.findAllByDataQa('club-term-row')
    this.confirmCheckbox = new Checkbox(page.findByDataQa('confirm-checkbox'))
    this.#addTermBreakButton = page.findByDataQa('add-term-break-button')
    this.#termBreakRows = page.findAllByDataQa('term-break')
    this.#periodInputs = {
      start: new DatePicker(
        page.findByDataQa('period').findAll('input').first()
      ),
      end: new DatePicker(page.findByDataQa('period').findAll('input').last()),
      reservationsOpenOn: new DatePicker(
        this.page.findByDataQa('input-reservations-open-on')
      ),
      reservationDeadline: new DatePicker(
        page.findByDataQa('input-reservation-deadline')
      )
    }
    this.#questionnaireInputs = {
      activeStart: new DatePicker(page.findByDataQa('input-start')),
      activeEnd: new DatePicker(page.findByDataQa('input-end')),
      title: new TextInput(page.findByDataQa('input-title-fi')),
      description: new TextInput(page.findByDataQa('input-description-fi')),
      descriptionSv: new TextInput(page.findByDataQa('input-description-sv')),
      descriptionEn: new TextInput(page.findByDataQa('input-description-en')),
      descriptionLink: new TextInput(
        page.findByDataQa('input-description-link-fi')
      ),
      descriptionLinkSv: new TextInput(
        page.findByDataQa('input-description-link-sv')
      ),
      descriptionLinkEn: new TextInput(
        page.findByDataQa('input-description-link-en')
      ),
      fixedPeriodOptions: new TextInput(
        page.findByDataQa('input-fixed-period-options')
      ),
      fixedPeriodOptionLabel: new TextInput(
        page.findByDataQa('input-fixed-period-option-label-fi')
      )
    }
    this.#preschoolTermInputs = {
      finnishPreschoolStart: new DatePicker(
        page.findByDataQa('finnish-preschool').findAll('input').first()
      ),
      finnishPreschoolEnd: new DatePicker(
        page.findByDataQa('finnish-preschool').findAll('input').last()
      ),
      extendedTermStart: new DatePicker(
        page.findByDataQa('input-extended-term-start')
      ),
      applicationPeriodStart: new DatePicker(
        page.findByDataQa('input-application-period-start')
      )
    }
    this.#clubTermInputs = {
      termStart: new DatePicker(
        page.findByDataQa('term').findAll('input').first()
      ),
      termEnd: new DatePicker(
        page.findByDataQa('term').findAll('input').last()
      ),
      applicationPeriodStart: new DatePicker(
        page.findByDataQa('input-application-period-start')
      )
    }
  }

  get visiblePeriods(): Promise<string[]> {
    return this.page.findAllByDataQa('holiday-period').allTexts()
  }

  get visibleHolidayPeriodDeadlines(): Promise<string[]> {
    return this.page.findAllByDataQa('holiday-period-deadline').allTexts()
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

  async fillHolidayPeriodForm(params: {
    start?: string
    end?: string
    reservationsOpenOn?: string
    reservationDeadline?: string
  }) {
    for (const [key, val] of Object.entries(params)) {
      if (val !== undefined) {
        await this.#periodInputs[key as keyof typeof params].fill(val)
      }
    }
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
