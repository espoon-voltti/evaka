// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(async (): Promise<void> => resetServiceState())

describe('Ended placements report', () => {
  test('works', async () => {
    const mockedToday = LocalDate.of(2023, 6, 19)

    const admin = await Fixture.employee().admin().save()
    const area = await Fixture.careArea({ name: 'Alue 1A' }).save()

    const daycare = await Fixture.daycare({
      areaId: area.id,
      type: ['CENTRE']
    }).save()
    const child = await Fixture.person({
      ssn: null,
      firstName: 'Testi',
      lastName: 'Henkilö'
    }).saveChild()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const endedPlacement = await Fixture.placement({
      childId: child.id,
      unitId: daycare.id,
      startDate: mockedToday.subMonths(1),
      endDate: mockedToday.subDays(2)
    }).save()

    const nextPlacement = await Fixture.placement({
      childId: child.id,
      unitId: daycare.id,
      startDate: mockedToday.addMonths(1),
      endDate: mockedToday.addMonths(2)
    }).save()

    const report = await navigateToReport(page, admin)
    await report.assertRows([
      {
        childName: `${child.lastName} ${child.firstName}`,
        areaName: area.name,
        unitName: daycare.name,
        placementEnd: endedPlacement.endDate,
        nextPlacementStart: nextPlacement.startDate
      }
    ])
  })
})

const navigateToReport = async (page: Page, user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openEndedPlacementsReport()
}
