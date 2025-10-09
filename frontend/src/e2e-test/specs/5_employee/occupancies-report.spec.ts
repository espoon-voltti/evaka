// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let admin: DevEmployee

beforeEach(async () => {
  await resetServiceState()

  admin = await Fixture.employee().admin().save()
})

describe('Occupancies report', () => {
  test('confirmed type shows date columns for whole month', async () => {
    const mockedToday = LocalDate.of(2025, 10, 2)
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(6, 11))
    })
    const report = await navigateToReport(page, admin)
    await report.areaCombobox.fillAndSelectFirst('Kaikki')
    // 'Vahvistettu täyttöaste yksikössä' selected by default
    await report.assertReportDateColumns(
      LocalDate.range(
        LocalDate.of(2025, 10, 1),
        LocalDate.of(2025, 10, 31)
      ).filter((date) => !date.isWeekend())
    )
  })

  test('planned type shows date columns for whole month', async () => {
    const mockedToday = LocalDate.of(2025, 10, 2)
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(6, 11))
    })
    const report = await navigateToReport(page, admin)
    await report.areaCombobox.fillAndSelectFirst('Kaikki')
    await report.typeCombobox.fill('')
    await report.typeCombobox.fillAndSelectFirst(
      'Suunniteltu täyttöaste yksikössä'
    )
    await report.assertReportDateColumns(
      LocalDate.range(
        LocalDate.of(2025, 10, 1),
        LocalDate.of(2025, 10, 31)
      ).filter((date) => !date.isWeekend())
    )
  })

  test('realized type shows date columns only from past', async () => {
    const mockedToday = LocalDate.of(2025, 10, 3)
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(6, 11))
    })
    const report = await navigateToReport(page, admin)
    await report.areaCombobox.fillAndSelectFirst('Kaikki')
    await report.typeCombobox.fill('')
    await report.typeCombobox.fillAndSelectFirst('Käyttöaste yksikössä')
    await report.assertReportDateColumns([
      LocalDate.of(2025, 10, 1),
      LocalDate.of(2025, 10, 2)
    ])
  })

  test('realized type shows no date columns on first day of month', async () => {
    const mockedToday = LocalDate.of(2025, 10, 1)
    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(6, 11))
    })
    const report = await navigateToReport(page, admin)
    await report.areaCombobox.fillAndSelectFirst('Kaikki')
    await report.typeCombobox.fill('')
    await report.typeCombobox.fillAndSelectFirst('Käyttöaste yksikössä')
    await report.assertReportDateColumns([])
  })
})

const navigateToReport = async (page: Page, user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openOccupanciesReport()
}
