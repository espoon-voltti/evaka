// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type {
  DevDaycare,
  DevDaycareGroup,
  DevEmployee,
  DevPerson
} from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedToday = LocalDate.of(2024, 3, 15)

test.describe('Child absence report', () => {
  test.use({
    evakaOptions: {
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    }
  })

  let child: DevPerson
  let unit: DevDaycare
  let group: DevDaycareGroup
  let admin: DevEmployee

  test.beforeEach(async () => {
    await resetServiceState()
    const area = await Fixture.careArea().save()
    unit = await Fixture.daycare({
      areaId: area.id,
      name: 'Testipäiväkoti',
      language: 'fi'
    }).save()
    group = await Fixture.daycareGroup({
      daycareId: unit.id,
      name: 'Avoin ryhmä'
    }).save()
    child = await Fixture.person({
      firstName: 'Esko',
      lastName: 'Beck'
    }).saveChild()

    const placement = await Fixture.placement({
      type: 'DAYCARE',
      childId: child.id,
      unitId: unit.id,
      startDate: mockedToday.subDays(20),
      endDate: mockedToday.addDays(20)
    }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: placement.id,
      daycareGroupId: group.id,
      startDate: mockedToday.subDays(20),
      endDate: mockedToday.addDays(20)
    }).save()

    // full-day UNKNOWN_ABSENCE on a past day (DAYCARE = BILLABLE only)
    await Fixture.absence({
      absenceType: 'UNKNOWN_ABSENCE',
      absenceCategory: 'BILLABLE',
      date: mockedToday.subDays(2),
      childId: child.id
    }).save()
    // future absence is clamped out
    await Fixture.absence({
      absenceType: 'UNKNOWN_ABSENCE',
      absenceCategory: 'BILLABLE',
      date: mockedToday.addDays(1),
      childId: child.id
    }).save()

    admin = await Fixture.employee().admin().save()
  })

  test('report data is shown', async ({ evaka }) => {
    const report = await navigateToReport(evaka, admin)
    await report.selectUnit(unit.name)
    await report.selectRange(mockedToday.subDays(10), mockedToday)

    await report.assertRows([
      {
        firstName: child.firstName,
        lastName: child.lastName,
        daycareName: 'Testipäiväkoti',
        groupName: 'Avoin ryhmä',
        TOTAL: '1',
        OTHER_ABSENCE: '0',
        SICKLEAVE: '0',
        PLANNED_ABSENCE: '0',
        UNKNOWN_ABSENCE: '1'
      }
    ])
  })
})

const navigateToReport = async (page: Page, user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openChildAbsenceReport()
}
