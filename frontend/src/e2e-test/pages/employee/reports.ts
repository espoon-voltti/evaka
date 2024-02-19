// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationStatus } from 'lib-common/generated/api-types/application'
import LocalDate from 'lib-common/local-date'

import { captureTextualDownload } from '../../browser'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import {
  Checkbox,
  Combobox,
  DatePicker,
  DatePickerDeprecated,
  MultiSelect,
  Page,
  Select,
  StaticChip,
  TextInput
} from '../../utils/page'

export default class ReportsPage {
  constructor(private readonly page: Page) {}

  async openMissingHeadOfFamilyReport() {
    await this.page.find('[data-qa="report-missing-head-of-family"]').click()
    return new MissingHeadOfFamilyReport(this.page)
  }

  async openApplicationsReport() {
    await this.page.find('[data-qa="report-applications"]').click()
    return new ApplicationsReport(this.page)
  }

  async openNonSsnChildrenReport() {
    await this.page.find('[data-qa="report-non-ssn-children"]').click()
    return new NonSsnChildrenReport(this.page)
  }

  async openPlacementGuaranteeReport() {
    await this.page.find('[data-qa="report-placement-guarantee"]').click()
    return new PlacementGuaranteeReport(this.page)
  }

  async openPlacementSketchingReport() {
    await this.page.find('[data-qa="report-placement-sketching"]').click()
    return new PlacementSketchingReport(this.page)
  }

  async openVoucherServiceProvidersReport() {
    await this.page.find('[data-qa="report-voucher-service-providers"]').click()
    return new VoucherServiceProvidersReport(this.page)
  }

  async openManualDuplicationReport() {
    await this.page.find('[data-qa="report-manual-duplication"]').click()
    return new ManualDuplicationReport(this.page)
  }

  async openVardaErrorsReport() {
    await this.page.find('[data-qa="report-varda-errors"]').click()
    return new VardaErrorsReport(this.page)
  }
}

export class MissingHeadOfFamilyReport {
  constructor(private page: Page) {}

  async toggleShowIntentionalDuplicates() {
    await new Checkbox(
      this.page.findByDataQa('show-intentional-duplicates-checkbox')
    ).toggle()
  }

  async assertRows(
    expected: {
      childName: string
      rangesWithoutHead: string
    }[]
  ) {
    const rows = this.page.findAllByDataQa('missing-head-of-family-row')
    await rows.assertCount(expected.length)
    for (const [index, data] of expected.entries()) {
      const row = rows.nth(index)
      await row.findByDataQa('child-name').assertTextEquals(data.childName)
      await row
        .findByDataQa('ranges-without-head')
        .assertTextEquals(data.rangesWithoutHead)
    }
  }
}

export class NonSsnChildrenReport {
  constructor(private page: Page) {}

  #nameHeader = this.page.find(`[data-qa="child-name-header"]`)

  async changeSortOrder() {
    await this.#nameHeader.click()
  }

  async assertRows(
    expected: {
      childName: string
      dateOfBirth: string
      personOid: string
      vardaOid: string
    }[]
  ) {
    const rows = this.page.findAllByDataQa('non-ssn-child-row')
    await rows.assertCount(expected.length)
    for (const [index, data] of expected.entries()) {
      const row = rows.nth(index)
      await row.findByDataQa('child-name').assertTextEquals(data.childName)
      await row.findByDataQa('date-of-birth').assertTextEquals(data.dateOfBirth)
      await row.findByDataQa('person-oid').assertTextEquals(data.personOid)
      await row.findByDataQa('varda-oid').assertTextEquals(data.vardaOid)
    }
  }
}

export class ApplicationsReport {
  constructor(private page: Page) {}

  #table = this.page.find(`[data-qa="report-application-table"]`)
  #areaSelector = new Combobox(this.page.find('[data-qa="select-area"]'))

  private async areaWithNameExists(area: string, exists = true) {
    await this.#table.waitUntilVisible()

    const areaElem = this.#table
      .findAll(`[data-qa="care-area-name"]:has-text("${area}")`)
      .first()

    if (exists) {
      await areaElem.waitUntilVisible()
    } else {
      await areaElem.waitUntilHidden()
    }
  }

  async assertContainsArea(area: string) {
    await this.areaWithNameExists(area, true)
  }

  async assertDoesntContainArea(area: string) {
    await this.areaWithNameExists(area, false)
  }

  async assertContainsServiceProviders(providers: string[]) {
    await this.#table.waitUntilVisible()

    for (const provider of providers) {
      await this.#table
        .findAll(`[data-qa="unit-provider-type"]:has-text("${provider}")`)
        .first()
        .waitUntilVisible()
    }
  }

  async selectArea(area: string) {
    await this.#areaSelector.fillAndSelectFirst(area)
  }

  async selectDateRangePickerDates(from: LocalDate, to: LocalDate) {
    const fromInput = new DatePickerDeprecated(
      this.page.find('[data-qa="datepicker-from"]')
    )
    const toInput = new DatePickerDeprecated(
      this.page.find('[data-qa="datepicker-to"]')
    )
    await fromInput.fill(from.format())
    await toInput.fill(to.format())
  }
}

export class PlacementGuaranteeReport {
  constructor(private page: Page) {}

  async selectDate(formattedDate: string) {
    const datePicker = new DatePicker(this.page.findByDataQa('date-picker'))
    await datePicker.fill(formattedDate)
  }

  async selectUnit(unitName: string) {
    const unitSelector = new Combobox(this.page.findByDataQa('unit-selector'))
    await unitSelector.fillAndSelectFirst(unitName)
  }

  async assertRows(
    expected: {
      areaName: string
      unitName: string
      childName: string
      placementPeriod: string
    }[]
  ) {
    const rows = this.page.findAllByDataQa('placement-guarantee-row')
    await rows.assertCount(expected.length)
    await Promise.all(
      expected.map(async (data, index) => {
        const row = rows.nth(index)
        await row.findByDataQa('area-name').assertTextEquals(data.areaName)
        await row.findByDataQa('unit-name').assertTextEquals(data.unitName)
        await row.findByDataQa('child-name').assertTextEquals(data.childName)
        await row
          .findByDataQa('placement-period')
          .assertTextEquals(data.placementPeriod)
      })
    )
  }
}

export class PlacementSketchingReport {
  constructor(private page: Page) {}

  #applicationStatus = new MultiSelect(
    this.page.find('[data-qa="select-application-status"]')
  )

  async assertRow(
    applicationId: string,
    requestedUnitName: string,
    childName: string,
    currentUnitName: string | null = null
  ) {
    const element = this.page.find(`[data-qa="${applicationId}"]`)
    await element.waitUntilVisible()

    await element
      .find('[data-qa="requested-unit"]')
      .assertTextEquals(requestedUnitName)
    if (currentUnitName) {
      await element
        .find('[data-qa="current-unit"]')
        .assertTextEquals(currentUnitName)
    }
    await element.find('[data-qa="child-name"]').assertTextEquals(childName)
  }

  async assertNotRow(applicationId: string) {
    const element = this.page.find(`[data-qa="${applicationId}"]`)
    await element.waitUntilHidden()
  }

  async toggleApplicationStatus(status: ApplicationStatus) {
    await this.#applicationStatus.click() // open options menu
    await this.#applicationStatus.selectItem(status)
    await this.#applicationStatus.click() // close options menu
  }
}

export class VoucherServiceProvidersReport {
  constructor(private page: Page) {}

  #month = new Select(this.page.find('[data-qa="select-month"]'))
  #year = new Select(this.page.find('[data-qa="select-year"]'))
  #area = new Select(this.page.find('[data-qa="select-area"]'))

  #downloadCsvLink = this.page.find('[data-qa="download-csv"] a')

  async selectMonth(month: 'Tammikuu') {
    await this.#month.selectOption({ label: month.toLowerCase() })
  }

  async selectYear(year: number) {
    await this.#year.selectOption({ label: String(year) })
  }

  async selectArea(area: string) {
    await this.#area.selectOption({ label: area })
  }

  async assertRowCount(expectedChildCount: number) {
    await waitUntilEqual(
      () => this.page.findAll('.reportRow').count(),
      expectedChildCount
    )
  }

  async assertRow(
    unitId: string,
    expectedChildCount: string,
    expectedMonthlySum: string
  ) {
    const row = this.page.find(`[data-qa="${unitId}"]`)
    await row.waitUntilVisible()
    expect(await row.find(`[data-qa="child-count"]`).text).toStrictEqual(
      expectedChildCount
    )
    expect(await row.find(`[data-qa="child-sum"]`).text).toStrictEqual(
      expectedMonthlySum
    )
  }

  async getCsvReport(): Promise<string> {
    const [download] = await Promise.all([
      this.page.waitForDownload(),
      this.#downloadCsvLink.click()
    ])
    return captureTextualDownload(download)
  }

  async openUnitReport(unitName: string) {
    await this.page.findTextExact(unitName).click()
  }
}

export class ServiceVoucherUnitReport {
  constructor(private page: Page) {}

  #childRows = this.page.findAllByDataQa('child-row')

  async assertChildRowCount(expected: number) {
    await waitUntilEqual(() => this.#childRows.count(), expected)
  }

  async assertChild(
    nth: number,
    expectedName: string,
    expectedVoucherValue: number,
    expectedCoPayment: number,
    expectedRealizedAmount: number
  ) {
    await this.page
      .findAllByDataQa('child-name')
      .nth(nth)
      .assertTextEquals(expectedName)
    await this.page
      .findAllByDataQa('voucher-value')
      .nth(nth)
      .assertText((text) => parseFloat(text) === expectedVoucherValue)
    await this.page
      .findAllByDataQa('co-payment')
      .nth(nth)
      .assertText((text) => parseFloat(text) === expectedCoPayment)
    await this.page
      .findAllByDataQa('realized-amount')
      .nth(nth)
      .assertText((text) => parseFloat(text) === expectedRealizedAmount)
  }
}

export class ManualDuplicationReport {
  constructor(private page: Page) {}

  async assertRows(
    expected: {
      childName: string
      connectedUnitName: string
      serviceNeedOptionName: string
      preschoolUnitName: string
    }[]
  ) {
    const rows = this.page.findAllByDataQa('manual-duplication-row')
    await rows.assertCount(expected.length)
    await Promise.all(
      expected.map(async (data, index) => {
        const row = rows.nth(index)
        await row.findByDataQa('child-name').assertTextEquals(data.childName)
        await row
          .findByDataQa('connected-unit-name')
          .assertTextEquals(data.connectedUnitName)
        await row
          .findByDataQa('service-need-option-name')
          .assertTextEquals(data.serviceNeedOptionName)
        await row
          .findByDataQa('preschool-unit-name')
          .assertTextEquals(data.preschoolUnitName)
      })
    )
  }
}

export class VardaErrorsReport {
  constructor(private page: Page) {}

  #errorsTable = this.page.find('[data-qa="varda-errors-table"]')
  #errorRows = this.page.findAll('[data-qa="varda-error-row"]')
  #errors = (childId: string) => this.page.find(`[data-qa="errors-${childId}"]`)
  #resetChild = (childId: string) =>
    this.page.find(`[data-qa="reset-button-${childId}"]`)

  async assertErrorsContains(childId: string, expected: string) {
    await waitUntilTrue(async () =>
      ((await this.#errors(childId).text) || '').includes(expected)
    )
  }

  async resetChild(childId: string) {
    await this.#resetChild(childId).click()
  }

  async assertErrorRowCount(expected: number) {
    await waitUntilTrue(() => this.#errorsTable.visible)
    await waitUntilEqual(() => this.#errorRows.count(), expected)
  }
}

export class AssistanceNeedDecisionsReport {
  constructor(private page: Page) {}

  rows = this.page.findAllByDataQa('assistance-need-decision-row')

  async row(nth: number) {
    const row = this.rows.nth(nth)
    return {
      sentForDecision: await row.findByDataQa('sent-for-decision').text,
      childName: await row.findByDataQa('child-name').text,
      careAreaName: await row.findByDataQa('care-area-name').text,
      unitName: await row.findByDataQa('unit-name').text,
      decisionMade: await row.findByDataQa('decision-made').text,
      status: await row
        .findByDataQa('decision-chip')
        .getAttribute('data-qa-status'),
      isUnopened:
        (await row.locator
          .locator('[data-qa="unopened-indicator"]')
          .count()) === 1
    }
  }
}

export class AssistanceNeedDecisionsReportDecision {
  constructor(private page: Page) {}

  decisionMaker = this.page.findByDataQa('labelled-value-decision-maker')
  decisionStatus = new StaticChip(this.page.findByDataQa('decision-status'))
  annulmentReason = this.page.findByDataQa('labelled-value-annulment-reason')
  returnForEditBtn = this.page.findByDataQa('return-for-edit')

  get returnForEditModal() {
    return {
      okBtn: this.page.findByDataQa('modal-okBtn')
    }
  }

  approveBtn = this.page.findByDataQa('approve-button')
  rejectBtn = this.page.findByDataQa('reject-button')
  annulBtn = this.page.findByDataQa('annul-button')
  annulReasonInput = new TextInput(this.page.findByDataQa('annul-reason-input'))
  modalOkBtn = this.page.findByDataQa('modal-okBtn')

  mismatchModalLink = this.page.findByDataQa('mismatch-modal-link')

  get mismatchModal() {
    return {
      titleInput: new TextInput(this.page.findByDataQa('title-input')),
      okBtn: this.page.findByDataQa('modal-okBtn')
    }
  }
}

export class AssistanceNeedPreschoolDecisionsReportDecision {
  constructor(private page: Page) {}

  returnForEditBtn = this.page.findByDataQa('return-for-edit-button')
  approveBtn = this.page.findByDataQa('approve-button')
  rejectBtn = this.page.findByDataQa('reject-button')
  annulBtn = this.page.findByDataQa('annul-button')
  annulReasonInput = new TextInput(this.page.findByDataQa('annul-reason-input'))
  modalOkBtn = this.page.findByDataQa('modal-okBtn')

  status = this.page.findByDataQa('status')
  annulmentReason = this.page.findByDataQa('annulment-reason')
}

export class AssistanceNeedsAndActionsReport {
  constructor(private page: Page) {}
  needsAndActionsRows = this.page.findAllByDataQa(
    'assistance-needs-and-actions-row'
  )

  async assertRowAndColumn(row: number, column: number, expected: string) {
    const tableRow = this.needsAndActionsRows.nth(row)
    await tableRow.findAll('td').nth(column).assertTextEquals(expected)
  }

  async assertRow(row: number, expected: string) {
    await this.needsAndActionsRows
      .nth(row)
      .assertText((t) => t.replace(/\s/g, ',') === expected)
  }
}
