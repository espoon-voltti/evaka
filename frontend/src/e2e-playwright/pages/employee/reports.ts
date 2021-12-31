// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Combobox,
  DatePickerDeprecated,
  Page,
  Select
} from 'e2e-playwright/utils/page'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { captureTextualDownload } from 'e2e-playwright/browser'
import LocalDate from 'lib-common/local-date'

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
}
