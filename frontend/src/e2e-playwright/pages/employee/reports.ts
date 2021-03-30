// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { RawElement } from 'e2e-playwright/utils/element'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { captureTextualDownload } from 'e2e-playwright/browser'

export default class ReportsPage {
  constructor(private readonly page: Page) {}

  readonly #month = new RawElement(this.page, '[data-qa="select-month"]')
  readonly #year = new RawElement(this.page, '[data-qa="select-year"]')
  readonly #area = new RawElement(this.page, '[data-qa="select-area"]')

  readonly #downloadCsvLink = new RawElement(
    this.page,
    '[data-qa="download-csv"] a'
  )

  async selectVoucherServiceProvidersReport() {
    await this.page.click('[data-qa="report-voucher-service-providers"]')
  }

  async selectMonth(month: 'Tammikuu') {
    await this.#month.click()
    await this.page.keyboard.type(month)
    await this.page.keyboard.press('Enter')
  }

  async selectYear(year: number) {
    await this.#year.click()
    await this.page.keyboard.type(year.toString())
    await this.page.keyboard.press('Enter')
  }

  async selectArea(area: string) {
    await this.#area.click()
    await this.page.keyboard.type(area)
    await this.page.keyboard.press('Enter')
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
