// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import {
  insertDefaultServiceNeedOptions,
  insertStaffRealtimeAttendance,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  careArea2Fixture,
  daycare2Fixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { Child, Daycare, EmployeeDetail } from '../../dev-api/types'
import { UnitPage } from '../../pages/employee/units/unit'
import { UnitAttendancesPage } from '../../pages/employee/units/unit-attendances-page'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
let calendarPage: UnitAttendancesPage
let child1Fixture: Child
let child1DaycarePlacementId: UUID
let daycare: Daycare
let unitSupervisor: EmployeeDetail
let staff: EmployeeDetail[]

const mockedToday = LocalDate.of(2022, 3, 28)
const placementStartDate = mockedToday.subWeeks(4)
const placementEndDate = mockedToday.addWeeks(4)
const groupId: UUID = uuidv4()

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  daycare = (
    await Fixture.daycare()
      .with({
        ...daycare2Fixture,
        enabledPilotFeatures: ['REALTIME_STAFF_ATTENDANCE']
      })
      .careArea(careArea)
      .save()
  ).data

  unitSupervisor = (await Fixture.employeeUnitSupervisor(daycare.id).save())
    .data

  await insertDefaultServiceNeedOptions()

  await Fixture.daycareGroup()
    .with({
      id: groupId,
      daycareId: daycare.id,
      name: 'Testailijat'
    })
    .save()

  child1Fixture = fixtures.familyWithTwoGuardians.children[0]
  child1DaycarePlacementId = uuidv4()
  await Fixture.placement()
    .with({
      id: child1DaycarePlacementId,
      childId: child1Fixture.id,
      unitId: daycare.id,
      startDate: placementStartDate.formatIso(),
      endDate: placementEndDate.formatIso()
    })
    .save()

  await Fixture.groupPlacement()
    .with({
      daycareGroupId: groupId,
      daycarePlacementId: child1DaycarePlacementId,
      startDate: placementStartDate.formatIso(),
      endDate: placementEndDate.formatIso()
    })
    .save()

  staff = [
    (await Fixture.employeeStaff(daycare.id).save()).data,
    (
      await Fixture.employee()
        .with({
          email: 'kalle.kasvattaja@evaka.test',
          firstName: 'Kalle',
          lastName: 'Kasvattaja',
          roles: []
        })
        .withDaycareAcl(daycare.id, 'STAFF')
        .save()
    ).data
  ]
  await Fixture.staffOccupancyCoefficient(daycare.id, staff[1].id).save()

  await insertStaffRealtimeAttendance({
    id: uuidv4(),
    employeeId: staff[0].id,
    groupId: groupId,
    arrived: HelsinkiDateTime.of(2022, 3, 27, 7, 0),
    departed: HelsinkiDateTime.of(2022, 3, 27, 15, 0),
    occupancyCoefficient: 7.0
  })
  await insertStaffRealtimeAttendance({
    id: uuidv4(),
    employeeId: staff[1].id,
    groupId: groupId,
    arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
    occupancyCoefficient: 7.0
  })

  page = await Page.open({
    viewport: { width: 1440, height: 720 },
    mockedTime: mockedToday
      .toHelsinkiDateTime(LocalTime.of(0, 0))
      .toSystemTzDate()
  })
  await employeeLogin(page, unitSupervisor)
})

const openAttendancesPage = async (): Promise<UnitAttendancesPage> => {
  unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(daycare.id)
  return await unitPage.openAttendancesPage()
}

describe('Realtime staff attendances', () => {
  describe('Group selection: staff', () => {
    beforeEach(async () => {
      calendarPage = await openAttendancesPage()
      await calendarPage.selectGroup('staff')
    })

    test('The staff attendances table shows all unit staff', async () => {
      await calendarPage.assertStaffInAttendanceTable(
        staff.map((s) => `${s.lastName} ${s.firstName}`)
      )
    })

    test('The icon tells whether a staff member is counted in occupancy or not', async () => {
      await calendarPage.assertPositiveOccupancyCoefficientCount(1)
      await calendarPage.assertZeroOccupancyCoefficientCount(1)
    })

    test('It is not possible to create new entries', async () => {
      await calendarPage.selectGroup('staff')
      await calendarPage.assertNoTimeInputsVisible()
      await calendarPage.clickEditOnRow(0)
      await calendarPage.assertNoTimeInputsVisible()
      await calendarPage.clickCommitOnRow(0)
    })

    test('Sunday entries are shown in the calendar', async () => {
      await calendarPage.selectGroup('staff')
      await calendarPage.navigateToPreviousWeek()
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 6,
        arrival: '07:00',
        departure: '15:00'
      })
    })

    describe('With one attendance entry', () => {
      test('Existing entries can be edited', async () => {
        await calendarPage.selectGroup('staff')
        await calendarPage.assertNoTimeInputsVisible()
        await calendarPage.clickEditOnRow(1)
        await calendarPage.assertCountTimeInputsVisible(1)
      })

      test('Editing an existing entry updates it', async () => {
        await calendarPage.selectGroup('staff')
        await calendarPage.assertNoTimeInputsVisible()
        await calendarPage.assertArrivalDeparture({
          rowIx: 1,
          nth: 0,
          arrival: '07:00',
          departure: '-'
        })
        await calendarPage.clickEditOnRow(1)
        await calendarPage.setNthArrivalDeparture(0, '07:00', '15:00')
        await calendarPage.closeInlineEditor()
        await calendarPage.assertArrivalDeparture({
          rowIx: 1,
          nth: 0,
          arrival: '07:00',
          departure: '15:00'
        })
      })
    })
  })
})
