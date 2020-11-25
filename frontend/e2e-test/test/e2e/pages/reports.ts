// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../config'
import { ClientFunction, Selector, t } from 'testcafe'

export default class ReportsPage {
  readonly url = config.employeeUrl
  readonly downloadCsvLink = Selector('[data-qa="download-csv"] a')

  async selectReportsTab() {
    await t.click(Selector('[data-qa="reports-nav"]'))
  }

  async selectVoucherServiceProvidersReport() {
    await t.click(Selector('[data-qa="report-voucher-service-providers"]'))
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
