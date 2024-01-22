// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { resetDatabase } from '../../dev-api'
import { Fixture } from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(resetDatabase)

describe('Missing head of family report', () => {
  test('showIntentionalDuplicates filter works', async () => {
    const mockedToday = LocalDate.of(2023, 6, 12)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const unit = await Fixture.daycare().with({ areaId: area.data.id }).save()
    const child = await Fixture.person().with({ lastName: '1' }).save()
    await Fixture.child(child.data.id).save()
    await Fixture.placement()
      .with({
        type: 'DAYCARE',
        childId: child.data.id,
        unitId: unit.data.id,
        startDate: mockedToday,
        endDate: mockedToday.addDays(4)
      })
      .save()
    const duplicate = await Fixture.person()
      .with({ ssn: undefined, duplicateOf: child.data.id, lastName: '2' })
      .save()
    await Fixture.child(duplicate.data.id).save()
    await Fixture.placement()
      .with({
        type: 'DAYCARE',
        childId: duplicate.data.id,
        unitId: unit.data.id,
        startDate: mockedToday,
        endDate: mockedToday.addDays(2)
      })
      .save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0)),
      employeeCustomizations: {
        featureFlags: { personDuplicate: true }
      }
    })
    const report = await navigateToReport(page, admin.data)
    await report.assertRows([
      {
        childName: `${child.data.lastName} ${child.data.firstName}`,
        rangesWithoutHead: '12.06.2023 - 16.06.2023'
      }
    ])

    await report.toggleShowIntentionalDuplicates()
    await report.assertRows([
      {
        childName: `${child.data.lastName} ${child.data.firstName}`,
        rangesWithoutHead: '12.06.2023 - 16.06.2023'
      },
      {
        childName: `${duplicate.data.lastName} ${duplicate.data.firstName}`,
        rangesWithoutHead: '12.06.2023 - 14.06.2023'
      }
    ])

    await report.toggleShowIntentionalDuplicates()
    await report.assertRows([
      {
        childName: `${child.data.lastName} ${child.data.firstName}`,
        rangesWithoutHead: '12.06.2023 - 16.06.2023'
      }
    ])
  })
})

const navigateToReport = async (page: Page, user: EmployeeDetail) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openMissingHeadOfFamilyReport()
}
