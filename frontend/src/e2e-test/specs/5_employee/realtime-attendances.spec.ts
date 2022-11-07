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
import {
  UnitAttendancesSection,
  UnitCalendarPage,
  UnitStaffAttendancesTable
} from '../../pages/employee/units/unit-attendances-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
let attendancesSection: UnitAttendancesSection
let calendarPage: UnitCalendarPage
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
    occupancyCoefficient: 7.0,
    type: 'PRESENT'
  })

  page = await Page.open({
    viewport: { width: 1440, height: 720 },
    mockedTime: mockedToday
      .toHelsinkiDateTime(LocalTime.of(12, 0))
      .toSystemTzDate()
  })
  await employeeLogin(page, unitSupervisor)
})

const openAttendancesSection = async (): Promise<UnitAttendancesSection> => {
  unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(daycare.id)
  calendarPage = await unitPage.openCalendarPage()
  return calendarPage.attendancesSection
}

describe('Realtime staff attendances', () => {
  test('Occupancy graph', async () => {
    await insertStaffRealtimeAttendance({
      id: uuidv4(),
      employeeId: staff[1].id,
      groupId: groupId,
      arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
      departed: null,
      occupancyCoefficient: 7.0,
      type: 'PRESENT'
    })

    attendancesSection = await openAttendancesSection()
    await attendancesSection.occupancies.assertGraphIsVisible()
    await attendancesSection.setFilterStartDate(LocalDate.of(2022, 3, 1))
    await attendancesSection.occupancies.assertGraphHasNoData()
  })
  describe('Group selection: staff', () => {
    let staffAttendances: UnitStaffAttendancesTable

    beforeEach(async () => {
      attendancesSection = await openAttendancesSection()
      await attendancesSection.selectGroup('staff')
      staffAttendances = attendancesSection.staffAttendances
    })

    test('The staff attendances table shows all unit staff', async () => {
      await waitUntilEqual(
        () => staffAttendances.allNames,
        staff.map(staffName)
      )
    })

    test('The icon tells whether a staff member is counted in occupancy or not', async () => {
      await staffAttendances.assertPositiveOccupancyCoefficientCount(1)
      await staffAttendances.assertZeroOccupancyCoefficientCount(1)
    })

    test('Sunday entries are shown in the calendar', async () => {
      await calendarPage.changeWeekToDate(mockedToday.subWeeks(1))
      await staffAttendances.assertTableRow({
        rowIx: 0,
        nth: 6,
        arrival: '07:00',
        departure: '15:00'
      })
    })
  })

  describe('Planned attendances', () => {
    const plannedStartTime = mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0))
    const plannedEndTime = mockedToday.toHelsinkiDateTime(LocalTime.of(15, 0))
    let staffAttendances: UnitStaffAttendancesTable

    beforeEach(async () => {
      await Fixture.realtimeStaffAttendance()
        .with({
          id: uuidv4(),
          employeeId: staff[1].id,
          groupId: groupId,
          arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
          departed: null,
          occupancyCoefficient: 7.0,
          type: 'PRESENT'
        })
        .save()

      await Fixture.staffAttendancePlan()
        .with({
          id: uuidv4(),
          employeeId: staff[1].id,
          startTime: plannedStartTime,
          endTime: plannedEndTime
        })
        .save()

      attendancesSection = await openAttendancesSection()
      await attendancesSection.selectGroup('staff')
      staffAttendances = attendancesSection.staffAttendances
    })
    test('Plan is shown on week view and day modal', async () => {
      await staffAttendances.assertPlannedAttendance(
        plannedStartTime.toLocalDate(),
        1,
        '07:00',
        '15:00'
      )

      const modal = await staffAttendances.openDetails(1, mockedToday)
      await waitUntilEqual(() => modal.summary(), {
        plan: '07:00 – 15:00',
        realized: '07:00 –',
        hours: '5:00 (-3:00)'
      })
    })
  })
  describe('Details modal', () => {
    let staffAttendances: UnitStaffAttendancesTable

    beforeEach(async () => {
      await insertStaffRealtimeAttendance({
        id: uuidv4(),
        employeeId: staff[1].id,
        groupId: groupId,
        arrived: mockedToday.toHelsinkiDateTime(LocalTime.of(7, 0)),
        departed: null,
        occupancyCoefficient: 7.0,
        type: 'PRESENT'
      })

      attendancesSection = await openAttendancesSection()
      await attendancesSection.selectGroup('staff')
      staffAttendances = attendancesSection.staffAttendances
    })
    test('An existing entry can be edited', async () => {
      const modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setDepartureTime(0, '15:00')
      await modal.save()

      await waitUntilEqual(() => modal.summary(), {
        plan: '–',
        realized: '07:00 – 15:00',
        hours: '8:00'
      })

      await modal.close()
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 0,
        arrival: '07:00',
        departure: '15:00'
      })
    })
    test('An existing overnight entry can be edited', async () => {
      const modal = await staffAttendances.openDetails(
        1,
        mockedToday.addDays(1)
      )
      await modal.setDepartureTime(0, '16:00')
      await modal.save()

      await waitUntilEqual(() => modal.summary(), {
        plan: '–',
        realized: '→ – 16:00',
        hours: '33:00'
      })

      await modal.close()
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 0,
        arrival: '07:00',
        departure: '→'
      })
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 1,
        arrival: '→',
        departure: '16:00'
      })
    })
    test('If departure is earlier than arrival, departure is on the next day', async () => {
      const modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setDepartureTime(0, '06:00')
      await modal.save()

      await waitUntilEqual(() => modal.summary(), {
        plan: '–',
        realized: '07:00 – →',
        hours: '23:00'
      })

      await modal.close()
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 0,
        arrival: '07:00',
        departure: '→'
      })
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 1,
        arrival: '→',
        departure: '06:00'
      })
    })
    test('Multiple new entries can be added', async () => {
      const modal = await staffAttendances.openDetails(1, mockedToday)
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

      await waitUntilEqual(() => modal.summary(), {
        plan: '–',
        realized: '07:00 – 15:00',
        hours: '8:00'
      })

      await modal.close()
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 0,
        timeNth: 0,
        arrival: '07:00',
        departure: '12:00'
      })
      await staffAttendances.assertTableRow({
        rowIx: 1,
        nth: 0,
        timeNth: 1,
        arrival: '13:00',
        departure: '14:30'
      })
    })
    test('Gaps in attendances are warned about', async () => {
      const modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setDepartureTime(0, '12:00')
      await modal.addNewAttendance()
      await modal.setGroup(1, groupId)
      await modal.setType(1, 'TRAINING')
      await modal.setArrivalTime(1, '12:30')
      await modal.setDepartureTime(1, '13:00')
      await modal.addNewAttendance()
      await modal.setGroup(2, groupId)
      await modal.setType(2, 'PRESENT')
      await modal.setArrivalTime(2, '13:20')
      await modal.setDepartureTime(2, '14:30')

      await waitUntilEqual(
        () => modal.gapWarning(1),
        'Kirjaus puuttuu välillä 12:00 – 12:30'
      )
      await waitUntilEqual(
        () => modal.gapWarning(2),
        'Kirjaus puuttuu välillä 13:00 – 13:20'
      )
    })
  })
  describe('Staff count sums in the table', () => {
    let staffAttendances: UnitStaffAttendancesTable

    beforeEach(async () => {
      const times: [number, number, number][] = [
        [0, 12, 15],
        [4, 9, 10],
        [5, 10, 13]
      ]
      for (const [day, arrivalHour, departureHour] of times) {
        await insertStaffRealtimeAttendance({
          id: uuidv4(),
          employeeId: staff[1].id,
          groupId: groupId,
          arrived: mockedToday
            .addDays(day)
            .toHelsinkiDateTime(LocalTime.of(arrivalHour, 0)),
          departed: mockedToday
            .addDays(day)
            .toHelsinkiDateTime(LocalTime.of(departureHour, 0)),
          occupancyCoefficient: 7.0,
          type: 'PRESENT'
        })
      }

      attendancesSection = await openAttendancesSection()
      staffAttendances = attendancesSection.staffAttendances
    })

    test('Total staff counts', async () => {
      await attendancesSection.selectGroup(groupId2)
      await waitUntilEqual(() => staffAttendances.personCountSum(0), '– hlö')

      await attendancesSection.selectGroup(groupId)
      await waitUntilEqual(() => staffAttendances.personCountSum(0), '1 hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(1), '– hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(2), '– hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(3), '– hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(4), '1 hlö')
      await waitUntilEqual(() => staffAttendances.personCountSum(5), '1 hlö')
    })
  })
  describe('External staff members', () => {
    let staffAttendances: UnitStaffAttendancesTable

    beforeEach(async () => {
      attendancesSection = await openAttendancesSection()
      staffAttendances = attendancesSection.staffAttendances
    })

    test('Can an add and modify an external', async () => {
      const addPersonModal = await attendancesSection.clickAddPersonButton()
      await addPersonModal.setArrivalDate(mockedToday.format())
      await addPersonModal.setArrivalTime('11:00')
      await addPersonModal.typeName('Sijainen Saija')
      await addPersonModal.selectGroup(groupId)
      await addPersonModal.save()

      await staffAttendances.assertTableRow({
        rowIx: 1,
        name: 'Sijainen Saija',
        arrival: '11:00',
        departure: '–'
      })

      let modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.setDepartureTime(0, '13:00')
      await modal.save()
      await modal.close()

      await waitUntilEqual(() => staffAttendances.rowCount, 2)
      await staffAttendances.assertTableRow({
        rowIx: 1,
        name: 'Sijainen Saija',
        arrival: '11:00',
        departure: '13:00'
      })

      modal = await staffAttendances.openDetails(1, mockedToday)
      await modal.removeAttendance(0)
      await modal.save()
      await modal.close()

      await waitUntilEqual(() => staffAttendances.rowCount, 1)
    })

    test('Can an add new external with arrival and departure times', async () => {
      const addPersonModal = await attendancesSection.clickAddPersonButton()
      await addPersonModal.setArrivalDate(mockedToday.format())
      await addPersonModal.setArrivalTime('11:00')
      await addPersonModal.setDepartureTime('16:00')
      await addPersonModal.typeName('Sijainen Saija')
      await addPersonModal.selectGroup(groupId)
      await addPersonModal.save()

      await staffAttendances.assertTableRow({
        rowIx: 1,
        name: 'Sijainen Saija',
        arrival: '11:00',
        departure: '16:00'
      })
    })

    test('Cannot add new external with invalid data', async () => {
      const addPersonModal = await attendancesSection.clickAddPersonButton()
      await addPersonModal.setArrivalDate(mockedToday.format())
      await addPersonModal.setArrivalTime('9:99')
      await addPersonModal.typeName('S')
      await addPersonModal.selectGroup(groupId)
      await addPersonModal.save()

      await addPersonModal.timeErrorVisible()
      await addPersonModal.nameErrorVisible()
    })
  })
})

function staffName(employeeDetail: EmployeeDetail): string {
  return `${employeeDetail.lastName} ${employeeDetail.firstName}`
}
