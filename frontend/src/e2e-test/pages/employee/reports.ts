// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type LocalDate from 'lib-common/local-date'

import { captureTextualDownload } from '../../browser'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import type { Page } from '../../utils/page'
import {
  Combobox,
  DatePickerDeprecated,
  Select,
  TextInput
} from '../../utils/page'

export default class ReportsPage {
  constructor(private readonly page: Page) {}

  async openApplicationsReport() {
    await this.page.find('[data-qa="report-applications"]').click()
    return new ApplicationsReport(this.page)
  }

  async openPlacementSketchingReport() {
    await this.page.find('[data-qa="report-placement-sketching"]').click()
    return new PlacementSketchingReport(this.page)
  }

  async openVoucherServiceProvidersReport() {
    await this.page.find('[data-qa="report-voucher-service-providers"]').click()
    return new VoucherServiceProvidersReport(this.page)
  }

  async openVardaErrorsReport() {
    await this.page.find('[data-qa="report-varda-errors"]').click()
    return new VardaErrorsReport(this.page)
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

export class PlacementSketchingReport {
  constructor(private page: Page) {}

  async assertRow(
    applicationId: string,
    requestedUnitName: string,
    childName: string,
    currentUnitName: string | null = null
  ) {
    const element = this.page.find(`[data-qa="${applicationId}"]`)
    await element.waitUntilVisible()

    await waitUntilEqual(
      () => element.find('[data-qa="requested-unit"]').innerText,
      requestedUnitName
    )
    if (currentUnitName) {
      await waitUntilEqual(
        () => element.find('[data-qa="current-unit"]').innerText,
        currentUnitName
      )
    }
    await waitUntilEqual(
      () => element.find('[data-qa="child-name"]').innerText,
      childName
    )
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
    expect(await row.find(`[data-qa="child-count"]`).innerText).toStrictEqual(
      expectedChildCount
    )
    expect(await row.find(`[data-qa="child-sum"]`).innerText).toStrictEqual(
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
    await waitUntilEqual(
      () => this.page.findAllByDataQa('child-name').nth(nth).textContent,
      expectedName
    )
    await waitUntilEqual(
      () =>
        this.page.findAllByDataQa('voucher-value').nth(nth).textContentAsFloat,
      expectedVoucherValue
    )
    await waitUntilEqual(
      () => this.page.findAllByDataQa('co-payment').nth(nth).textContentAsFloat,
      expectedCoPayment
    )
    await waitUntilEqual(
      () =>
        this.page.findAllByDataQa('realized-amount').nth(nth)
          .textContentAsFloat,
      expectedRealizedAmount
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
      ((await this.#errors(childId).textContent) || '').includes(expected)
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

  async row(nth: number) {
    const row = this.page
      .findAllByDataQa('assistance-need-decision-row')
      .nth(nth)

    return {
      sentForDecision: await row.findByDataQa('sent-for-decision').innerText,
      childName: await row.findByDataQa('child-name').innerText,
      careAreaName: await row.findByDataQa('care-area-name').innerText,
      unitName: await row.findByDataQa('unit-name').innerText,
      decisionMade: await row.findByDataQa('decision-made').innerText,
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

  decisionMaker = () =>
    this.page.findByDataQa('labelled-value-decision-maker').innerText
  decisionStatus = () =>
    this.page.findByDataQa('decision-status').getAttribute('data-qa-status')
  readonly returnForEditBtn = this.page.findByDataQa('return-for-edit')
  get returnForEditModal() {
    return {
      okBtn: this.page.findByDataQa('modal-okBtn')
    }
  }

  readonly approveBtn = this.page.findByDataQa('approve-button')
  readonly rejectBtn = this.page.findByDataQa('reject-button')
  readonly modalOkBtn = this.page.findByDataQa('modal-okBtn')

  readonly mismatchModalLink = this.page.findByDataQa('mismatch-modal-link')
  get mismatchModal() {
    return {
      titleInput: new TextInput(this.page.findByDataQa('title-input')),
      okBtn: this.page.findByDataQa('modal-okBtn')
    }
  }
}
