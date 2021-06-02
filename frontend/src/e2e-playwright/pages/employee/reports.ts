// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { Combobox, RawElement } from 'e2e-playwright/utils/element'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { captureTextualDownload } from 'e2e-playwright/browser'

export default class ReportsPage {
  constructor(private readonly page: Page) {}

  readonly #month = new Combobox(this.page, '[data-qa="select-month"]')
  readonly #year = new Combobox(this.page, '[data-qa="select-year"]')
  readonly #area = new Combobox(this.page, '[data-qa="select-area"]')

  readonly #downloadCsvLink = new RawElement(
    this.page,
    '[data-qa="download-csv"] a'
  )

  async selectVoucherServiceProvidersReport() {
    await this.page.click('[data-qa="report-voucher-service-providers"]')
  }

  async selectMonth(month: 'Tammikuu') {
    await this.#month.fill(month)
    await this.#month.findItem(month).click()
  }

  async selectYear(year: number) {
    await this.#year.fill(year.toString())
    await this.#year.findItem(year.toString()).click()
  }

  async selectArea(area: string) {
    await this.#area.fill(area)
    await this.#area.findItem(area).click()
  }

  async assertVoucherServiceProviderRowCount(expectedChildCount: number) {
    await waitUntilEqual(
      () =>
        this.page.evaluate(
          () => document.querySelectorAll('.reportRow').length
        ),
      expectedChildCount
    )
  }

  async assertVoucherServiceProviderRow(
    unitId: string,
    expectedChildCount: string,
    expectedMonthlySum: string
  ) {
    const row = new RawElement(this.page, `[data-qa="${unitId}"]`)
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
      this.page.waitForEvent('download'),
      this.#downloadCsvLink.click()
    ])
    return captureTextualDownload(download)
  }
}
