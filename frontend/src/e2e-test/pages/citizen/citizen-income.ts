// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Checkbox,
  Page,
  Radio,
  TextInput,
  Element,
  ElementCollection
} from '../../utils/page'

export default class CitizenIncomePage {
  requiredAttachments: Element
  rows: ElementCollection
  assureCheckBox: Checkbox
  #entrepreneurDate: TextInput
  validFromDate: TextInput
  validToDate: TextInput
  incomeStartDateInfo: Element
  incomeEndDateInfo: Element
  incomeValidMaxRangeInfo: Element
  constructor(private readonly page: Page) {
    this.requiredAttachments = page.findByDataQa('required-attachments')
    this.rows = page.findAll('tbody tr')
    this.assureCheckBox = new Checkbox(page.findByDataQa('assure-checkbox'))
    this.#entrepreneurDate = new TextInput(
      page.findByDataQa('entrepreneur-start-date')
    )
    this.validFromDate = new TextInput(page.findByDataQa('income-start-date'))
    this.validToDate = new TextInput(page.findByDataQa('income-end-date'))
    this.incomeStartDateInfo = page.findByDataQa('income-start-date-info')
    this.incomeEndDateInfo = page.findByDataQa('income-end-date-info')
    this.incomeValidMaxRangeInfo = page.findByDataQa('date-range-info')
  }

  async createNewIncomeStatement() {
    await this.page.findByDataQa('new-income-statement-btn').click()
  }

  async selectIncomeStatementType(
    type: 'highest-fee' | 'gross-income' | 'entrepreneur-income'
  ) {
    await new Checkbox(this.page.findByDataQa(`highest-fee-checkbox`)).uncheck()
    await new Checkbox(this.page.findByDataQa(`${type}-checkbox`)).check()
  }

  async setValidFromDate(date: string) {
    await this.validFromDate.fill(date)
    await this.page.findByDataQa('title').click()
  }

  async setValidToDate(date: string) {
    await this.validToDate.fill(date)
    await this.page.findByDataQa('title').click()
  }

  async submit() {
    await this.page.find('button.primary').click()
  }

  async checkAssured() {
    await this.assureCheckBox.check()
  }

  async checkIncomesRegisterConsent() {
    await this.page
      .find('[data-qa="incomes-register-consent-checkbox"]')
      .click()
  }

  async selectEntrepreneurType(value: 'full-time' | 'part-time') {
    await this.page.findByDataQa(`entrepreneur-${value}-option`).click()
  }

  async setEntrepreneurStartDate(date: string) {
    await this.#entrepreneurDate.fill(date)
    await this.#entrepreneurDate.press('Enter')
  }

  async selectEntrepreneurSpouse(yesNo: 'yes' | 'no') {
    await new Radio(
      this.page.findByDataQa(`entrepreneur-spouse-${yesNo}`)
    ).check()
  }

  private async toggleCheckbox(
    checkbox:
      | 'student'
      | 'alimony-payer'
      | 'entrepreneur-startup-grant'
      | 'entrepreneur-checkup-consent'
      | 'entrepreneur-llc'
      | 'entrepreneur-light-entrepreneur'
      | 'entrepreneur-self-employed'
      | 'entrepreneur-partnership'
      | 'self-employed-attachments'
      | 'self-employed-estimated-income',
    check: boolean
  ) {
    const elem = new Checkbox(this.page.findByDataQa(`${checkbox}`))
    if (check) {
      await elem.check()
    } else {
      await elem.uncheck()
    }
  }

  async toggleEntrepreneurStartupGrant(check: boolean) {
    await this.toggleCheckbox('entrepreneur-startup-grant', check)
  }

  async toggleEntrepreneurCheckupConsent(check: boolean) {
    await this.toggleCheckbox('entrepreneur-checkup-consent', check)
  }

  async toggleLimitedLiabilityCompany(check: boolean) {
    await this.toggleCheckbox('entrepreneur-llc', check)
  }

  async toggleLlcType(value: 'attachments' | 'incomes-register') {
    await this.page.findByDataQa(`llc-${value}`).click()
  }

  async toggleLightEntrepreneur(check: boolean) {
    await this.toggleCheckbox('entrepreneur-light-entrepreneur', check)
  }

  async toggleSelfEmployed(check: boolean) {
    await this.toggleCheckbox('entrepreneur-self-employed', check)
  }

  async togglePartnership(check: boolean) {
    await this.toggleCheckbox('entrepreneur-partnership', check)
  }

  async toggleSelfEmployedEstimatedIncome(check: boolean) {
    await this.toggleCheckbox('self-employed-estimated-income', check)
  }

  async toggleSelfEmployedAttachments(check: boolean) {
    await this.toggleCheckbox('self-employed-attachments', check)
  }

  async toggleStudent(check: boolean) {
    await this.toggleCheckbox('student', check)
  }

  async toggleAlimonyPayer(check: boolean) {
    await this.toggleCheckbox('alimony-payer', check)
  }

  async fillAccountant() {
    await new TextInput(this.page.findByDataQa('accountant-name')).fill(
      'Kirjanpitäjä'
    )
    await new TextInput(this.page.findByDataQa('accountant-email')).fill(
      'foo@example.com'
    )
    await new TextInput(this.page.findByDataQa('accountant-phone')).fill(
      '0400123456'
    )
  }

  async setGrossIncomeEstimate(income: number) {
    await new TextInput(
      this.page.findByDataQa('gross-monthly-income-estimate')
    ).fill(String(income))
  }
}
