// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { deleteApplication } from '../../../e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../../e2e-test-common/dev-api/data-init'
import { Fixture } from '../../../e2e-test-common/dev-api/fixtures'
import { seppoAdminRole } from '../../config/users'
import Home from '../../pages/home'
import ReportsPage from '../../pages/reports'
import { logConsoleMessages } from '../../utils/fixture'

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

const home = new Home()
const reports = new ReportsPage()

let applicationId: string | null = null

fixture('Reporting - applications')
  .meta({ type: 'regression', subType: 'reports' })
  .page(home.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()

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
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    if (applicationId) await deleteApplication(applicationId)
    applicationId = null
  })
  .after(async () => await cleanUp())

test('application report is generated correctly, respecting care area filter', async (t) => {
  await t.useRole(seppoAdminRole)
  await reports.selectReportsTab()
  await reports.selectApplicationsReport()

  await reports.assertApplicationsReportContainsArea('Superkeskus')
  await reports.assertApplicationsReportContainsArea('Toinen alue')
  await reports.assertApplicationsReportContainsServiceProviders([
    'Kunnallinen',
    'Palveluseteli'
  ])

  await reports.selectArea('Toinen alue')
  await reports.selectDateRangePickerDates(new Date(), new Date())
  await reports.assertApplicationsReportContainsArea('Toinen alue')
  await reports.assertApplicationsReportNotContainsArea('Superkeskus')
})
