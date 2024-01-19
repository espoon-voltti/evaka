// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { insertApplications, resetDatabase } from '../../dev-api'
import { Fixture, applicationFixture, uuidv4 } from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(resetDatabase)

describe('Manual duplication report', () => {
  test('works', async () => {
    const mockedToday = LocalDate.of(2023, 6, 19)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const preschool = await Fixture.daycare()
      .with({ areaId: area.data.id, type: ['PRESCHOOL'] })
      .save()
    const daycare = await Fixture.daycare()
      .with({ areaId: area.data.id, type: ['CENTRE'] })
      .save()
    const guardian = await Fixture.person().with({ ssn: undefined }).save()
    const child = await Fixture.person().with({ ssn: undefined }).save()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const application1Id = uuidv4()
    await insertApplications([
      {
        ...applicationFixture(
          child.data,
          guardian.data,
          undefined,
          'PRESCHOOL',
          null,
          [preschool.data.id],
          true,
          'SENT',
          mockedToday
        ),
        id: application1Id
      }
    ])
    await Fixture.decision()
      .with({
        applicationId: application1Id,
        employeeId: admin.data.id,
        unitId: preschool.data.id,
        type: 'PRESCHOOL',
        startDate: mockedToday,
        endDate: mockedToday,
        status: 'ACCEPTED'
      })
      .save()
    await Fixture.decision()
      .with({
        applicationId: application1Id,
        employeeId: admin.data.id,
        unitId: daycare.data.id,
        type: 'PRESCHOOL_DAYCARE',
        startDate: mockedToday,
        endDate: mockedToday,
        status: 'ACCEPTED'
      })
      .save()

    const application2Id = uuidv4()
    await insertApplications([
      {
        ...applicationFixture(
          child.data,
          guardian.data,
          undefined,
          'PRESCHOOL',
          null,
          [preschool.data.id],
          true,
          'SENT',
          mockedToday
        ),
        id: application2Id
      }
    ])
    await Fixture.decision()
      .with({
        applicationId: application2Id,
        employeeId: admin.data.id,
        unitId: daycare.data.id,
        type: 'PRESCHOOL',
        startDate: mockedToday,
        endDate: mockedToday,
        status: 'ACCEPTED'
      })
      .save()
    await Fixture.decision()
      .with({
        applicationId: application2Id,
        employeeId: admin.data.id,
        unitId: daycare.data.id,
        type: 'PRESCHOOL_DAYCARE',
        startDate: mockedToday,
        endDate: mockedToday,
        status: 'ACCEPTED'
      })
      .save()

    const report = await navigateToReport(page, admin.data)
    await report.assertRows([
      {
        childName: `${child.data.lastName}, ${child.data.firstName}`,
        connectedUnitName: daycare.data.name,
        serviceNeedOptionName: '',
        preschoolUnitName: preschool.data.name
      }
    ])
  })
})

const navigateToReport = async (page: Page, user: EmployeeDetail) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openManualDuplicationReport()
}
