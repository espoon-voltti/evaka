// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Select } from 'e2e-playwright/utils/page'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { captureTextualDownload } from 'e2e-playwright/browser'

export default class ReportsPage {
  constructor(private readonly page: Page) {}

  readonly #month = new Select(
    this.page.find('[data-qa="select-month"] select')
  )
  readonly #year = new Select(this.page.find('[data-qa="select-year"] select'))
  readonly #area = new Select(this.page.find('[data-qa="select-area"] select'))

  readonly #downloadCsvLink = this.page.find('[data-qa="download-csv"] a')

  async selectVoucherServiceProvidersReport() {
    await this.page.find('[data-qa="report-voucher-service-providers"]').click()
  }

  async selectMonth(month: 'Tammikuu') {
    await this.#month.selectOption({ label: month.toLowerCase() })
  }

  async selectYear(year: number) {
    await this.#year.selectOption({ label: String(year) })
  }

  async selectArea(area: string) {
    await this.#area.selectOption({ label: area })
  }

  async assertVoucherServiceProviderRowCount(expectedChildCount: number) {
    await waitUntilEqual(
      () => this.page.findAll('.reportRow').count(),
      expectedChildCount
    )
  }

  async assertVoucherServiceProviderRow(
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
