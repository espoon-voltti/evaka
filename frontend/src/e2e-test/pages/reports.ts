// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import config from 'e2e-test-common/config'
import { format } from 'date-fns'

export default class ReportsPage {
  readonly url = config.employeeUrl

  async selectReportsTab() {
    await t.click(Selector('[data-qa="reports-nav"]'))
  }

  async selectApplicationsReport() {
    await t.click(Selector('[data-qa="report-applications"]'))
  }

  async selectPlacementSketchingReport() {
    await t.click(Selector('[data-qa="report-placement-sketching"]'))
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

  private async areaWithNameExistsAssertion(
    area: string,
    exists = true
  ): Promise<Assertion<boolean>> {
    const applicationTableSelector = Selector(
      `[data-qa="report-application-table"]`
    )
    await t.expect(applicationTableSelector.exists).ok()
    return t
      .expect(
        applicationTableSelector.find('td:first-child').withExactText(area)
          .exists
      )
      .eql(exists)
  }

  async assertApplicationsReportContainsArea(area: string) {
    await this.areaWithNameExistsAssertion(area, true)
  }

  async assertApplicationsReportNotContainsArea(area: string) {
    await this.areaWithNameExistsAssertion(area, false)
  }

  async assertApplicationsReportContainsServiceProviders(providers: string[]) {
    const applicationTableSelector = Selector(
      `[data-qa="report-application-table"]`
    )
    await t.expect(applicationTableSelector.exists).ok()
    for (const provider of providers) {
      await t
        .expect(
          applicationTableSelector
            .find('td:nth-child(3)')
            .withExactText(provider).exists
        )
        .ok(`Service provider ${provider} does not exist`)
    }
  }

  async assertPlacementSketchingRow(
    applicationId: string,
    requestedUnitName: string,
    childName: string,
    currentUnitName: string | null = null
  ) {
    const childSelector = Selector(`[data-qa="${applicationId}"]`)
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
}
