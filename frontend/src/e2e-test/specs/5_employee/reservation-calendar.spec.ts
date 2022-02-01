// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
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
  ReservationModal,
  UnitCalendarPage
} from '../../pages/employee/units/unit-calendar-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
let calendarPage: UnitCalendarPage
let reservationModal: ReservationModal
let child1Fixture: Child
let child1DaycarePlacementId: UUID
let daycare: Daycare
let unitSupervisor: EmployeeDetail

const placementStartDate = LocalDate.today().subWeeks(4)
const placementEndDate = LocalDate.today().addWeeks(4)
const groupId: UUID = uuidv4()

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()

  daycare = daycare2Fixture

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
})

const loadUnitCalendarPage = async (): Promise<UnitCalendarPage> => {
  unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(daycare.id)
  return await unitPage.openCalendarPage()
}

describe('Unit group calendar', () => {
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, unitSupervisor)
  })

  test('Employee sees row for child', async () => {
    calendarPage = await loadUnitCalendarPage()
    await calendarPage.selectMode('week')
    await waitUntilEqual(() => calendarPage.childRowCount(child1Fixture.id), 1)
  })

  test('Employee can add reservation', async () => {
    calendarPage = await loadUnitCalendarPage()
    await calendarPage.selectMode('week')
    reservationModal = await calendarPage.openReservationModal(child1Fixture.id)
    await reservationModal.addReservation()
  })
})

describe('Unit group calendar for shift care unit', () => {
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, unitSupervisor)
  })

  test('Employee can add two reservations for day and sees two rows', async () => {
    calendarPage = await loadUnitCalendarPage()

    await calendarPage.selectMode('week')

    reservationModal = await calendarPage.openReservationModal(child1Fixture.id)
    await reservationModal.selectRepetitionType('IRREGULAR')

    const startDate = LocalDate.today().startOfWeek()
    await reservationModal.setStartDate(startDate.format())
    await reservationModal.setEndDate(startDate.format())
    await reservationModal.setStartTime('10:00', 0)
    await reservationModal.setEndTime('16:00', 0)

    await reservationModal.addNewTimeRow(0)

    await reservationModal.setStartTime('20:00', 1)
    await reservationModal.setEndTime('08:00', 1)

    await reservationModal.save()

    await waitUntilEqual(() => calendarPage.childRowCount(child1Fixture.id), 2)
  })

  test('Employee sees attendances along reservations', async () => {
    calendarPage = await loadUnitCalendarPage()
    await calendarPage.selectMode('week')

    reservationModal = await calendarPage.openReservationModal(child1Fixture.id)
    await reservationModal.selectRepetitionType('IRREGULAR')

    const startDate = LocalDate.today().startOfWeek()

    await Fixture.childAttendances()
      .with({
        childId: child1Fixture.id,
        unitId: daycare2Fixture.id,
        arrived: new Date(`${startDate.formatIso()}T08:30Z`),
        departed: new Date(`${startDate.formatIso()}T13:30Z`)
      })
      .save()

    await Fixture.childAttendances()
      .with({
        childId: child1Fixture.id,
        unitId: daycare2Fixture.id,
        arrived: new Date(`${startDate.formatIso()}T18:15Z`),
        departed: new Date(`${startDate.addDays(1).formatIso()}T05:30Z`)
      })
      .save()

    await reservationModal.setStartDate(startDate.format())
    await reservationModal.setEndDate(startDate.format())
    await reservationModal.setStartTime('10:00', 0)
    await reservationModal.setEndTime('16:00', 0)

    await reservationModal.addNewTimeRow(0)

    await reservationModal.setStartTime('20:00', 1)
    await reservationModal.setEndTime('08:00', 1)

    await reservationModal.save()

    await waitUntilEqual(() => calendarPage.childRowCount(child1Fixture.id), 2)

    const expectedReservations = [
      ['10:00', '16:00', startDate],
      ['20:00', '00:00', startDate],
      ['00:00', '08:00', startDate.addDays(1)]
    ]

    await Promise.all(
      expectedReservations.map((res, rowIndex) => {
        return [0, 1].map(
          (colIndex) => () =>
            waitUntilEqual(
              () =>
                calendarPage.reservationCell(
                  res[2] as LocalDate,
                  rowIndex % 2,
                  colIndex
                ),
              res[colIndex]
            )
        )
      })
    )

    const expectedAttendances = [
      ['10:30', '15:30', startDate],
      ['20:15', '00:00', startDate],
      ['00:00', '07:30', startDate.addDays(1)]
    ]

    await Promise.all(
      expectedAttendances.map((att, rowIndex) => {
        return [0, 1].map(
          (colIndex) => () =>
            waitUntilEqual(
              () =>
                calendarPage.attendanceCell(
                  att[2] as LocalDate,
                  rowIndex % 2,
                  colIndex
                ),
              att[colIndex]
            )
        )
      })
    )
  })
})
