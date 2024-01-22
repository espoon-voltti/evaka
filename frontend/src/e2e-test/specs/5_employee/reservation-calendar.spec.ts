// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  careArea2Fixture,
  daycare2Fixture,
  daycareFixture,
  Fixture,
  fullDayTimeRange,
  uuidv4
} from '../../dev-api/fixtures'
import { Child, Daycare, EmployeeDetail } from '../../dev-api/types'
import { UnitPage } from '../../pages/employee/units/unit'
import {
  ReservationModal,
  UnitAttendancesSection,
  UnitCalendarPage
} from '../../pages/employee/units/unit-attendances-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
let calendarPage: UnitCalendarPage
let attendancesSection: UnitAttendancesSection
let reservationModal: ReservationModal
let child1Fixture: Child
let child1DaycarePlacementId: UUID
let daycare: Daycare
let unitSupervisor: EmployeeDetail

const mockedToday = LocalDate.of(2023, 2, 15) // wed
const placementStartDate = mockedToday.subWeeks(4)
const placementEndDate = mockedToday.addWeeks(8)
const backupCareStartDate = mockedToday.startOfWeek().addWeeks(2)
const backupCareEndDate = backupCareStartDate.addDays(8)
const groupId: UUID = uuidv4()

beforeEach(async () => {
  await resetDatabase()
})

const insertTestDataAndLogin = async ({
  roundTheClockDaycare = true
}: {
  roundTheClockDaycare?: boolean
} = {}) => {
  const fixtures = await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  const daycareBuilder = Fixture.daycare()
    .with(daycare2Fixture)
    .careArea(careArea)
  if (!roundTheClockDaycare) {
    daycareBuilder.with({
      operationDays: [1, 2, 3, 4, 5],
      operationTimes: [
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        null,
        null
      ],
      roundTheClock: false
    })
  }
  await daycareBuilder.save()
  daycare = daycareBuilder.data

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

  const groupId2 = uuidv4()
  await Fixture.daycareGroup()
    .with({
      id: groupId2,
      daycareId: daycareFixture.id,
      name: 'Testailijat Toisessa'
    })
    .save()

  child1Fixture = fixtures.familyWithTwoGuardians.children[0]
  child1DaycarePlacementId = uuidv4()
  await Fixture.placement()
    .with({
      id: child1DaycarePlacementId,
      childId: child1Fixture.id,
      unitId: daycare.id,
      startDate: placementStartDate,
      endDate: placementEndDate
    })
    .save()

  await Fixture.backupCare()
    .with({
      id: uuidv4(),
      childId: child1Fixture.id,
      unitId: daycareFixture.id,
      groupId: groupId2,
      period: {
        start: backupCareStartDate,
        end: backupCareEndDate
      }
    })
    .save()

  await Fixture.groupPlacement()
    .with({
      daycareGroupId: groupId,
      daycarePlacementId: child1DaycarePlacementId,
      startDate: placementStartDate,
      endDate: placementEndDate
    })
    .save()

  page = await Page.open({
    mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await employeeLogin(page, unitSupervisor)
}

const loadUnitAttendancesSection =
  async (): Promise<UnitAttendancesSection> => {
    unitPage = new UnitPage(page)
    await unitPage.navigateToUnit(daycare.id)
    calendarPage = await unitPage.openCalendarPage()
    return calendarPage.attendancesSection
  }

describe('Unit group calendar', () => {
  test('Employee sees row for child', async () => {
    await insertTestDataAndLogin()
    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')
    await waitUntilEqual(
      () => childReservations.childReservationRows(child1Fixture.id).count(),
      1
    )
  })

  test('Child in backup care in other group for part of the week is shown', async () => {
    await insertTestDataAndLogin()
    const groupId3 = uuidv4()
    const backupCareSameUnitStartDate = backupCareStartDate.addWeeks(2)
    const backupCareSameUnitEndDate = backupCareSameUnitStartDate.addDays(3)
    await Fixture.daycareGroup()
      .with({
        id: groupId3,
        daycareId: daycare.id,
        name: 'Varasijoitusryhmä samassa'
      })
      .save()
    await Fixture.backupCare()
      .with({
        id: uuidv4(),
        childId: child1Fixture.id,
        unitId: daycare.id,
        groupId: groupId3,
        period: {
          start: backupCareSameUnitStartDate,
          end: backupCareSameUnitEndDate
        }
      })
      .save()

    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')
    await calendarPage.changeWeekToDate(backupCareSameUnitStartDate)
    await waitUntilEqual(
      () => childReservations.childInOtherGroup(child1Fixture.id).count(),
      4
    )
  })

  test('Reservations are shown in the backup group calendar when backup is within the same unit', async () => {
    await insertTestDataAndLogin()
    const groupId3 = uuidv4()
    const backupCareSameUnitStartDate = backupCareStartDate.addWeeks(2)
    const backupCareSameUnitEndDate = backupCareSameUnitStartDate.addDays(3)
    await Fixture.daycareGroup()
      .with({
        id: groupId3,
        daycareId: daycare.id,
        name: 'Varasijoitusryhmä samassa'
      })
      .save()
    await Fixture.backupCare()
      .with({
        id: uuidv4(),
        childId: child1Fixture.id,
        unitId: daycare.id,
        groupId: groupId3,
        period: {
          start: backupCareSameUnitStartDate,
          end: backupCareSameUnitEndDate
        }
      })
      .save()

    const unitAttendancesSection = await loadUnitAttendancesSection()
    const childReservations = unitAttendancesSection.childReservations

    await calendarPage.selectMode('week')
    reservationModal = await childReservations.openReservationModal(
      child1Fixture.id
    )
    await reservationModal.selectRepetitionType('DAILY')

    const startDate = backupCareSameUnitStartDate.subWeeks(1)
    await reservationModal.startDate.fill(startDate.format())
    await reservationModal.endDate.fill(
      backupCareSameUnitStartDate.addWeeks(1).format()
    )
    await reservationModal.setStartTime('08:00', 0)
    await reservationModal.setEndTime('16:00', 0)
    await reservationModal.save()

    await calendarPage.changeWeekToDate(backupCareSameUnitStartDate)
    await unitAttendancesSection.selectGroup(groupId3)
    await waitUntilEqual(
      () => childReservations.childInOtherUnit(child1Fixture.id).count(),
      0
    )
  })

  test('Child in backup care for the entire week is shown', async () => {
    await insertTestDataAndLogin()
    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')
    await calendarPage.changeWeekToDate(backupCareStartDate)
    await waitUntilEqual(
      () => childReservations.childInOtherUnit(child1Fixture.id).count(),
      7
    )
  })

  test('Child in backup care during the week is shown', async () => {
    await insertTestDataAndLogin()
    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')
    await calendarPage.changeWeekToDate(backupCareEndDate)
    await waitUntilEqual(
      () => childReservations.childInOtherUnit(child1Fixture.id).count(),
      2
    )
  })

  test('Missing holiday reservations are shown', async () => {
    await insertTestDataAndLogin()
    const holidayPeriodStart = LocalDate.of(2023, 3, 13)
    const holidayPeriodEnd = LocalDate.of(2023, 3, 19)
    await Fixture.holidayPeriod()
      .with({
        period: new FiniteDateRange(holidayPeriodStart, holidayPeriodEnd),
        reservationDeadline: LocalDate.of(2023, 3, 1)
      })
      .save()

    await Fixture.dailyServiceTime(child1Fixture.id)
      .with({
        validityPeriod: new DateRange(holidayPeriodStart.subWeeks(1), null),
        type: 'REGULAR',
        regularTimes: {
          start: LocalTime.of(8, 0),
          end: LocalTime.of(16, 0)
        }
      })
      .save()

    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: child1Fixture.id,
      date: holidayPeriodStart.subDays(1),
      reservation: {
        start: LocalTime.of(11, 0),
        end: LocalTime.of(13, 0)
      },
      secondReservation: null
    }).save()

    // Reservation on the second day
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: child1Fixture.id,
      date: holidayPeriodStart.addDays(1),
      reservation: {
        start: LocalTime.of(8, 0),
        end: LocalTime.of(14, 0)
      },
      secondReservation: null
    }).save()
    // Absence on the third day
    await Fixture.attendanceReservation({
      type: 'ABSENT',
      childId: child1Fixture.id,
      date: holidayPeriodStart.addDays(2)
    }).save()

    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')
    await calendarPage.changeWeekToDate(holidayPeriodStart)

    await waitUntilEqual(
      () =>
        childReservations.missingHolidayReservations(child1Fixture.id).count(),
      5
    )
  })

  test('Missing holiday reservations are shown if reservation deadline has passed', async () => {
    await insertTestDataAndLogin()
    const holidayPeriodStart = LocalDate.of(2023, 3, 13)
    const holidayPeriodEnd = LocalDate.of(2023, 3, 19)
    await Fixture.holidayPeriod()
      .with({
        period: new FiniteDateRange(holidayPeriodStart, holidayPeriodEnd),
        reservationDeadline: mockedToday.subDays(1)
      })
      .save()

    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')
    await calendarPage.changeWeekToDate(holidayPeriodStart)

    await waitUntilEqual(
      () =>
        childReservations.missingHolidayReservations(child1Fixture.id).count(),
      7
    )
  })

  test('Tooltip for attendance reservation is shown', async () => {
    await insertTestDataAndLogin()
    const holidayPeriodStart = LocalDate.of(2023, 3, 13)
    const holidayPeriodEnd = LocalDate.of(2023, 3, 19)
    await Fixture.holidayPeriod()
      .with({
        period: new FiniteDateRange(holidayPeriodStart, holidayPeriodEnd),
        reservationDeadline: LocalDate.of(2023, 3, 1)
      })
      .save()

    const dailyServiceTimeStart = holidayPeriodStart.subDays(5)
    await Fixture.dailyServiceTime(child1Fixture.id)
      .with({
        validityPeriod: new DateRange(dailyServiceTimeStart, null),
        type: 'REGULAR',
        regularTimes: {
          start: LocalTime.of(8, 0),
          end: LocalTime.of(16, 0)
        }
      })
      .save()

    const attendanceReservationBeforeHolidayDate = holidayPeriodStart.subDays(1)
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: child1Fixture.id,
      date: attendanceReservationBeforeHolidayDate,
      reservation: {
        start: LocalTime.of(11, 0),
        end: LocalTime.of(13, 0)
      },
      secondReservation: null
    }).save()

    const attendanceReservationDuringHolidayDate = holidayPeriodStart.addDays(1)

    // Reservation on the second day
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: child1Fixture.id,
      date: attendanceReservationDuringHolidayDate,
      reservation: {
        start: LocalTime.of(8, 0),
        end: LocalTime.of(14, 0)
      },
      secondReservation: null
    }).save()
    // Absence on the third day
    await Fixture.attendanceReservation({
      type: 'ABSENT',
      childId: child1Fixture.id,
      date: holidayPeriodStart.addDays(2)
    }).save()

    await loadUnitAttendancesSection()

    await calendarPage.selectMode('week')
    await calendarPage.changeWeekToDate(holidayPeriodStart)
    await calendarPage.selectMode('month')

    await calendarPage.assertDayTooltip(
      child1Fixture.id,
      dailyServiceTimeStart,
      ['Sopimusaika 08:00 - 16:00']
    )

    const todayStr = LocalDate.todayInHelsinkiTz().format('dd.MM.yyyy')

    await calendarPage.assertDayTooltip(
      child1Fixture.id,
      attendanceReservationBeforeHolidayDate,
      [
        'Varaus 11:00 - 13:00',
        `${todayStr} Henkilökunta`,
        'Sopimusaika 08:00 - 16:00'
      ]
    )

    await calendarPage.assertDayTooltip(
      child1Fixture.id,
      attendanceReservationDuringHolidayDate,
      [
        'Varaus 08:00 - 14:00',
        `${todayStr} Henkilökunta`,
        'Sopimusaika 08:00 - 16:00'
      ]
    )

    await calendarPage.assertDayTooltip(child1Fixture.id, holidayPeriodStart, [
      'Huoltaja ei ole vahvistanut loma-ajan varausta',
      'Sopimusaika 08:00 - 16:00'
    ])

    await calendarPage.assertDayTooltip(child1Fixture.id, backupCareEndDate, [
      'Lapsi varasijoitettuna muualla'
    ])

    await calendarPage.nextWeek.click()
    await calendarPage.assertDayTooltip(
      child1Fixture.id,
      placementEndDate.addDays(1),
      []
    )
  })

  test('Employee can add reservation', async () => {
    await insertTestDataAndLogin()
    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')
    reservationModal = await childReservations.openReservationModal(
      child1Fixture.id
    )
    await reservationModal.addReservation(mockedToday)
  })

  test('Employee can change between calendar modes', async () => {
    await insertTestDataAndLogin()
    attendancesSection = await loadUnitAttendancesSection()
    await calendarPage.selectMode('week')
    await calendarPage.assertMode('week')
    await calendarPage.waitForWeekLoaded()

    await calendarPage.selectMode('month')
    await calendarPage.assertMode('month')
    await attendancesSection.waitUntilLoaded()
  })

  test('Employee can see the correct date range based on mode', async () => {
    await insertTestDataAndLogin()
    attendancesSection = await loadUnitAttendancesSection()
    await calendarPage.selectMode('week')
    await calendarPage.assertDateRange(
      new FiniteDateRange(
        mockedToday.startOfWeek(),
        mockedToday.startOfWeek().addDays(6)
      )
    )

    await calendarPage.selectMode('month')
    await calendarPage.assertDateRange(
      new FiniteDateRange(
        mockedToday.startOfMonth(),
        mockedToday.lastDayOfMonth()
      )
    )
  })

  test('Employee sees all attendances for a child during a day', async () => {
    await insertTestDataAndLogin()
    const attendances = [
      [LocalTime.of(8, 15), LocalTime.of(9, 30)],
      [LocalTime.of(10, 30), LocalTime.of(11, 45)],
      [LocalTime.of(13, 0), LocalTime.of(14, 30)],
      [LocalTime.of(15, 0), LocalTime.of(16, 0)]
    ]
    await Promise.all(
      attendances.map(async ([arrival, departure]) => {
        await Fixture.childAttendance()
          .with({
            childId: child1Fixture.id,
            unitId: daycare2Fixture.id,
            arrived: HelsinkiDateTime.fromLocal(mockedToday, arrival),
            departed: HelsinkiDateTime.fromLocal(mockedToday, departure)
          })
          .save()
      })
    )

    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')

    await waitUntilEqual(
      () => childReservations.childAttendanceRows(child1Fixture.id).count(),
      attendances.length
    )
  })

  test('Employee can add absence records', async () => {
    await insertTestDataAndLogin()
    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')
    reservationModal = await childReservations.openReservationModal(
      child1Fixture.id
    )
    await reservationModal.addAbsence(mockedToday)
    await waitUntilEqual(
      () => childReservations.childAbsenceCells(child1Fixture.id).count(),
      1
    )
  })
})

describe('Unit group calendar for shift care unit', () => {
  test('Employee can add two reservations for day and sees two rows', async () => {
    await insertTestDataAndLogin()
    const childReservations = (await loadUnitAttendancesSection())
      .childReservations

    await calendarPage.selectMode('week')

    reservationModal = await childReservations.openReservationModal(
      child1Fixture.id
    )
    await reservationModal.selectRepetitionType('IRREGULAR')

    const startDate = mockedToday
    await reservationModal.startDate.fill(startDate.format())
    await reservationModal.endDate.fill(startDate.format())
    await reservationModal.setStartTime('00:00', 0)
    await reservationModal.setEndTime('12:00', 0)

    await reservationModal.addNewTimeRow(0)

    await reservationModal.setStartTime('20:00', 1)
    await reservationModal.setEndTime('23:59', 1)
    await reservationModal.save()

    await waitUntilEqual(
      () => childReservations.childReservationRows(child1Fixture.id).count(),
      2
    )
  })

  test('Irregular reservation over weekend in normal unit', async () => {
    await insertTestDataAndLogin({
      roundTheClockDaycare: false
    })
    const childReservations = (await loadUnitAttendancesSection())
      .childReservations

    await calendarPage.selectMode('week')

    reservationModal = await childReservations.openReservationModal(
      child1Fixture.id
    )
    await reservationModal.selectRepetitionType('IRREGULAR')

    const nextFriday = mockedToday.addDays(2)
    const nextWeekMonday = mockedToday.addDays(5)
    await reservationModal.startDate.fill(nextFriday.format())
    await reservationModal.endDate.fill(nextWeekMonday.format())
    await reservationModal.setStartTime('09:00', 0)
    await reservationModal.setEndTime('17:00', 0)
    await reservationModal.setStartTime('08:00', 1)
    await reservationModal.setEndTime('16:00', 1)
    await reservationModal.save()

    await childReservations
      .reservationCells(child1Fixture.id, nextFriday)
      .nth(0)
      .assertTextEquals('09:00\n17:00')
    await calendarPage.nextWeek.click()
    await childReservations
      .reservationCells(child1Fixture.id, nextWeekMonday)
      .nth(0)
      .assertTextEquals('08:00\n16:00')
  })

  test('Employee sees attendances along reservations', async () => {
    await insertTestDataAndLogin()
    const childReservations = (await loadUnitAttendancesSection())
      .childReservations
    await calendarPage.selectMode('week')

    reservationModal = await childReservations.openReservationModal(
      child1Fixture.id
    )
    await reservationModal.selectRepetitionType('IRREGULAR')

    const startDate = mockedToday
    const arrived = HelsinkiDateTime.fromLocal(startDate, LocalTime.of(8, 30))
    const departed = HelsinkiDateTime.fromLocal(startDate, LocalTime.of(13, 30))

    await Fixture.childAttendance()
      .with({
        childId: child1Fixture.id,
        unitId: daycare2Fixture.id,
        arrived: arrived,
        departed: departed
      })
      .save()

    const arrived2 = HelsinkiDateTime.fromLocal(startDate, LocalTime.of(18, 15))
    const departed2 = HelsinkiDateTime.fromLocal(
      startDate.addDays(1),
      LocalTime.of(5, 30)
    )

    await Fixture.childAttendance()
      .with({
        childId: child1Fixture.id,
        unitId: daycare2Fixture.id,
        arrived: arrived2,
        departed: departed2
      })
      .save()

    await reservationModal.startDate.fill(startDate.format())
    await reservationModal.endDate.fill(startDate.format())
    await reservationModal.setStartTime('00:00', 0)
    await reservationModal.setEndTime('12:00', 0)

    await reservationModal.addNewTimeRow(0)

    await reservationModal.setStartTime('20:00', 1)
    await reservationModal.setEndTime('23:59', 1)

    await reservationModal.save()

    await waitUntilEqual(
      () => childReservations.childReservationRows(child1Fixture.id).count(),
      2
    )

    await waitUntilEqual(
      () => childReservations.getReservation(startDate, 0),
      ['00:00', '12:00']
    )
    await waitUntilEqual(
      () => childReservations.getReservation(startDate, 1),
      ['20:00', '23:59']
    )

    await waitUntilEqual(
      () => childReservations.getAttendance(startDate, 0),
      [arrived.toLocalTime().format(), departed.toLocalTime().format()]
    )
    await waitUntilEqual(
      () => childReservations.getAttendance(startDate, 1),
      [arrived2.toLocalTime().format(), '23:59']
    )
    await waitUntilEqual(
      () => childReservations.getAttendance(startDate.addDays(1), 0),
      ['00:00', departed2.toLocalTime().format()]
    )
    await waitUntilEqual(
      () => childReservations.getAttendance(startDate.addDays(1), 1),
      ['-', '-']
    )
  })
})
