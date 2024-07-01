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
  ElementCollection
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
  constructor(private readonly page: Page) {
    this.startDateInput = new TextInput(page.findByDataQa('start-date'))
    this.otherInfoInput = new TextInput(page.findByDataQa('other-info'))
    this.assure = new Checkbox(page.findByDataQa('assure-checkbox'))
    this.saveButton = page.findByDataQa('save-btn')
  }

  async waitUntilReady() {
    await this.startDateInput.waitUntilVisible()
  }

  async setValidFromDate(date: string) {
    await this.startDateInput.fill(date)
    await this.startDateInput.press('Enter')
  }

  async typeOtherInfo(text: string) {
    await this.otherInfoInput.type(text)
  }

  async selectAssure() {
    await this.assure.check()
  }

  async save() {
    await this.saveButton.click()
  }

  async uploadAttachment(filePath: string) {
    await new FileInput(
      this.page.findByDataQa('btn-upload-file')
    ).setInputFiles(filePath)
  }
}

export class CitizenChildIncomeStatementListPage {
  private childIncomeStatementList: Element
  private childIncomeStatementRow: ElementCollection
  constructor(
    private readonly page: Page,
    private readonly nth: number
  ) {
    this.childIncomeStatementList = page
      .findAll(`[data-qa="child-income-statement"]`)
      .nth(this.nth)
    this.childIncomeStatementRow =
      this.childIncomeStatementList.findAll('tbody>tr')
  }

  async createIncomeStatement(): Promise<CitizenChildIncomeStatementEditPage> {
    await this.childIncomeStatementList
      .find('[data-qa="new-child-income-statement-btn"]')
      .click()
    const isPage = new CitizenChildIncomeStatementEditPage(this.page)
    await isPage.waitUntilReady()
    return isPage
  }

  async assertChildName(expectedName: string) {
    await this.childIncomeStatementList
      .find('[data-qa="child-name"]')
      .assertTextEquals(expectedName)
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
