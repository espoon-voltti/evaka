// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { evakaUserId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import config from '../../config'
import {
  testDaycareGroup,
  Fixture,
  familyWithTwoGuardians,
  testCareArea,
  testDaycare,
  testDaycare2
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
  admin = await Fixture.employee().admin().save()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.daycare({
    ...testDaycare2,
    areaId: testCareArea.id,
    closingDate: LocalDate.of(2024, 1, 1)
  }).save()
  await familyWithTwoGuardians.save()
  await createDefaultServiceNeedOptions()
  const fullTimeServiceNeedOption1 = await Fixture.serviceNeedOption({
    nameFi: 'Kokopäiväinen 1',
    validPlacementType: 'DAYCARE',
    validFrom: LocalDate.of(2020, 1, 1),
    validTo: null
  }).save()
  await createDaycareGroups({
    body: [
      testDaycareGroup,
      Fixture.daycareGroup({
        daycareId: testDaycare.id,
        name: 'Suljettu ryhmä',
        endDate: LocalDate.of(2024, 1, 1)
      })
    ]
  })

  unitId = testDaycare.id
  child = familyWithTwoGuardians.children[0]

  const placement = await Fixture.placement({
    childId: child.id,
    unitId: unitId,
    startDate: mockedTime,
    endDate: mockedTime.addDays(1)
  }).save()

  await Fixture.serviceNeed({
    placementId: placement.id,
    optionId: fullTimeServiceNeedOption1.id,
    confirmedBy: evakaUserId(admin.id),
    shiftCare: 'NONE',
    startDate: mockedTime,
    endDate: mockedTime
  }).save()

  await Fixture.serviceNeed({
    placementId: placement.id,
    optionId: fullTimeServiceNeedOption1.id,
    confirmedBy: evakaUserId(admin.id),
    shiftCare: 'FULL',
    startDate: mockedTime.addDays(1),
    endDate: mockedTime.addDays(1)
  }).save()

  await Fixture.groupPlacement({
    daycareGroupId: testDaycareGroup.id,
    daycarePlacementId: placement.id,
    startDate: mockedTime,
    endDate: mockedTime.addDays(1)
  }).save()

  page = await Page.open({
    mockedTime: mockedTime.toHelsinkiDateTime(LocalTime.of(12, 0))
  })

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

  test('Shift care filtering works', async () => {
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
        attendanceReservationStart: null,
        attendanceReservationEnd: null
      }
    ])

    await report.filterByShiftCare.click()
    await report.assertRows([])

    //next day has a shift care service need
    await report.setDates(mockedTime.addDays(1), mockedTime.addDays(1))

    await report.searchButton.click()
    await report.assertRows([
      {
        childName: `${child.lastName} ${child.firstName}`,
        attendanceReservationStart: null,
        attendanceReservationEnd: null
      }
    ])

    await report.filterByShiftCare.click()
    await report.assertRows([
      {
        childName: `${child.lastName} ${child.firstName}`,
        attendanceReservationStart: null,
        attendanceReservationEnd: null
      }
    ])
  })

  test('Closed filtering works', async () => {
    await page.goto(
      `${config.employeeUrl}/reports/attendance-reservation-by-child`
    )
    const report = new ChildAttendanceReservationByChildReport(page)
    await report.setDates(mockedTime, mockedTime)
    await report.unitSelector.click()
    await report.unitSelector.assertOptions([
      testDaycare.name,
      testDaycare2.name
    ])
    await report.unitSelector.click()
    await report.selectUnit(testDaycare.name)
    await report.groupSelector.click()
    await report.groupSelector.assertOptions([
      testDaycareGroup.name,
      'Suljettu ryhmä'
    ])
    await report.groupSelector.click()

    await report.filterByClosed.click()

    await report.unitSelector.click()
    await report.unitSelector.assertOptions([testDaycare.name])
    await report.unitSelector.click()
    await report.groupSelector.click()
    await report.groupSelector.assertOptions([testDaycareGroup.name])
  })
})
