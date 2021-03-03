// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ReportsPage from '../../pages/reports'
import { seppoAdminRole } from '../../config/users'

fixture('Reporting - applications').meta({
  type: 'regression',
  subType: 'reports'
})

const reports = new ReportsPage()

test('application report is generated correctly, respecting care area filter', async (t) => {
  await t.useRole(seppoAdminRole)
  await reports.selectReportsTab()
  await reports.selectApplicationsReport()

  await reports.assertApplicationsReportContainsArea(
    'Espoon keskus (eteläinen)'
  )
  await reports.selectArea('Espoonlahti')
  await reports.selectDateRangePickerDates(new Date(), new Date())
  await reports.assertApplicationsReportContainsArea('Espoonlahti')
})
