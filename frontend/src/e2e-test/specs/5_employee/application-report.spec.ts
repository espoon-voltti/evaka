// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import config from '../../config'
import { Fixture, testCareArea, testDaycare } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import EmployeeNav from '../../pages/employee/employee-nav'
import type { ApplicationsReport } from '../../pages/employee/reports'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let report: ApplicationsReport

beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()

  const careArea2 = await Fixture.careArea({ name: 'Toinen alue' }).save()
  await Fixture.daycare({
    areaId: careArea2.id,
    name: 'Palvelusetelikoti',
    providerType: 'PRIVATE_SERVICE_VOUCHER'
  }).save()

  const admin = await Fixture.employee().admin().save()

  page = await Page.open()
  await employeeLogin(page, admin)

  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  report = await new ReportsPage(page).openApplicationsReport()
})

describe('Reporting - applications', () => {
  test('application report is generated correctly, respecting care area filter', async () => {
    await report.assertContainsArea(testCareArea.name)
    await report.assertContainsArea('Toinen alue')
    await report.assertContainsServiceProviders([
      'Kunnallinen',
      'Palveluseteli'
    ])

    await report.selectArea('Toinen alue')
    await report.selectDateRangePickerDates(
      LocalDate.todayInSystemTz(),
      LocalDate.todayInSystemTz()
    )
    await report.assertContainsArea('Toinen alue')
    await report.assertDoesntContainArea(testCareArea.name)
  })
})
