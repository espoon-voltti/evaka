// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ClientFunction, Selector, t } from 'testcafe'
import config from '../config'
import { format } from 'date-fns'

export default class ReportsPage {
  readonly url = config.employeeUrl
  readonly downloadCsvLink = Selector('[data-qa="download-csv"] a')

  async selectReportsTab() {
    await t.click(Selector('[data-qa="reports-nav"]'))
  }

  async selectVoucherServiceProvidersReport() {
    await t.click(Selector('[data-qa="report-voucher-service-providers"]'))
  }

  async selectApplicationsReport() {
    await t.click(Selector('[data-qa="report-applications"]'))
  }

  async selectPlacementSketchingReport() {
    await t.click(Selector('[data-qa="report-placement-sketching"]'))
  }

  async selectMonth(month: 'Tammikuu') {
    const monthSelector = Selector('[data-qa="select-month"]')
    await t.click(monthSelector)
    await t.typeText(monthSelector, month)
    await t.pressKey('enter')
  }

  async selectYear(year: number) {
    const yearSelector = Selector('[data-qa="select-year"]')
    await t.click(yearSelector)
    await t.typeText(yearSelector, year.toString())
    await t.pressKey('enter')
  }

  async selectArea(area: string) {
    const areaSelector = Selector('[data-qa="select-area"]')
    await t.click(areaSelector)
    await t.typeText(areaSelector, area)
    await t.pressKey('enter')
  }

  async selectDateRangePickerDates(from: Date, to: Date) {
    const fromInput = Selector('[data-qa="datepicker-from"] input')
    const toInput = Selector('[data-qa="datepicker-to"] input')
    await t.selectText(fromInput).pressKey('delete')
    await t.typeText(fromInput, format(from, 'dd.MM.yyyy'))
    await t.selectText(toInput).pressKey('delete')
    await t.typeText(toInput, format(to, 'dd.MM.yyyy'))
  }

  async assertVoucherServiceProviderRowCount(expectedChildCount: number) {
    await t.expect(Selector('.reportRow').count).eql(expectedChildCount)
  }

  async assertVoucherServiceProviderRow(
    unitId: string,
    expectedChildCount: string,
    expectedMonthlySum: string
  ) {
    const unitRowSelector = Selector(`[data-qa="${unitId}"]`)
    await t.expect(unitRowSelector.exists).ok()
    await t
      .expect(unitRowSelector.find('[data-qa="child-count"]').innerText)
      .eql(expectedChildCount)
    await t
      .expect(unitRowSelector.find('[data-qa="child-sum"]').innerText)
      .eql(expectedMonthlySum)
  }

  async assertApplicationsReportContainsArea(area: string) {
    const applicationTableSelector = Selector(
      `[data-qa="report-application-table"]`
    )
    await t.expect(applicationTableSelector.exists).ok()
    await t.expect(applicationTableSelector.find('td').innerText).eql(area)
  }

  async assertPlacementSketchingRow(
    requestedUnitId: string,
    childId: string,
    requestedUnitName: string,
    childName: string,
    currentUnitName: string | null = null
  ) {
    const childSelector = Selector(`[data-qa="${requestedUnitId}:${childId}"]`)
    await t.expect(childSelector.exists).ok()

    await t
      .expect(childSelector.find('[data-qa="requested-unit"]').innerText)
      .eql(requestedUnitName)

    if (currentUnitName)
      await t
        .expect(childSelector.find('[data-qa="current-unit"]').innerText)
        .eql(currentUnitName)

    await t
      .expect(childSelector.find('[data-qa="child-name"]').innerText)
      .eql(childName)
  }

  async getCsvReport(): Promise<string> {
    const path = await this.downloadCsvLink.getAttribute('href')
    const getCsvFile = ClientFunction((url) => {
      return new Promise<string>((resolve) => {
        const xhr = new XMLHttpRequest()
        xhr.open('GET', url)

        xhr.onload = function () {
          resolve(xhr.responseText)
        }

        xhr.send(null)
      })
    })

    return await getCsvFile(`${path}`)
  }
}
