// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import {
  Checkbox,
  FileInput,
  Page,
  TextInput,
  Element,
  ElementCollection,
  EnvType
} from '../../utils/page'

export class CitizenChildIncomeStatementViewPage {
  startDate: Element
  otherInfo: Element
  constructor(private readonly page: Page) {
    this.startDate = page.findByDataQa('start-date')
    this.otherInfo = page.findByDataQa('other-info')
  }

  async waitUntilReady() {
    await this.startDate.waitUntilVisible()
  }

  async assertOtherInfo(expected: string) {
    await this.otherInfo.assertTextEquals(expected)
  }

  async assertAttachmentExists(name: string) {
    await waitUntilTrue(async () =>
      (
        await this.page.findAllByDataQa('attachment-download-button').allTexts()
      ).includes(name)
    )
  }

  async clickEdit() {
    await this.page.findByDataQa('edit-button').click()
  }

  async clickGoBack() {
    await this.page.findText('Palaa').click()
  }
}

export class CitizenChildIncomeStatementEditPage {
  startDateInput: TextInput
  otherInfoInput: TextInput
  assure: Checkbox
  saveButton: Element
  saveDraftButton: Element
  constructor(private readonly page: Page) {
    this.startDateInput = new TextInput(page.findByDataQa('start-date'))
    this.otherInfoInput = new TextInput(page.findByDataQa('other-info'))
    this.assure = new Checkbox(page.findByDataQa('assure-checkbox'))
    this.saveButton = page.findByDataQa('save-btn')
    this.saveDraftButton = page.findByDataQa('save-draft-btn')
  }

  async waitUntilReady() {
    await this.otherInfoInput.waitUntilVisible()
  }

  async setValidFromDate(date: string) {
    await this.startDateInput.fill(date)
    await this.startDateInput.press('Enter')
  }

  async fillOtherInfo(text: string) {
    await this.otherInfoInput.fill(text)
  }

  async selectAssure() {
    await this.assure.check()
  }

  async saveDraft() {
    await this.saveDraftButton.click()
  }

  async save() {
    await this.saveButton.click()
  }

  async uploadAttachment(filePath: string, fileName: string) {
    await new FileInput(
      this.page.findByDataQa('btn-upload-file')
    ).setInputFiles(filePath)
    await this.page
      .findByDataQa(`file-delete-button-${fileName}`)
      .waitUntilVisible()
  }
}

export class CitizenChildIncomeStatementListPage {
  private childIncomeStatementsSection: Element
  private childIncomeStatementsList: Element
  private childIncomeStatementRows: ElementCollection
  constructor(
    private readonly page: Page,
    private readonly nth: number,
    env: EnvType
  ) {
    this.childIncomeStatementsSection = page
      .findAllByDataQa('child-income-statements')
      .nth(this.nth)
    this.childIncomeStatementsList =
      this.childIncomeStatementsSection.findByDataQa(
        env === 'desktop'
          ? 'child-income-statement-table'
          : 'child-income-statement-list'
      )
    this.childIncomeStatementRows =
      this.childIncomeStatementsList.findAllByDataQa(
        'child-income-statement-row'
      )
  }

  async createIncomeStatement(): Promise<CitizenChildIncomeStatementEditPage> {
    await this.childIncomeStatementsSection
      .find('[data-qa="new-child-income-statement-btn"]')
      .click()
    const isPage = new CitizenChildIncomeStatementEditPage(this.page)
    await isPage.waitUntilReady()
    return isPage
  }

  async assertChildName(expectedName: string) {
    await this.childIncomeStatementsSection
      .find('[data-qa="child-name"]')
      .assertTextEquals(expectedName)
  }

  async assertIncomeStatementMissingWarningIsShown() {
    await waitUntilTrue(
      () =>
        this.childIncomeStatementsSection.find(
          '[data-qa="child-income-statement-missing-warning"]'
        ).visible
    )
  }

  async assertChildCount(expected: number) {
    await this.page
      .find('[data-qa="children-income-statements"]')
      .waitUntilVisible()
    await waitUntilEqual(
      () => this.page.findAll(`[data-qa="child-income-statements"]`).count(),
      expected
    )
  }

  async assertChildIncomeStatementRowCount(expected: number) {
    await this.childIncomeStatementsList.waitUntilVisible()
    await waitUntilEqual(() => this.childIncomeStatementRows.count(), expected)
  }

  async clickEditChildIncomeStatement(nth: number) {
    await this.childIncomeStatementRows
      .nth(nth)
      .findByDataQa('edit-income-statement')
      .click()
    const isPage = new CitizenChildIncomeStatementEditPage(this.page)
    await isPage.waitUntilReady()
    return isPage
  }

  async deleteChildIncomeStatement(nth: number) {
    await this.childIncomeStatementRows
      .nth(nth)
      .findByDataQa('delete-income-statement')
      .click()
    await this.page.findAll('[data-qa="modal-okBtn"]').nth(0).click()
  }

  async clickViewChildIncomeStatement(nth: number) {
    await this.childIncomeStatementRows
      .nth(nth)
      .findByDataQa('view-income-statement')
      .click()
    const isPage = new CitizenChildIncomeStatementViewPage(this.page)
    await isPage.waitUntilReady()
    return isPage
  }
}
