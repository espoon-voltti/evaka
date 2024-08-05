// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { formatCents } from 'lib-common/money'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import {
  Checkbox,
  Combobox,
  DatePicker,
  DatePickerDeprecated,
  Element,
  FileInput,
  Modal,
  Page,
  Select,
  TextInput
} from '../../utils/page'

import { IncomeStatementPage } from './IncomeStatementPage'
import { TimelinePage } from './timeline/timeline-page'

export default class GuardianInformationPage {
  #restrictedDetailsEnabledLabel: Element
  #personStreetAddress: Element
  #timelineButton: Element
  constructor(private readonly page: Page) {
    this.#restrictedDetailsEnabledLabel = page.findByDataQa(
      'restriction-details-enabled-label'
    )
    this.#personStreetAddress = page.findByDataQa(
      'person-details-street-address'
    )
    this.#timelineButton = page.findByDataQa('timeline-button')
  }

  async navigateToGuardian(personId: UUID) {
    await this.page.goto(config.employeeUrl + '/profile/' + personId)
    await this.waitUntilLoaded()
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="person-info-section"][data-isloading="false"]')
      .waitUntilVisible()
    await this.page
      .find('[data-qa="family-overview-section"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async assertRestrictedDetails(enabled: boolean) {
    switch (enabled) {
      case true:
        await this.#restrictedDetailsEnabledLabel.waitUntilVisible()
        await this.#personStreetAddress.assertTextEquals(
          'Osoite ei ole saatavilla turvakiellon vuoksi'
        )
        break
      default:
        await this.#restrictedDetailsEnabledLabel.waitUntilHidden()
        await this.#personStreetAddress.assertText(
          (text) => text !== 'Osoite ei ole saatavilla turvakiellon vuoksi'
        )
    }
  }

  async openCollapsible<C extends Collapsible>(
    collapsible: C
  ): Promise<SectionFor<C>> {
    const { selector } = collapsibles[collapsible]
    const element = this.page.find(selector)
    if ((await element.getAttribute('data-status')) === 'closed') {
      await element.click()
    }
    return this.getCollapsible(collapsible)
  }

  getCollapsible<C extends Collapsible>(collapsible: C): SectionFor<C> {
    const { selector, section } = collapsibles[collapsible]
    const element = this.page.find(selector)
    return new section(this.page, element) as SectionFor<C>
  }
  async openTimeline(): Promise<TimelinePage> {
    await this.#timelineButton.click()
    return new TimelinePage(this.page)
  }
}

class Section extends Element {
  constructor(
    protected page: Page,
    root: Element
  ) {
    super(root)
  }
}

class PersonInfoSection extends Section {
  #lastName = this.find('[data-qa="person-last-name"]')
  #firstName = this.find('[data-qa="person-first-names"]')
  #ssn = this.find('[data-qa="person-ssn"]')

  async assertPersonInfo(lastName: string, firstName: string, ssn: string) {
    await this.#lastName.findText(lastName).waitUntilVisible()
    await this.#firstName.findText(firstName).waitUntilVisible()
    await this.#ssn.findText(ssn).waitUntilVisible()
  }
}

class FamilyOverviewSection extends Section {
  async assertPerson({
    personId,
    age,
    incomeCents
  }: {
    personId: string
    age?: number
    incomeCents?: number
  }) {
    const person = this.find(
      `[data-qa="table-family-overview-row-${personId}"]`
    )
    await person.waitUntilVisible()

    if (age !== undefined) {
      const personAge = person.find('[data-qa="person-age"]')
      await personAge.assertTextEquals(age.toString())
    }

    if (incomeCents !== undefined) {
      const personIncome = person.find('[data-qa="person-income-total"]')
      const expectedIncome = formatCents(incomeCents)
      await waitUntilEqual(
        async () => ((await personIncome.text) ?? '').split(' ')[0],
        expectedIncome
      )
    }
  }
}

class PartnersSection extends Section {
  #addPartnerButton = this.find('[data-qa="add-partner-button"]')

  async addPartner(partnerName: string, startDate: string) {
    await this.#addPartnerButton.click()
    const modal = new Modal(this.page.findByDataQa('fridge-partner-modal'))

    const combobox = new Combobox(
      modal.find('[data-qa="fridge-partner-person-search"]')
    )
    await combobox.fillAndSelectFirst(partnerName)

    const startDatePicker = new DatePickerDeprecated(
      modal.find('[data-qa="fridge-partner-start-date"]')
    )
    await startDatePicker.fill(startDate)

    await modal.submit()
  }
}

class ChildrenSection extends Section {
  #addChildButton = this.find('[data-qa="add-child-button"]')

  async addChild(childName: string, startDate: string) {
    await this.#addChildButton.click()
    const modal = new Modal(this.page.findByDataQa('fridge-child-modal'))

    const combobox = new Combobox(
      modal.find('[data-qa="fridge-child-person-search"]')
    )
    await combobox.fillAndSelectFirst(childName)

    const startDatePicker = new DatePickerDeprecated(
      modal.find('[data-qa="fridge-child-start-date"]')
    )
    await startDatePicker.fill(startDate)

    await modal.submit()
  }

  #childrenTableRow = this.findAll('[data-qa="table-fridge-child-row"]')

  async verifyChildAge(age: number) {
    const childAge = this.#childrenTableRow.nth(0).find('[data-qa="child-age"]')
    await childAge.assertTextEquals(age.toString())
  }
}

class DependantsSection extends Section {
  #childRow = (id: UUID) => this.page.findByDataQa(`table-dependant-row-${id}`)

  async assertContainsDependantChild(id: UUID) {
    await this.#childRow(id).waitUntilVisible()
  }

  async assertDoesNotContainDependantChild(id: UUID) {
    await this.#childRow(id).waitUntilHidden()
  }
}

class FosterChildrenSection extends Section {
  async addFosterChild(
    childName: string,
    startDate: LocalDate,
    endDate: LocalDate | null
  ) {
    await this.findByDataQa('add-foster-child-button').click()
    const modal = new Modal(this.page.findByDataQa('add-foster-child-modal'))
    await new Combobox(modal.findByDataQa('person-search')).fillAndSelectFirst(
      childName
    )
    await new DatePicker(modal.findByDataQa('start-date')).fill(
      startDate.format()
    )
    if (endDate !== null) {
      await new DatePicker(modal.findByDataQa('end-date')).fill(
        endDate?.format() ?? ''
      )
    }
    await modal.submit()
  }

  async editFosterChild(
    childId: string,
    startDate: LocalDate,
    endDate: LocalDate | null
  ) {
    await this.findByDataQa(`foster-child-row-${childId}`)
      .findByDataQa('edit')
      .click()
    const modal = new Modal(this.page.findByDataQa('edit-foster-child-modal'))
    await new DatePicker(modal.findByDataQa('start-date')).fill(
      startDate.format()
    )
    await new DatePicker(modal.findByDataQa('end-date')).fill(
      endDate?.format() ?? ''
    )
    await modal.submit()
    await modal.waitUntilHidden()
    await this.findByDataQa('spinner').waitUntilHidden()
  }

  async deleteFosterChild(childId: string) {
    await this.findByDataQa(`foster-child-row-${childId}`)
      .findByDataQa('delete')
      .click()
    const modal = new Modal(this.page.findByDataQa('delete-foster-child-modal'))
    await modal.submit()
    await modal.waitUntilHidden()
    await this.findByDataQa('spinner').waitUntilHidden()
  }

  async assertRowExists(
    childId: string,
    start: LocalDate,
    end: LocalDate | null
  ) {
    const row = this.findByDataQa(`foster-child-row-${childId}`)
    await row.findByDataQa('start').assertTextEquals(start.format())
    await row.findByDataQa('end').assertTextEquals(end?.format() ?? '')
  }

  async assertRowDoesNotExist(childId: string) {
    await this.findByDataQa(`foster-child-row-${childId}`).waitUntilHidden()
  }
}

class ApplicationsSection extends Section {
  #applicationRows = this.findAll('[data-qa="table-application-row"]')

  async assertApplicationCount(n: number) {
    await waitUntilEqual(() => this.#applicationRows.count(), n)
  }

  async assertApplicationSummary(
    n: number,
    childName: string,
    unitName: string
  ) {
    const row = this.#applicationRows.nth(n)
    await row.findText(childName).waitUntilVisible()
    await row.findText(unitName).waitUntilVisible()
  }
}

class DecisionsSection extends Section {
  #decisionRows = this.findAll('[data-qa="table-decision-row"]')

  async assertDecisionCount(n: number) {
    await waitUntilEqual(() => this.#decisionRows.count(), n)
  }

  async assertDecision(
    n: number,
    childName: string,
    unitName: string,
    status: string
  ) {
    const row = this.#decisionRows.nth(n)
    await row.findText(childName).waitUntilVisible()
    await row.findText(unitName).waitUntilVisible()
    await row.findText(status).waitUntilVisible()
  }
}

export class IncomeSection extends Section {
  #newIncomeButton: Element
  #incomeDateRange: Element
  #saveIncomeButton: Element
  #cancelIncomeButton: Element
  #toggleIncomeItemButton: Element
  #incomeSum: Element
  #expensesSum: Element
  #editIncomeItemButton: Element
  #incomeStartDateInput: DatePicker
  #incomeEndDateInput: DatePicker

  constructor(page: Page, root: Element) {
    super(page, root)
    this.#newIncomeButton = page.findByDataQa('add-income-button')
    this.#incomeDateRange = page.findByDataQa('income-date-range')
    this.#saveIncomeButton = page.findByDataQa('save-income')
    this.#cancelIncomeButton = page.findByDataQa('cancel-income-edit')
    this.#toggleIncomeItemButton = page.findByDataQa('toggle-income-item')
    this.#incomeSum = page.findByDataQa('income-sum-income')
    this.#expensesSum = page.findByDataQa('income-sum-expenses')
    this.#editIncomeItemButton = page.findByDataQa('edit-income-item')

    this.#incomeStartDateInput = new DatePicker(
      this.#incomeDateRange.findByDataQa('start-date')
    )
    this.#incomeEndDateInput = new DatePicker(
      this.#incomeDateRange.findByDataQa('end-date')
    )
  }

  // Income statements

  #incomeStatementRows = this.findAll(`[data-qa="income-statement-row"]`)
  #childIncomeStatementsTitles = this.findAll(
    '[data-qa="child-income-statement-title"]'
  )

  incomeNotificationRows = this.findAllByDataQa('income-notification-sent-date')
  incomeNotifications = this.findAllByDataQa('income-notifications')
  confirmRetroactive = new Checkbox(this.findByDataQa('confirm-retroactive'))

  async assertIncomeStatementChildName(nth: number, childName: string) {
    await this.#childIncomeStatementsTitles.nth(nth).assertTextEquals(childName)
  }

  async assertIncomeStatementRowCount(expected: number) {
    await this.#incomeStatementRows.assertCount(expected)
  }

  async isIncomeStatementHandled(nth = 0) {
    return new Checkbox(
      this.#incomeStatementRows.nth(nth).find(`[data-qa="is-handled-checkbox"]`)
    ).checked
  }

  async openIncomeStatement(nth = 0) {
    await this.#incomeStatementRows.nth(nth).find('a').click()
    return new IncomeStatementPage(this.page)
  }

  async getIncomeStatementInnerText(nth = 0) {
    return this.#incomeStatementRows.nth(nth).text
  }

  async openNewIncomeForm() {
    await this.#newIncomeButton.click()
  }

  async fillIncomeStartDate(value: string) {
    await this.#incomeStartDateInput.fill(value)
  }

  async fillIncomeEndDate(value: string) {
    await this.#incomeEndDateInput.fill(value)
  }

  #incomeInput = (type: string) =>
    new TextInput(this.page.findByDataQa(`income-input-${type}`))

  async fillIncome(type: string, value: string) {
    await this.#incomeInput(type).fill(value)
  }

  #incomeEffect = (effect: string) =>
    this.page.findByDataQa(`income-effect-${effect}`)

  async chooseIncomeEffect(effect: string) {
    await this.#incomeEffect(effect).click()
  }

  #coefficientSelect = (type: string) =>
    new Select(this.page.findByDataQa(`income-coefficient-select-${type}`))

  async chooseCoefficient(type: string, coefficient: string) {
    await this.#coefficientSelect(type).selectOption({ value: coefficient })
  }

  async save() {
    await this.#saveIncomeButton.click()
    await this.#saveIncomeButton.waitUntilHidden()
  }

  async saveFailing() {
    await this.#saveIncomeButton.click()
  }

  async saveIsDisabled() {
    return await this.#saveIncomeButton.disabled
  }

  async cancelEdit() {
    await this.#cancelIncomeButton.click()
  }

  incomeListItems = this.page.findAll('[data-qa="income-list-item"]')

  async incomeListItemCount() {
    return await this.incomeListItems.count()
  }

  async deleteIncomeItem(nth: number) {
    await this.incomeListItems
      .nth(nth)
      .find('[data-qa="delete-income-item"]')
      .click()
    await this.find('[data-qa="modal-okBtn"]').click()
  }

  async toggleIncome() {
    await this.#toggleIncomeItemButton.click()
  }

  async getIncomeSum() {
    return await this.#incomeSum.text
  }

  async getExpensesSum() {
    return await this.#expensesSum.text
  }

  async edit() {
    await this.#editIncomeItemButton.click()
  }

  #incomeFileUpload = this.find('[data-qa="income-attachment-upload"]')

  async uploadedCount() {
    return this.#incomeFileUpload
      .findAll('[data-qa="file-download-button"]')
      .count()
  }

  async addAttachment() {
    const testFileName = 'test_file.png'
    const initiallyUploadedCount = await this.uploadedCount()

    const testFilePath = `src/e2e-test/assets/${testFileName}`
    await new FileInput(
      this.#incomeFileUpload.find('[data-qa="btn-upload-file"]')
    ).setInputFiles(testFilePath)

    await waitUntilEqual(() => this.uploadedCount(), initiallyUploadedCount + 1)
  }

  async deleteIncomeAttachment(n: number) {
    return this.findAll('[data-qa^="file-delete-button"]').nth(n).click()
  }

  async getAttachmentCount() {
    return this.findAll('[data-qa="attachment"]').count()
  }
}

class FeeDecisionsSection extends Section {
  #feeDecisionTableRows = this.findAll('tbody tr')
  #feeDecisionSentAt = this.findAll(`[data-qa="fee-decision-sent-at"]`)

  async assertFeeDecision(
    n: number,
    {
      startDate,
      endDate,
      status
    }: {
      startDate: string
      endDate: string
      status: string
    }
  ) {
    const decision = this.#feeDecisionTableRows.nth(n)
    await waitUntilTrue(async () =>
      ((await decision.text) ?? '').includes(
        `Maksupäätös ${startDate} - ${endDate}`
      )
    )
    await waitUntilTrue(async () =>
      ((await decision.text) ?? '').includes(status)
    )
  }

  async createRetroactiveFeeDecisions(date: string) {
    await this.find(
      '[data-qa="create-retroactive-fee-decision-button"]'
    ).click()
    const modal = new Modal(this.page.findByDataQa('modal'))

    const startDate = new DatePicker(
      modal.find('[data-qa="retroactive-fee-decision-start-date"]')
    )
    await startDate.fill(date)

    await modal.submit()
  }

  async checkFeeDecisionSentAt(nth: number, expectedSentAt: LocalDate) {
    await this.#feeDecisionSentAt
      .nth(nth)
      .assertTextEquals(expectedSentAt.format('dd.MM.yyyy'))
  }
}

class VoucherValueDecisionsSection extends Section {
  #voucherValueDecisionSentAt = this.findAllByDataQa(
    'voucher-value-decision-sent-at'
  )

  #createRetroactiveDecisionsBtn = this.findByDataQa(
    'create-retroactive-value-decisions'
  )

  #voucherValueDecisions = this.findAllByDataQa(
    'table-voucher-value-decision-row'
  )

  async checkVoucherValueDecisionSentAt(
    nth: number,
    expectedSentAt: LocalDate
  ) {
    await this.#voucherValueDecisionSentAt
      .nth(nth)
      .assertTextEquals(expectedSentAt.format('dd.MM.yyyy'))
  }

  async checkVoucherValueDecisionCount(expectedCount: number) {
    await waitUntilEqual(
      () => this.#voucherValueDecisions.count(),
      expectedCount
    )
  }

  async createRetroactiveDecisions(from: LocalDate) {
    await this.#createRetroactiveDecisionsBtn.click()
    await new TextInput(
      this.findByDataQa('retroactive-value-decisions-from-date')
    ).type(from.format())
    await this.findByDataQa('modal-okBtn').click()
  }
}

class InvoicesSection extends Section {
  #invoiceRows = this.findAll('[data-qa="table-invoice-row"]')

  async assertInvoiceCount(n: number) {
    await waitUntilEqual(() => this.#invoiceRows.count(), n)
  }

  async assertInvoice(
    n: number,
    startDate: string,
    endDate: string,
    status: string
  ) {
    const row = this.#invoiceRows.nth(n)
    await row.findText(startDate).waitUntilVisible()
    await row.findText(endDate).waitUntilVisible()
    await row.findText(status).waitUntilVisible()
  }
}

class InvoiceCorrectionsSection extends Section {
  invoiceCorrectionRows = this.findAll(
    '[data-qa="invoice-details-invoice-row"]'
  )
  createInvoiceCorrectionButton = this.findByDataQa('create-invoice-correction')
  saveButton = this.findByDataQa('save-invoice-correction')

  lastRow() {
    return new InvoiceCorrectionRow(this, this.invoiceCorrectionRows.last())
  }

  async addNewInvoiceCorrection() {
    await this.createInvoiceCorrectionButton.click()
    return new InvoiceCorrectionRowEdit(this, this.invoiceCorrectionRows.last())
  }
}

class InvoiceCorrectionRow extends Element {
  constructor(
    public parent: Element,
    self: Element
  ) {
    super(self)
  }

  productSelect = this.findByDataQa('product')
  unitSelect = this.findByDataQa('unit')
  description = this.findByDataQa('description')
  period = this.findByDataQa('period')
  amount = this.findByDataQa('amount')
  unitPrice = this.findByDataQa('unit-price')
  totalPrice = this.findByDataQa('total-price')
  status = this.findByDataQa('status')
  noteIcon = this.findByDataQa('note-icon')
  noteTooltip = this.findByDataQa('note-tooltip')
  deleteButton = this.findByDataQa('delete-invoice-row-button')
}

class InvoiceCorrectionRowEdit extends Element {
  constructor(
    public parent: Element,
    self: Element
  ) {
    super(self)
  }

  productSelect = new Select(this.findByDataQa('select-product'))
  unitSelect = new Combobox(this.findByDataQa('input-unit'))
  description = new TextInput(this.findByDataQa('input-description'))
  startDate = new DatePickerDeprecated(
    this.findByDataQa('date-range-input-start-date')
  )
  endDate = new DatePickerDeprecated(
    this.findByDataQa('date-range-input-end-date')
  )
  amount = new TextInput(this.findByDataQa('input-amount'))
  price = new TextInput(this.findByDataQa('input-price'))
  totalPrice = this.findByDataQa('total-price')

  async clickAndAssertUnitVisibility(
    expectedUnitName: string,
    visible: boolean
  ) {
    await this.unitSelect.click()
    if (visible) {
      await this.unitSelect.findText(expectedUnitName).waitUntilVisible()
    } else {
      await this.unitSelect.findText(expectedUnitName).waitUntilHidden()
    }
  }

  async addNote() {
    await this.findByDataQa('add-note').click()
    return new NoteModal(this.parent.findByDataQa('modal'))
  }
}

class NoteModal extends Modal {
  note = new TextInput(this.findByDataQa('note-textarea'))
}

const collapsibles = {
  personInfo: {
    selector: '[data-qa="person-info-collapsible"]',
    section: PersonInfoSection
  },
  familyOverview: {
    selector: '[data-qa="family-overview-collapsible"]',
    section: FamilyOverviewSection
  },
  partners: {
    selector: '[data-qa="person-partners-collapsible"]',
    section: PartnersSection
  },
  children: {
    selector: '[data-qa="person-children-collapsible"]',
    section: ChildrenSection
  },
  dependants: {
    selector: '[data-qa="person-dependants-collapsible"]',
    section: DependantsSection
  },
  fosterChildren: {
    selector: '[data-qa="person-foster-children-collapsible"]',
    section: FosterChildrenSection
  },
  applications: {
    selector: '[data-qa="person-applications-collapsible"]',
    section: ApplicationsSection
  },
  decisions: {
    selector: '[data-qa="person-decisions-collapsible"]',
    section: DecisionsSection
  },
  incomes: {
    selector: '[data-qa="person-income-collapsible"]',
    section: IncomeSection
  },
  feeDecisions: {
    selector: '[data-qa="person-fee-decisions-collapsible"]',
    section: FeeDecisionsSection
  },
  voucherValueDecisions: {
    selector: '[data-qa="person-voucher-value-decisions-collapsible"]',
    section: VoucherValueDecisionsSection
  },
  invoices: {
    selector: '[data-qa="person-invoices-collapsible"]',
    section: InvoicesSection
  },
  invoiceCorrections: {
    selector: '[data-qa="person-invoice-corrections-collapsible"]',
    section: InvoiceCorrectionsSection
  }
}

type Collapsibles = typeof collapsibles
type Collapsible = keyof Collapsibles
type SectionFor<C extends Collapsible> = InstanceType<
  Collapsibles[C]['section']
>
