// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DaycareId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import config from '../../config'
import {
  testDaycareGroup,
  Fixture,
  familyWithTwoGuardians,
  testCareArea,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import type { DevEmployee, DevPerson } from '../../generated/api-types'
import { ChildAttendanceReservationByChildReport } from '../../pages/employee/reports'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let child: DevPerson
let unitId: DaycareId
let admin: DevEmployee

const mockedTime = LocalDate.of(2024, 2, 19)

beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()
  await familyWithTwoGuardians.save()
  await createDefaultServiceNeedOptions()
  await createDaycareGroups({ body: [testDaycareGroup] })

  unitId = testDaycare.id
  child = familyWithTwoGuardians.children[0]
  const placement = await Fixture.placement({
    childId: child.id,
    unitId: unitId,
    startDate: mockedTime,
    endDate: mockedTime
  }).save()

  await Fixture.groupPlacement({
    daycareGroupId: testDaycareGroup.id,
    daycarePlacementId: placement.id,
    startDate: mockedTime,
    endDate: mockedTime
  }).save()

  page = await Page.open({
    mockedTime: mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  admin = await Fixture.employee().admin().save()
  await employeeLogin(page, admin)
})

describe('Child attendance reservation report', () => {
  test('Shows child attendance reservations', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      date: mockedTime,
      childId: child.id,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(10, 0)),
      secondReservation: new TimeRange(LocalTime.of(14, 0), LocalTime.of(16, 0))
    }).save()

    await page.goto(
      `${config.employeeUrl}/reports/attendance-reservation-by-child`
    )
    const report = new ChildAttendanceReservationByChildReport(page)
    await report.setDates(mockedTime, mockedTime)
    await report.selectUnit(testDaycare.name)
    await report.selectGroup(testDaycareGroup.name)
    await report.searchButton.click()
    await report.assertRows([
      {
        childName: `${child.lastName} ${child.firstName}`,
        attendanceReservationStart: '08:00',
        attendanceReservationEnd: '10:00'
      },
      {
        childName: `${child.lastName} ${child.firstName}`,
        attendanceReservationStart: '14:00',
        attendanceReservationEnd: '16:00'
      }
    ])

    await report.selectTimeFilter('00:00', '09:59')
    await report.searchButton.click()
    await report.assertRows([
      {
        childName: `${child.lastName} ${child.firstName}`,
        attendanceReservationStart: '08:00',
        attendanceReservationEnd: '10:00'
      }
    ])

    await report.selectTimeFilter('14:00', '23:59')
    await report.searchButton.click()
    await report.assertRows([
      {
        childName: `${child.lastName} ${child.firstName}`,
        attendanceReservationStart: '14:00',
        attendanceReservationEnd: '16:00'
      }
    ])
  })
})
