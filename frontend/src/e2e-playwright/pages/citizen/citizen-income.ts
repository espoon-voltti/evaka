// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

export default class CitizenIncomePage {
  constructor(private readonly page: Page) {}

  rows = this.page.locator('tbody tr')
  requiredAttachments = this.page.locator('[data-qa="required-attachments"]')

  async createNewIncomeStatement() {
    await this.page.locator('[data-qa="new-income-statement-btn"]').click()
  }

  async selectIncomeStatementType(
    type: 'highest-fee' | 'gross-income' | 'entrepreneur-income'
  ) {
    await this.page.locator(`[data-qa="${type}-checkbox"]`).click()
  }

  #startDate = this.page.locator('#start-date')
  async setValidFromDate(date: string) {
    await this.#startDate.selectText()
    await this.#startDate.type(date)
  }

  async submit() {
    await this.page.locator('button.primary').click()
  }

  async checkAssured() {
    await this.page.locator('[data-qa="assure-checkbox"]').click()
  }

  async checkIncomesRegisterConsent() {
    await this.page
      .locator('[data-qa="incomes-register-consent-checkbox"]')
      .click()
  }

  async selectEntrepreneurType(value: 'full-time' | 'part-time') {
    await this.page.locator(`[data-qa="entrepreneur-${value}-option"]`).click()
  }

  #entrepreneurDate = this.page.locator('[data-qa="entrepreneur-start-date"]')
  async setEntrepreneurStartDate(date: string) {
    await this.#entrepreneurDate.selectText()
    await this.#entrepreneurDate.type(date)
  }

  async selectEntrepreneurSpouse(yesNo: 'yes' | 'no') {
    await this.page.locator(`[data-qa="entrepreneur-spouse-${yesNo}"]`).click()
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
    const locator = this.page.locator(`[data-qa="${checkbox}-input"]`)
    check ? await locator.check() : await locator.uncheck()
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
    await this.page.locator(`[data-qa="llc-${value}"]`).click()
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
    await this.page.locator('[data-qa="accountant-name"]').type('Kirjanpitäjä')
    await this.page
      .locator('[data-qa="accountant-email"]')
      .type('foo@example.com')
    await this.page.locator('[data-qa="accountant-phone"]').type('0400123456')
  }
}
