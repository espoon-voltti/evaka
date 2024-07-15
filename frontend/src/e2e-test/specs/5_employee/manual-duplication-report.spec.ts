// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import { applicationFixture, Fixture, uuidv4 } from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import EmployeeNav from '../../pages/employee/employee-nav'
import ReportsPage from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(async (): Promise<void> => resetServiceState())

describe('Manual duplication report', () => {
  test('works', async () => {
    const mockedToday = LocalDate.of(2023, 6, 19)

    const admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const preschool = await Fixture.daycare({
      areaId: area.id,
      type: ['PRESCHOOL']
    }).save()
    const daycare = await Fixture.daycare({
      areaId: area.id,
      type: ['CENTRE']
    }).save()
    const guardian = await Fixture.person().with({ ssn: null }).saveAdult()
    const child = await Fixture.person().with({ ssn: null }).saveChild()

    const page = await Page.open({
      mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0))
    })

    const application1Id = uuidv4()
    await createApplications({
      body: [
        {
          ...applicationFixture(
            child,
            guardian,
            undefined,
            'PRESCHOOL',
            null,
            [preschool.id],
            true,
            'SENT',
            mockedToday
          ),
          id: application1Id
        }
      ]
    })
    await Fixture.decision({
      applicationId: application1Id,
      employeeId: admin.id,
      unitId: preschool.id,
      type: 'PRESCHOOL',
      startDate: mockedToday,
      endDate: mockedToday,
      status: 'ACCEPTED'
    }).save()
    await Fixture.decision({
      applicationId: application1Id,
      employeeId: admin.id,
      unitId: daycare.id,
      type: 'PRESCHOOL_DAYCARE',
      startDate: mockedToday,
      endDate: mockedToday,
      status: 'ACCEPTED'
    }).save()

    const application2Id = uuidv4()
    await createApplications({
      body: [
        {
          ...applicationFixture(
            child,
            guardian,
            undefined,
            'PRESCHOOL',
            null,
            [preschool.id],
            true,
            'SENT',
            mockedToday
          ),
          id: application2Id
        }
      ]
    })
    await Fixture.decision({
      applicationId: application2Id,
      employeeId: admin.id,
      unitId: daycare.id,
      type: 'PRESCHOOL',
      startDate: mockedToday,
      endDate: mockedToday,
      status: 'ACCEPTED'
    }).save()
    await Fixture.decision({
      applicationId: application2Id,
      employeeId: admin.id,
      unitId: daycare.id,
      type: 'PRESCHOOL_DAYCARE',
      startDate: mockedToday,
      endDate: mockedToday,
      status: 'ACCEPTED'
    }).save()

    const report = await navigateToReport(page, admin)
    await report.assertRows([
      {
        childName: `${child.lastName}, ${child.firstName}`,
        connectedUnitName: daycare.name,
        serviceNeedOptionName: '',
        preschoolUnitName: preschool.name
      }
    ])
  })
})

const navigateToReport = async (page: Page, user: DevEmployee) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).openTab('reports')
  return await new ReportsPage(page).openManualDuplicationReport()
}
