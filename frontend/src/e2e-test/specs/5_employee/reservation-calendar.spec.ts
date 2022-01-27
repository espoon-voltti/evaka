// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { Fixture, uuidv4 } from '../../dev-api/fixtures'
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
  daycare = fixtures.daycareFixture

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

  test('Employee can add two reservations for day and sees two rows', async () => {
    calendarPage = await loadUnitCalendarPage()
    await calendarPage.selectMode('week')

    reservationModal = await calendarPage.openReservationModal(child1Fixture.id)
    await reservationModal.addReservation()

    reservationModal = await calendarPage.openReservationModal(child1Fixture.id)
    await reservationModal.addOvernightReservation()

    await waitUntilEqual(() => calendarPage.childRowCount(child1Fixture.id), 2)
  })
})
