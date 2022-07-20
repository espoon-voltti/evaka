// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

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
import { waitUntilEqual } from '../../utils'
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
let groupStaff: EmployeeDetail

const mockedToday = LocalDate.of(2022, 3, 28)
const placementStartDate = mockedToday.subWeeks(4)
const placementEndDate = mockedToday.addWeeks(4)
const groupId: UUID = uuidv4()
const groupId2 = uuidv4()

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

  await Fixture.daycareGroup()
    .with({
      id: groupId2,
      daycareId: daycare.id,
      name: 'Testailijat 2'
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

  groupStaff = (
    await Fixture.employee()
      .with({
        email: 'kalle.kasvattaja@evaka.test',
        firstName: 'Kalle',
        lastName: 'Kasvattaja',
        roles: []
      })
      .withDaycareAcl(daycare.id, 'STAFF')
      .withGroupAcl(groupId)
      .withGroupAcl(groupId2)
      .save()
  ).data
  staff = [(await Fixture.employeeStaff(daycare.id).save()).data, groupStaff]
  await Fixture.staffOccupancyCoefficient(daycare.id, staff[1].id).save()

  await insertStaffRealtimeAttendance({
    id: uuidv4(),
    employeeId: staff[0].id,
    groupId: groupId,
    arrived: mockedToday.subDays(1).toHelsinkiDateTime(LocalTime.of(7, 0)),
    departed: mockedToday.subDays(1).toHelsinkiDateTime(LocalTime.of(15, 0)),
    occupancyCoefficient: 7.0
  })

  page = await Page.open({
    viewport: { width: 1440, height: 720 },
    mockedTime: mockedToday
      .toHelsinkiDateTime(LocalTime.of(12, 0))
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
  test('Occupancy graph', async () => {
    await insertStaffRealtimeAttendance({
      id: uuidv4(),
      employeeId: staff[1].id,
      groupId: groupId,
      arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
      occupancyCoefficient: 7.0
    })

    calendarPage = await openAttendancesPage()
    await calendarPage.occupancies.assertGraphIsVisible()
    await calendarPage.setFilterStartDate(LocalDate.of(2022, 3, 1))
    await calendarPage.occupancies.assertGraphHasNoData()
  })
  describe('Group selection: staff', () => {
    beforeEach(async () => {
      calendarPage = await openAttendancesPage()
      await calendarPage.selectGroup('staff')
    })

    test('The staff attendances table shows all unit staff', async () => {
      await waitUntilEqual(
        () => calendarPage.staffInAttendanceTable(),
        staff.map(staffName)
      )
    })

    test('The icon tells whether a staff member is counted in occupancy or not', async () => {
      await calendarPage.assertPositiveOccupancyCoefficientCount(1)
      await calendarPage.assertZeroOccupancyCoefficientCount(1)
    })

    test('It is not possible to create new entries', async () => {
      await calendarPage.assertNoTimeInputsVisible()
      await calendarPage.clickEditOnRow(0)
      await calendarPage.assertNoTimeInputsVisible()
      await calendarPage.clickCommitOnRow(0)
    })

    test('Sunday entries are shown in the calendar', async () => {
      await calendarPage.changeWeekToDate(mockedToday.subWeeks(1))
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 6,
        arrival: '07:00',
        departure: '15:00'
      })
    })
  })
  describe('Group selection: staff, with one attendance entry', () => {
    beforeEach(async () => {
      await insertStaffRealtimeAttendance({
        id: uuidv4(),
        employeeId: staff[1].id,
        groupId: groupId,
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
        occupancyCoefficient: 7.0
      })

      calendarPage = await openAttendancesPage()
      await calendarPage.selectGroup('staff')
      await calendarPage.assertNoTimeInputsVisible()
    })
    test('Existing entries can be edited', async () => {
      await calendarPage.clickEditOnRow(1)
      await calendarPage.assertCountTimeInputsVisible(1)
    })

    test('Editing an existing entry updates it', async () => {
      const rowIx = 1
      const nth = 0
      await calendarPage.assertArrivalDeparture({
        rowIx,
        nth,
        arrival: '07:00',
        departure: '–'
      })
      await calendarPage.clickEditOnRow(rowIx)
      await calendarPage.setNthArrivalDeparture(0, nth, '07:00', '15:00', 1)
      await calendarPage.closeInlineEditor()
      await calendarPage.assertArrivalDeparture({
        rowIx,
        nth,
        arrival: '07:00',
        departure: '15:00'
      })
    })
  })
  describe('Group selection: group', () => {
    beforeEach(async () => {
      await insertStaffRealtimeAttendance({
        id: uuidv4(),
        employeeId: staff[1].id,
        groupId: groupId,
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
        departed: mockedToday.toHelsinkiDateTime(LocalTime.of(16, 0)),
        occupancyCoefficient: 7.0
      })

      calendarPage = await openAttendancesPage()
      await calendarPage.selectGroup(groupId)
      await waitUntilEqual(
        () => calendarPage.staffInAttendanceTable(),
        [staffName(groupStaff)]
      )
    })
    test('A new entry can be added', async () => {
      const rowIx = 0
      await calendarPage.clickEditOnRow(rowIx)
      await calendarPage.setNthArrivalDeparture(0, 2, '07:00', '15:00')
      await calendarPage.closeInlineEditor()
      await calendarPage.assertArrivalDeparture({
        rowIx,
        nth: 2,
        arrival: '07:00',
        departure: '15:00'
      })
    })
    test('An overnight entry can be added', async () => {
      const rowIx = 0
      await calendarPage.clickEditOnRow(rowIx)
      await calendarPage.setNthArrivalDeparture(0, 1, '09:00', '')
      await calendarPage.setNthDeparture(0, 2, '15:00')
      await calendarPage.closeInlineEditor()
      await calendarPage.assertArrivalDeparture({
        rowIx,
        nth: 1,
        arrival: '09:00',
        departure: '→'
      })
      await calendarPage.assertArrivalDeparture({
        rowIx,
        nth: 2,
        arrival: '→',
        departure: '15:00'
      })
    })
    test('Existing entries can be deleted by entering empty values', async () => {
      const rowIx = 0
      await calendarPage.clickEditOnRow(rowIx)
      await calendarPage.setNthArrivalDeparture(0, 0, '', '')
      await calendarPage.closeInlineEditor()
      await calendarPage.assertArrivalDeparture({
        rowIx,
        nth: 0,
        arrival: '–',
        departure: '–'
      })
    })
    test('An overnight entry can be added over the week boundary', async () => {
      const rowIx = 0
      await calendarPage.clickEditOnRow(rowIx)
      await calendarPage.setNthArrivalDeparture(0, 6, '15:00', '')
      await calendarPage.changeWeekToDate(mockedToday.addWeeks(1))
      await calendarPage.assertDepartureLocked(0, 0)
      await calendarPage.setNthDeparture(0, 0, '07:00')
      await calendarPage.closeInlineEditor()
      await calendarPage.assertArrivalDeparture({
        rowIx,
        nth: 0,
        arrival: '→',
        departure: '07:00'
      })
      await calendarPage.changeWeekToDate(mockedToday)
      await calendarPage.assertArrivalDeparture({
        rowIx,
        nth: 6,
        arrival: '15:00',
        departure: '→'
      })
    })
    test('A warning is shown if open range is added with a future attendance', async () => {
      await calendarPage.clickEditOnRow(0)
      await calendarPage.setNthArrivalDeparture(0, 6, '09:00', '10:00')
      await calendarPage.setNthArrivalDeparture(0, 4, '10:00', '')
      await calendarPage.assertFormWarning()
    })
    test('A warning is shown if open range is added with a future attendance over the week boundary', async () => {
      await calendarPage.clickEditOnRow(0)
      await calendarPage.changeWeekToDate(mockedToday.addWeeks(1))
      await calendarPage.setNthArrivalDeparture(0, 0, '09:00', '10:00')
      await calendarPage.changeWeekToDate(mockedToday)
      await calendarPage.setNthArrivalDeparture(0, 4, '10:00', '')
      await calendarPage.assertFormWarning()
    })
  })
  describe('Group selected, multiple per-day attendances', () => {
    beforeEach(async () => {
      await insertStaffRealtimeAttendance({
        id: uuidv4(),
        employeeId: staff[1].id,
        groupId: groupId,
        arrived: mockedToday.addDays(1).toHelsinkiDateTime(LocalTime.of(7, 0)),
        departed: mockedToday.addDays(1).toHelsinkiDateTime(LocalTime.of(9, 0)),
        occupancyCoefficient: 2.0
      })
      await insertStaffRealtimeAttendance({
        id: uuidv4(),
        employeeId: staff[1].id,
        groupId: groupId,
        arrived: mockedToday.addDays(1).toHelsinkiDateTime(LocalTime.of(9, 0)),
        departed: mockedToday
          .addDays(1)
          .toHelsinkiDateTime(LocalTime.of(10, 0)),
        occupancyCoefficient: 2.0
      })

      calendarPage = await openAttendancesPage()
      await calendarPage.selectGroup(groupId)
      await waitUntilEqual(
        () => calendarPage.staffInAttendanceTable(),
        [staffName(groupStaff)]
      )
    })

    test('Multi-row attendances can be edited', async () => {
      await calendarPage.clickEditOnRow(0)
      await calendarPage.setNthArrivalDeparture(1, 1, '12:00', '14:00')
      await calendarPage.closeInlineEditor()
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 1,
        timeNth: 0,
        arrival: '07:00',
        departure: '09:00'
      })
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 1,
        timeNth: 1,
        arrival: '12:00',
        departure: '14:00'
      })
    })
    test('Multi-row attendance can be extended overnight', async () => {
      await calendarPage.clickEditOnRow(0)
      await calendarPage.setNthArrivalDeparture(1, 1, '14:00', '')
      await calendarPage.setNthDeparture(0, 2, '04:00')
      await calendarPage.closeInlineEditor()
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 1,
        timeNth: 1,
        arrival: '14:00',
        departure: '→'
      })
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 2,
        timeNth: 0,
        arrival: '→',
        departure: '04:00'
      })
    })
    test('An open multi-row attendance has warning when there is another after it', async () => {
      await calendarPage.clickEditOnRow(0)
      await calendarPage.setNthArrivalDeparture(0, 1, '07:00', '')
      await calendarPage.assertFormWarning()
    })
  })
  describe('Details modal', () => {
    beforeEach(async () => {
      await insertStaffRealtimeAttendance({
        id: uuidv4(),
        employeeId: staff[1].id,
        groupId: groupId,
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
        occupancyCoefficient: 7.0
      })

      calendarPage = await openAttendancesPage()
      await calendarPage.selectGroup('staff')
    })
    test('An existing entry can be edited', async () => {
      const modal = await calendarPage.openDetails(groupStaff.id, mockedToday)
      await modal.setDepartureTime(0, '15:00')
      await modal.save()

      await waitUntilEqual(() => modal.summaryPlan, '–')
      await waitUntilEqual(() => modal.summaryRealized, '07:00 – 15:00')
      await waitUntilEqual(() => modal.summaryHours, '8:00')

      await modal.close()
      await calendarPage.assertArrivalDeparture({
        rowIx: 1,
        nth: 0,
        arrival: '07:00',
        departure: '15:00'
      })
    })
    test('An existing overnight entry can be edited', async () => {
      const modal = await calendarPage.openDetails(
        groupStaff.id,
        mockedToday.addDays(1)
      )
      await modal.setDepartureTime(0, '16:00')
      await modal.save()

      await waitUntilEqual(() => modal.summaryPlan, '–')
      await waitUntilEqual(() => modal.summaryRealized, '→ – 16:00')
      await waitUntilEqual(() => modal.summaryHours, '33:00')

      await modal.close()
      await calendarPage.assertArrivalDeparture({
        rowIx: 1,
        nth: 0,
        arrival: '07:00',
        departure: '→'
      })
      await calendarPage.assertArrivalDeparture({
        rowIx: 1,
        nth: 1,
        arrival: '→',
        departure: '16:00'
      })
    })
    test('Multiple new entries can be added', async () => {
      const modal = await calendarPage.openDetails(groupStaff.id, mockedToday)
      await modal.setDepartureTime(0, '12:00')
      await modal.addNewAttendance()
      await modal.setGroup(1, groupId)
      await modal.setType(1, 'TRAINING')
      await modal.setArrivalTime(1, '12:00')
      await modal.setDepartureTime(1, '13:00')
      await modal.addNewAttendance()
      await modal.setGroup(2, groupId)
      await modal.setType(2, 'PRESENT')
      await modal.setArrivalTime(2, '13:00')
      await modal.setDepartureTime(2, '14:30')
      await modal.addNewAttendance()
      await modal.setGroup(3, groupId)
      await modal.setType(3, 'OTHER_WORK')
      await modal.setArrivalTime(3, '14:30')
      await modal.setDepartureTime(3, '15:00')
      await modal.save()

      await waitUntilEqual(() => modal.summaryPlan, '–')
      await waitUntilEqual(() => modal.summaryRealized, '07:00 – 15:00')
      await waitUntilEqual(() => modal.summaryHours, '8:00')

      await modal.close()
      await calendarPage.assertArrivalDeparture({
        rowIx: 1,
        nth: 0,
        timeNth: 0,
        arrival: '07:00',
        departure: '12:00'
      })
      await calendarPage.assertArrivalDeparture({
        rowIx: 1,
        nth: 0,
        timeNth: 1,
        arrival: '13:00',
        departure: '14:30'
      })
    })
  })
  describe('Entries to multiple groups', () => {
    beforeEach(async () => {
      await insertStaffRealtimeAttendance({
        id: uuidv4(),
        employeeId: staff[1].id,
        groupId: groupId,
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(8, 0)),
        occupancyCoefficient: 7.0
      })

      calendarPage = await openAttendancesPage()
    })
    test('Inline editor does not overwrite entries to other groups', async () => {
      await calendarPage.selectGroup(groupId)
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 0,
        arrival: '08:00',
        departure: '–'
      })
      await calendarPage.clickEditOnRow(0)
      await calendarPage.setNthArrivalDeparture(0, 0, '07:00', '15:00')
      await calendarPage.closeInlineEditor()
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 0,
        arrival: '07:00',
        departure: '15:00'
      })
      await calendarPage.selectGroup(groupId2)
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 0,
        arrival: '–',
        departure: '–'
      })
      await calendarPage.clickEditOnRow(0)
      await calendarPage.setNthArrivalDeparture(0, 0, '15:00', '16:15')
      await calendarPage.closeInlineEditor()
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 0,
        arrival: '15:00',
        departure: '16:15'
      })
      await calendarPage.selectGroup(groupId)
      await calendarPage.assertArrivalDeparture({
        rowIx: 0,
        nth: 0,
        arrival: '07:00',
        departure: '15:00'
      })
    })
  })
})

function staffName(employeeDetail: EmployeeDetail): string {
  return `${employeeDetail.lastName} ${employeeDetail.firstName}`
}
