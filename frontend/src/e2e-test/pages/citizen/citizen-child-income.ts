// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { FileInput, Page } from '../../utils/page'

export class CitizenChildIncomeStatementViewPage {
  constructor(private readonly page: Page) {}

  private startDate = this.page.find('[data-qa="start-date"]')

  async waitUntilReady() {
    await this.startDate.waitUntilVisible()
  }

  async assertAttachmentExists(name: string) {
    await waitUntilTrue(async () =>
      (
        await this.page.find('[data-qa="attachment-download-button"]').innerText
      ).includes(name)
    )
  }

  async clickEdit() {
    await this.page.find('[data-qa="edit-button"]').click()
  }

  async clickGoBack() {
    await this.page.findText('Palaa').click()
  }
}

export class CitizenChildIncomeStatementEditPage {
  constructor(private readonly page: Page) {}

  private startDateInput = this.page.find('[data-qa="start-date"]')
  private noIncomeSelect = this.page.find('[data-qa="child-income-no-income"]')
  private hasIncomeSelect = this.page.find(
    '[data-qa="child-income-has-income"]'
  )
  private assure = this.page.find('[data-qa="assure-checkbox"]')
  private saveButton = this.page.find('[data-qa="save-btn"]')

  async waitUntilReady() {
    await this.startDateInput.waitUntilVisible()
  }

  async selectNoIncome() {
    await this.noIncomeSelect.click()
  }

  async selectHasIncome() {
    await this.hasIncomeSelect.click()
  }

  async selectAssure() {
    await this.assure.click()
  }

  async save() {
    await this.saveButton.click()
  }

  async uploadAttachment(filePath: string) {
    await new FileInput(
      this.page.find('[data-qa="btn-upload-file"]')
    ).setInputFiles(filePath)
  }
}

export class CitizenChildIncomeStatementListPage {
  constructor(private readonly page: Page, private readonly nth: number) {}

  private childIncomeStatementList = this.page
    .findAll(`[data-qa="child-income-statement"]`)
    .nth(this.nth)

  private childIncomeStatementRow =
    this.childIncomeStatementList.findAll('tbody>tr')

  async createIncomeStatement(): Promise<CitizenChildIncomeStatementEditPage> {
    await this.childIncomeStatementList
      .find('[data-qa="new-child-income-statement-btn"]')
      .click()
    const isPage = new CitizenChildIncomeStatementEditPage(this.page)
    await isPage.waitUntilReady()
    return isPage
  }

  async assertChildName(expectedName: string) {
    await waitUntilEqual(
      () =>
        this.childIncomeStatementList.find('[data-qa="child-name"]')
          .textContent,
      expectedName
    )
  }

  async assertIncomeStatementMissingWarningIsShown() {
    await waitUntilTrue(
      () =>
        this.childIncomeStatementList.find(
          '[data-qa="child-income-statement-missing-warning"]'
        ).visible
    )
  }

  async assertChildCount(expected: number) {
    await this.page
      .find('[data-qa="children-income-statements"]')
      .waitUntilVisible()
    await waitUntilEqual(
      () => this.page.findAll(`[data-qa="child-income-statement"]`).count(),
      expected
    )
  }

  async assertChildIncomeStatementRowCount(expected: number) {
    await this.childIncomeStatementList.find('table').waitUntilVisible()
    await waitUntilEqual(() => this.childIncomeStatementRow.count(), expected)
  }

  async clickEditChildIncomeStatement(nth: number) {
    await this.childIncomeStatementList
      .findAll('[data-qa="edit-income-statement"]')
      .nth(nth)
      .click()
    const isPage = new CitizenChildIncomeStatementEditPage(this.page)
    await isPage.waitUntilReady()
    return isPage
  }

  async deleteChildIncomeStatement(nth: number) {
    await this.childIncomeStatementList
      .findAll('[data-qa="delete-income-statement"]')
      .nth(nth)
      .click()
    await this.page.findAll('[data-qa="modal-okBtn"]').nth(0).click()
  }

  async clickViewChildIncomeStatement(nth: number) {
    await this.childIncomeStatementList
      .findAll('[data-qa="button-open-income-statement"]')
      .nth(nth)
      .click()
    const isPage = new CitizenChildIncomeStatementViewPage(this.page)
    await isPage.waitUntilReady()
    return isPage
  }
}
