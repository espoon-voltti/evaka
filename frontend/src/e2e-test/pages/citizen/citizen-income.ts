// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { IncomeStatementAttachmentType } from 'lib-common/generated/api-types/incomestatement'

import type {
  Page,
  Element,
  ElementCollection,
  EnvType
} from '../../utils/page'
import { Checkbox, Radio, TextInput, FileUpload } from '../../utils/page'

export default class CitizenIncomePage {
  missingAttachments: Element
  rows: ElementCollection
  assureCheckBox: Checkbox
  #entrepreneurDate: TextInput
  invalidForm: Element
  validFromDate: TextInput
  validToDate: TextInput
  incomeStartDateInfo: Element
  incomeEndDateInfo: Element
  constructor(
    private readonly page: Page,
    env: EnvType
  ) {
    this.missingAttachments = page.findByDataQa('missing-attachments')
    this.rows = page
      .findByDataQa(
        env === 'desktop' ? 'income-statements-table' : 'income-statements-list'
      )
      .findAllByDataQa('income-statement-row')
    this.assureCheckBox = new Checkbox(page.findByDataQa('assure-checkbox'))
    this.#entrepreneurDate = new TextInput(
      page.findByDataQa('entrepreneur-start-date')
    )
    this.invalidForm = page.findByDataQa('invalid-form')
    this.validFromDate = new TextInput(page.findByDataQa('income-start-date'))
    this.validToDate = new TextInput(page.findByDataQa('income-end-date'))
    this.incomeStartDateInfo = page.findByDataQa('income-start-date-info')
    this.incomeEndDateInfo = page.findByDataQa('income-end-date-info')
  }

  async createNewIncomeStatement() {
    await this.page.findByDataQa('new-income-statement-btn').click()
  }

  async editIncomeStatement(n: number) {
    await this.rows.nth(n).findByDataQa('edit-income-statement').click()
  }

  async selectIncomeStatementType(type: 'highest-fee' | 'gross-income') {
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

  #missingAttachment(attachmentType: IncomeStatementAttachmentType) {
    return this.missingAttachments.findByDataQa(`attachment-${attachmentType}`)
  }

  async assertMissingAttachment(attachmentType: IncomeStatementAttachmentType) {
    await this.#missingAttachment(attachmentType).waitUntilVisible()
  }

  async saveDraft() {
    await this.page.findByDataQa('save-draft-btn').click()
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
    await elem.evaluate((e) => e.scrollIntoView({ block: 'center' }))
    if (check) {
      await elem.check()
    } else {
      await elem.uncheck()
    }
  }

  attachmentInput(type: IncomeStatementAttachmentType) {
    return new FileUpload(this.page.findByDataQa(`attachment-section-${type}`))
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

  async setEntrepreneur(value: boolean) {
    const option = value ? 'yes' : 'no'
    await new Radio(this.page.findByDataQa(`entrepreneur-${option}`)).check()
  }
}
