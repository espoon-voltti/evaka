// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { resetDatabase } from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../../e2e-test-common/dev-api/data-init'
import { Fixture } from '../../../e2e-test-common/dev-api/fixtures'
import { employeeLogin, seppoAdmin } from '../../config/users'
import Home from '../../pages/home'
import ReportsPage from '../../pages/reports'
import { logConsoleMessages } from '../../utils/fixture'

let fixtures: AreaAndPersonFixtures

const home = new Home()
const reports = new ReportsPage()

fixture('Reporting - applications')
  .meta({ type: 'regression', subType: 'reports' })
  .beforeEach(async () => {
    await resetDatabase()
    ;[fixtures] = await initializeAreaAndPersonData()

    const careArea = await Fixture.careArea()
      .with({ name: 'Toinen alue' })
      .save()

    await Fixture.daycare()
      .with({
        name: 'Palvelusetelikoti',
        providerType: 'PRIVATE_SERVICE_VOUCHER'
      })
      .careArea(careArea)
      .save()
  })
  .afterEach(logConsoleMessages)

test('application report is generated correctly, respecting care area filter', async (t) => {
  await employeeLogin(t, seppoAdmin, home.homePage('admin'))
  await reports.selectReportsTab()
  await reports.selectApplicationsReport()

  await reports.assertApplicationsReportContainsArea(
    fixtures.careAreaFixture.name
  )
  await reports.assertApplicationsReportContainsArea('Toinen alue')
  await reports.assertApplicationsReportContainsServiceProviders([
    'Kunnallinen',
    'Palveluseteli'
  ])

  await reports.selectArea('Toinen alue')
  await reports.selectDateRangePickerDates(new Date(), new Date())
  await reports.assertApplicationsReportContainsArea('Toinen alue')
  await reports.assertApplicationsReportNotContainsArea(
    fixtures.careAreaFixture.name
  )
})
