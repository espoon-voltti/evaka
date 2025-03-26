// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IncomeStatementAttachmentType } from 'lib-common/generated/api-types/incomestatement'
import { IncomeStatementId } from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import {
  Checkbox,
  Page,
  TextInput,
  Element,
  ElementCollection,
  EnvType,
  FileUpload
} from '../../utils/page'

export class CitizenChildIncomeStatementViewPage {
  startDate: Element
  otherInfo: Element

  constructor(private readonly page: Page) {
    this.startDate = page.findByDataQa('start-date')
    this.otherInfo = page.findByDataQa('other-info')
  }

  async assertAttachmentExists(
    attachmentType: IncomeStatementAttachmentType,
    name: string
  ) {
    await this.page
      .findByDataQa(`attachments-${attachmentType}`)
      .findByDataQa(`file-${name}`)
      .waitUntilVisible()
  }
}

export class CitizenChildIncomeStatementEditSentPage {
  startDate: Element
  otherInfoInput: TextInput
  attachment: FileUpload
  saveButton: Element

  constructor(page: Page) {
    this.startDate = page.findByDataQa('start-date')
    this.otherInfoInput = new TextInput(page.findByDataQa('other-info'))
    this.attachment = new FileUpload(
      page.findByDataQa('attachment-section-CHILD_INCOME')
    )
    this.saveButton = page.findByDataQa('save-btn')
  }
}

export class CitizenChildIncomeStatementEditPage {
  startDateInput: TextInput
  otherInfoInput: TextInput
  attachments: FileUpload
  assure: Checkbox
  sendButton: Element
  saveDraftButton: Element

  constructor(readonly page: Page) {
    this.startDateInput = new TextInput(page.findByDataQa('start-date'))
    this.otherInfoInput = new TextInput(page.findByDataQa('other-info'))
    this.attachments = new FileUpload(
      page.findByDataQa('attachment-section-CHILD_INCOME')
    )
    this.assure = new Checkbox(page.findByDataQa('assure-checkbox'))
    this.sendButton = page.findByDataQa('save-btn')
    this.saveDraftButton = page.findByDataQa('save-draft-btn')
  }

  async setValidFromDate(date: string) {
    await this.startDateInput.fill(date)
    await this.startDateInput.press('Enter')
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
    return new CitizenChildIncomeStatementEditPage(this.page)
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

  async incomeStatementId(nth: number) {
    const href = await this.childIncomeStatementRows
      .nth(nth)
      .findByDataQa('view-income-statement')
      .getAttribute('href')
    return fromUuid<IncomeStatementId>(href!.split('/').at(-2)!)
  }

  async deleteChildIncomeStatement(nth: number) {
    await this.childIncomeStatementRows
      .nth(nth)
      .findByDataQa('delete-income-statement')
      .click()
    await this.page.findAll('[data-qa="modal-okBtn"]').nth(0).click()
  }

  async editChildDraftIncomeStatement(nth: number) {
    await this.childIncomeStatementRows
      .nth(nth)
      .findByDataQa('edit-income-statement-DRAFT')
      .click()
    return new CitizenChildIncomeStatementEditPage(this.page)
  }

  async editChildSentIncomeStatement(nth: number) {
    await this.childIncomeStatementRows
      .nth(nth)
      .findByDataQa('edit-income-statement-SENT')
      .click()
    return new CitizenChildIncomeStatementEditSentPage(this.page)
  }

  async viewChildHandledIncomeStatement(nth: number) {
    await this.childIncomeStatementRows
      .nth(nth)
      .findByDataQa('view-income-statement')
      .click()
    return new CitizenChildIncomeStatementViewPage(this.page)
  }
}
