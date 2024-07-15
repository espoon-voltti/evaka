// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'
import { UUID } from 'lib-common/types'

import { Fixture, preschoolTerm2023 } from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import { UnitPage } from '../../pages/employee/units/unit'
import { UnitWeekCalendarPage } from '../../pages/employee/units/unit-week-calendar-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let daycareId: UUID
let childId: UUID
let placementId: UUID
let unitSupervisor: DevEmployee
let daycareServiceNeedOptionId: UUID

let page: Page

const today = LocalDate.of(2023, 9, 13) // Wed

beforeEach(async () => {
  await resetServiceState()
})

const setupTestData = async ({
  placementType = 'DAYCARE'
}: {
  placementType?: PlacementType
} = {}) => {
  await createDefaultServiceNeedOptions()
  await Fixture.preschoolTerm(preschoolTerm2023).save()
  daycareServiceNeedOptionId = (
    await Fixture.serviceNeedOption({ validPlacementType: 'DAYCARE' }).save()
  ).id
  const careArea = await Fixture.careArea().save()
  const operationTime = new TimeRange(LocalTime.of(7, 0), LocalTime.of(18, 0))
  const daycare = await Fixture.daycare({
    areaId: careArea.id,
    enabledPilotFeatures: ['RESERVATIONS'],
    operationTimes: [
      operationTime,
      operationTime,
      operationTime,
      operationTime,
      operationTime,
      null,
      null
    ]
  }).save()
  daycareId = daycare.id
  const daycareGroup = await Fixture.daycareGroup({
    daycareId: daycare.id
  }).save()
  const child = await Fixture.person().saveChild()
  childId = child.id
  const placement = await Fixture.placement({
    childId: child.id,
    unitId: daycare.id,
    type: placementType,
    startDate: today.subMonths(6),
    endDate: today.addMonths(6)
  }).save()
  placementId = placement.id
  await Fixture.groupPlacement({
    daycareGroupId: daycareGroup.id,
    daycarePlacementId: placement.id,
    startDate: placement.startDate,
    endDate: placement.endDate
  }).save()
  unitSupervisor = await Fixture.employeeUnitSupervisor(daycare.id).save()
}

async function navigateToTestView({
  intermittentShiftCareEnabled = false
}: {
  intermittentShiftCareEnabled?: boolean
} = {}): Promise<UnitWeekCalendarPage> {
  page = await Page.open({
    mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0)),
    employeeCustomizations: {
      featureFlags: {
        intermittentShiftCare: intermittentShiftCareEnabled
      }
    }
  })
  await employeeLogin(page, unitSupervisor)
  const unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(daycareId)
  return await unitPage.openWeekCalendar()
}

test('Add two reservations for tomorrow then update one and remove other', async () => {
  await setupTestData()
  const weekCalendar = await navigateToTestView()
  const reservationsTable = weekCalendar.childReservations
  const date = today.addDays(1)
  const modal = await reservationsTable.openChildDateModal(childId, date)
  await modal.addReservationBtn.click()
  await modal.reservationStart(0).type('900')
  await modal.reservationEnd(0).type('16:00')
  await modal.addReservationBtn.click()
  await modal.reservationStart(1).type('2030')
  await modal.reservationEnd(1).type('2359')
  await modal.addReservationBtn.waitUntilHidden() // max 2
  await modal.addAttendanceBtn.waitUntilHidden() // no attendances in future
  await modal.submit()

  let reservations = reservationsTable.reservationCells(childId, date)
  await reservations.assertCount(2)
  await reservations.nth(0).assertTextEquals('09:00\n16:00*')
  await reservations.nth(1).assertTextEquals('20:30\n23:59 *')
  await reservationsTable
    .outsideOpeningHoursWarning(childId, date, 1)
    .waitUntilVisible()

  await reservationsTable.openChildDateModal(childId, date)
  await modal.reservationStart(0).assertValueEquals('09:00')
  await modal.reservationStart(0).fill('10:15')
  await modal.reservationRemove(1).click()
  await modal.submit()

  reservations = reservationsTable.reservationCells(childId, date)
  await reservations.assertCount(1)
  await reservations.nth(0).assertTextEquals('10:15\n16:00*')
})

test('Add two attendances for yesterday then update one and remove other', async () => {
  await setupTestData()
  const weekCalendar = await navigateToTestView()
  const reservationsTable = weekCalendar.childReservations
  const date = today.subDays(1)
  const modal = await reservationsTable.openChildDateModal(childId, date)
  await modal.addAttendanceBtn.click()
  await modal.attendanceStart(0).type('900')
  await modal.attendanceEnd(0).type('16:00')
  await modal.addAttendanceBtn.click()
  await modal.attendanceStart(1).type('2030')
  await modal.attendanceEnd(1).type('')
  await modal.addAttendanceBtn.assertDisabled(false) // no max count
  await modal.submit()

  let attendances = reservationsTable.attendanceCells(childId, date)
  await attendances.assertCount(2)
  await attendances.nth(0).assertTextEquals('09:00\n16:00')
  await attendances.nth(1).assertTextEquals('20:30\n-')

  await reservationsTable.openChildDateModal(childId, date)
  await modal.attendanceStart(0).assertValueEquals('09:00')
  await modal.attendanceStart(0).fill('10:15')
  await modal.attendanceRemove(1).click()
  await modal.submit()

  attendances = reservationsTable.attendanceCells(childId, date)
  await attendances.assertCount(1)
  await attendances.nth(0).assertTextEquals('10:15\n16:00')
})

test('Add absences for child in preschool daycare for tomorrow then update one and remove other', async () => {
  await setupTestData({ placementType: 'PRESCHOOL_DAYCARE' })
  const weekCalendar = await navigateToTestView()
  const reservationsTable = weekCalendar.childReservations
  const date = today.addDays(1)

  const modal = await reservationsTable.openChildDateModal(childId, date)
  await modal.addBillableAbsenceBtn.click()
  await modal.billableAbsenceType.selectOption('SICKLEAVE')

  await modal.addNonbillableAbsenceBtn.click()
  await waitUntilEqual(
    () => modal.nonbillableAbsenceType.selectedOption,
    'SICKLEAVE' // defaults from the other absence
  )
  await modal.nonbillableAbsenceType.selectOption('UNKNOWN_ABSENCE')
  await modal.submit()

  // billable absence has precedence
  await reservationsTable
    .reservationCells(childId, date)
    .nth(0)
    .assertTextEquals('Sairaus*')

  await reservationsTable.openChildDateModal(childId, date)
  await modal.nonbillableAbsenceRemove.click()
  await modal.billableAbsenceType.selectOption('OTHER_ABSENCE')
  await modal.submit()

  await reservationsTable
    .reservationCells(childId, date)
    .nth(0)
    .assertTextEquals('Poissaolo*')

  // reservation times are shown if partially absent
  await reservationsTable.openChildDateModal(childId, date)
  await modal.addReservationBtn.click()
  await modal.reservationStart(0).fill('09:00')
  await modal.reservationEnd(0).fill('13:00')
  await modal.addReservationBtn.click()
  await modal.reservationStart(1).fill('14:00')
  await modal.reservationEnd(1).fill('17:00')
  await modal.submit()
  await reservationsTable
    .reservationCells(childId, date)
    .nth(0)
    .assertTextEquals('09:00\n13:00*')
  await reservationsTable
    .reservationCells(childId, date)
    .nth(1)
    .assertTextEquals('14:00\n17:00*')

  // reservation times are not shown if fully absent
  await reservationsTable.openChildDateModal(childId, date)
  await modal.addNonbillableAbsenceBtn.click()
  await modal.nonbillableAbsenceType.selectOption('OTHER_ABSENCE')
  await modal.submit()
  await reservationsTable
    .reservationCells(childId, date)
    .nth(0)
    .assertTextEquals('Poissaolo*')
  await reservationsTable
    .reservationCells(childId, date)
    .nth(1)
    .assertTextEquals('')
})

test('Add absence for child in preschool for yesterday', async () => {
  await setupTestData({ placementType: 'PRESCHOOL' })
  const weekCalendar = await navigateToTestView()
  const reservationsTable = weekCalendar.childReservations
  const date = today.subDays(1)

  const modal = await reservationsTable.openChildDateModal(childId, date)
  await modal.addReservationBtn.waitUntilHidden() // no reservations in preschool
  await modal.addBillableAbsenceBtn.waitUntilHidden() // no billable absences in preschool
  await modal.addNonbillableAbsenceBtn.click()
  await modal.nonbillableAbsenceType.selectOption('OTHER_ABSENCE')
  await modal.submit()

  await reservationsTable
    .reservationCells(childId, date)
    .nth(0)
    .assertTextEquals('Poissaolo*')
})

test('Add absence for child in daycare for yesterday', async () => {
  await setupTestData({ placementType: 'DAYCARE' })
  const weekCalendar = await navigateToTestView()
  const reservationsTable = weekCalendar.childReservations
  const date = today.subDays(1)

  const modal = await reservationsTable.openChildDateModal(childId, date)
  await modal.addReservationBtn.click()
  await modal.reservationStart(0).fill('09:00')
  await modal.reservationEnd(0).fill('13:00')
  await modal.addNonbillableAbsenceBtn.waitUntilHidden() // no nonbillable absences in daycare
  await modal.addBillableAbsenceBtn.click()
  await modal.billableAbsenceType.selectOption('OTHER_ABSENCE')
  await modal.submit()

  await reservationsTable
    .reservationCells(childId, date)
    .nth(0)
    .assertTextEquals('Poissaolo*')
})

test('Absence warnings for child in daycare', async () => {
  await setupTestData({ placementType: 'DAYCARE' })
  const weekCalendar = await navigateToTestView()
  const reservationsTable = weekCalendar.childReservations
  const date = today.subDays(1)

  const modal = await reservationsTable.openChildDateModal(childId, date)

  await modal.assertWarnings(['missing-billable-absence'])

  await modal.addBillableAbsenceBtn.click()
  await modal.assertWarnings([])

  await modal.addAttendanceBtn.click()
  await modal.attendanceStart(0).fill('08:55')
  await modal.attendanceEnd(0).fill('13:05')
  await modal.assertWarnings(['extra-billable-absence'])
})

test('Absence warnings for child in preschool daycare', async () => {
  await setupTestData({ placementType: 'PRESCHOOL_DAYCARE' })
  const weekCalendar = await navigateToTestView()
  const reservationsTable = weekCalendar.childReservations
  const date = today.subDays(1)

  const modal = await reservationsTable.openChildDateModal(childId, date)

  await modal.assertWarnings([
    'missing-nonbillable-absence',
    'missing-billable-absence'
  ])

  await modal.addNonbillableAbsenceBtn.click()
  await modal.assertWarnings(['missing-billable-absence'])
  await modal.addBillableAbsenceBtn.click()
  await modal.assertWarnings([])

  await modal.addAttendanceBtn.click()
  await modal.attendanceStart(0).fill('08:55')
  await modal.attendanceEnd(0).fill('13:05')
  await modal.assertWarnings(['extra-nonbillable-absence'])

  await modal.attendanceEnd(0).fill('15:00')
  await modal.assertWarnings([
    'extra-nonbillable-absence',
    'extra-billable-absence'
  ])

  await modal.attendanceStart(0).fill('12:30')
  await modal.assertWarnings(['extra-billable-absence'])
})

test('Intermittent shift care outside opening times', async () => {
  await setupTestData({ placementType: 'DAYCARE' })
  const date1 = today.addDays(1)
  const date2 = today.addDays(2)
  // Intermittent shift care for date 1
  await Fixture.serviceNeed({
    placementId,
    optionId: daycareServiceNeedOptionId,
    shiftCare: 'INTERMITTENT',
    startDate: today.subDays(2),
    endDate: today.addDays(1),
    confirmedBy: unitSupervisor.id
  }).save()
  // No intermittent shift care for date 2
  await Fixture.serviceNeed({
    placementId,
    optionId: daycareServiceNeedOptionId,
    shiftCare: 'NONE',
    startDate: today.addDays(2),
    endDate: today.addDays(2),
    confirmedBy: unitSupervisor.id
  }).save()
  const weekCalendar = await navigateToTestView({
    intermittentShiftCareEnabled: true
  })
  const reservationsTable = weekCalendar.childReservations

  // date 1 - before opening time
  const modal = await reservationsTable.openChildDateModal(childId, date1)
  await modal.addReservationBtn.click()
  await modal.reservationStart(0).fill('05:00')
  await modal.reservationEnd(0).fill('17:00')
  await modal.submit()

  // date 2 - after closing time
  await reservationsTable.openChildDateModal(childId, date2)
  await modal.addReservationBtn.click()
  await modal.reservationStart(0).fill('09:00')
  await modal.reservationEnd(0).fill('21:00')
  await modal.submit()

  await reservationsTable
    .outsideOpeningHoursWarning(childId, date1, 0)
    .waitUntilVisible()
  await reservationsTable
    .attendanceCells(childId, date1)
    .nth(0)
    .assertTextEquals('Tee varasijoitus ')

  await reservationsTable
    .outsideOpeningHoursWarning(childId, date2, 0)
    .waitUntilVisible()
  await reservationsTable
    .attendanceCells(childId, date2)
    .nth(0)
    .assertTextEquals('-\n-')

  const monthCalendar = await weekCalendar.openMonthCalendar()
  await monthCalendar.assertTooltipContains(childId, date1, [
    'Ilta-/vuorohoito Odottaa varasijoitusta'
  ])
})

test('Intermittent shift care on a holiday', async () => {
  await setupTestData({ placementType: 'DAYCARE' })
  const date1 = today.addDays(1)
  const date2 = today.addDays(2)
  await Fixture.holiday({ date: date1 }).save()
  await Fixture.holiday({ date: date2 }).save()
  // Intermittent shift care for date 1
  await Fixture.serviceNeed({
    placementId,
    optionId: daycareServiceNeedOptionId,
    shiftCare: 'INTERMITTENT',
    startDate: today.subDays(2),
    endDate: today.addDays(1),
    confirmedBy: unitSupervisor.id
  }).save()
  // No intermittent shift care for date 2
  await Fixture.serviceNeed({
    placementId,
    optionId: daycareServiceNeedOptionId,
    shiftCare: 'NONE',
    startDate: today.addDays(2),
    endDate: today.addDays(2),
    confirmedBy: unitSupervisor.id
  }).save()
  const weekCalendar = await navigateToTestView({
    intermittentShiftCareEnabled: true
  })
  const reservationsTable = weekCalendar.childReservations

  // date 1 - before opening time
  const modal = await reservationsTable.openChildDateModal(childId, date1)
  await modal.addReservationBtn.click()
  await modal.reservationStart(0).fill('08:00')
  await modal.reservationEnd(0).fill('17:00')
  await modal.submit()

  // date 2 - after closing time
  await reservationsTable.assertCannotOpenChildDateModal(childId, date2)

  await reservationsTable
    .attendanceCells(childId, date1)
    .nth(0)
    .assertTextEquals('Tee varasijoitus ')

  await reservationsTable
    .attendanceCells(childId, date2)
    .nth(0)
    .assertTextEquals('')

  const monthCalendar = await weekCalendar.openMonthCalendar()
  await monthCalendar.assertTooltipContains(childId, date1, [
    'Ilta-/vuorohoito Odottaa varasijoitusta'
  ])
})

test('Intermittent shift care on a weekend', async () => {
  await setupTestData({ placementType: 'DAYCARE' })
  const date = today.addDays(3) // Sat
  await Fixture.serviceNeed({
    placementId,
    optionId: daycareServiceNeedOptionId,
    shiftCare: 'INTERMITTENT',
    startDate: today.subDays(2),
    endDate: today.addDays(7),
    confirmedBy: unitSupervisor.id
  }).save()
  const weekCalendar = await navigateToTestView({
    intermittentShiftCareEnabled: true
  })
  const reservationsTable = weekCalendar.childReservations

  const modal = await reservationsTable.openChildDateModal(childId, date)
  await modal.addReservationBtn.click()
  await modal.reservationStart(0).fill('11:00')
  await modal.reservationEnd(0).fill('18:00')
  await modal.submit()

  await reservationsTable
    .attendanceCells(childId, date)
    .nth(0)
    .assertTextEquals('Tee varasijoitus ')

  const monthCalendar = await weekCalendar.openMonthCalendar()
  await monthCalendar.assertTooltipContains(childId, date, [
    'Ilta-/vuorohoito Odottaa varasijoitusta'
  ])
})
