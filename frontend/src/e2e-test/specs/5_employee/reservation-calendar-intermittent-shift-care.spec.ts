// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// TODO: Reimplement with the new editor modal

/*

import { EmployeeDetail } from 'e2e-test/dev-api/types'
import { TimeRange } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import { resetDatabase } from '../../dev-api'
import { Fixture } from '../../dev-api/fixtures'
import { UnitPage } from '../../pages/employee/units/unit'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(resetDatabase)

describe('Intermittent shiftcare reservation calendar', () => {
  test('Unit supervisor can add reservation on unit operation time', async () => {
    const placementDate = LocalDate.of(2023, 6, 14) // Wed
    const { unitSupervisor, unitId, childId } = await addTestData(placementDate)

    const page = await openPage(
      placementDate.subDays(1).toHelsinkiDateTime(LocalTime.of(8, 0))
    )
    const calendarPage = await navigateToUnitCalendar(
      page,
      unitSupervisor,
      unitId
    )
    await calendarPage.selectMode('week')
    const reservations = calendarPage.attendancesSection.childReservations
    await reservations.openInlineEditor(childId)
    await reservations.setReservationTimes(placementDate, '08:00', '16:00')
    await reservations.closeInlineEditor()

    await waitUntilEqual(
      () => reservations.getReservation(placementDate, 0),
      ['08:00', '16:00']
    )
    await waitUntilEqual(
      () => reservations.getAttendance(placementDate, 0),
      ['–', '–']
    )
  })

  test('Unit supervisor can add reservation before unit operation time', async () => {
    const placementDate = LocalDate.of(2023, 6, 14) // Wed
    const { unitSupervisor, unitId, childId } = await addTestData(placementDate)

    const page = await openPage(
      placementDate.subDays(1).toHelsinkiDateTime(LocalTime.of(8, 0))
    )
    const calendarPage = await navigateToUnitCalendar(
      page,
      unitSupervisor,
      unitId
    )
    await calendarPage.selectMode('week')
    const reservations = calendarPage.attendancesSection.childReservations
    await reservations.openInlineEditor(childId)
    await reservations.setReservationTimes(placementDate, '07:59', '16:00')
    await reservations.closeInlineEditor()

    await waitUntilEqual(
      () => reservations.getReservation(placementDate, 0),
      ['07:59 ', '16:00']
    )
    await waitUntilEqual(
      () => reservations.getBackupCareRequiredWarning(placementDate, 0),
      'Tee varasijoitus '
    )

    await calendarPage.selectMode('month')
    await calendarPage.assertDayTooltip(childId, placementDate, [
      'Ilta-/vuorohoito Odottaa varasijoitusta'
    ])
  })

  test('Unit supervisor can add reservation after unit operation time', async () => {
    const placementDate = LocalDate.of(2023, 6, 14) // Wed
    const { unitSupervisor, unitId, childId } = await addTestData(placementDate)

    const page = await openPage(
      placementDate.subDays(1).toHelsinkiDateTime(LocalTime.of(8, 0))
    )
    const calendarPage = await navigateToUnitCalendar(
      page,
      unitSupervisor,
      unitId
    )
    await calendarPage.selectMode('week')
    const reservations = calendarPage.attendancesSection.childReservations
    await reservations.openInlineEditor(childId)
    await reservations.setReservationTimes(placementDate, '08:00', '16:01')
    await reservations.closeInlineEditor()

    await waitUntilEqual(
      () => reservations.getReservation(placementDate, 0),
      ['08:00', '16:01 ']
    )
    await waitUntilEqual(
      () => reservations.getBackupCareRequiredWarning(placementDate, 0),
      'Tee varasijoitus '
    )

    await calendarPage.selectMode('month')
    await calendarPage.assertDayTooltip(childId, placementDate, [
      'Ilta-/vuorohoito Odottaa varasijoitusta'
    ])
  })

  test('Unit supervisor can add reservation for holiday', async () => {
    const placementDate = LocalDate.of(2023, 6, 14) // Wed
    const { unitSupervisor, unitId, childId } = await addTestData(placementDate)
    await Fixture.holiday().with({ date: placementDate }).save()

    const page = await openPage(
      placementDate.subDays(1).toHelsinkiDateTime(LocalTime.of(8, 0))
    )
    const calendarPage = await navigateToUnitCalendar(
      page,
      unitSupervisor,
      unitId
    )
    await calendarPage.selectMode('week')
    const reservations = calendarPage.attendancesSection.childReservations
    await reservations.openInlineEditor(childId)
    await reservations.setReservationTimes(placementDate, '08:00', '16:00')
    await reservations.closeInlineEditor()

    await waitUntilEqual(
      () => reservations.getReservation(placementDate, 0),
      ['08:00 ', '16:00 ']
    )
    await waitUntilEqual(
      () => reservations.getBackupCareRequiredWarning(placementDate, 0),
      'Tee varasijoitus '
    )

    await calendarPage.selectMode('month')
    await calendarPage.assertDayTooltip(childId, placementDate, [
      'Ilta-/vuorohoito Odottaa varasijoitusta'
    ])
  })

  test('Unit supervisor can add reservation for weekend', async () => {
    const placementDate = LocalDate.of(2023, 6, 17) // Sat
    const { unitSupervisor, unitId, childId } = await addTestData(placementDate)

    const page = await openPage(
      placementDate.subDays(1).toHelsinkiDateTime(LocalTime.of(8, 0))
    )
    const calendarPage = await navigateToUnitCalendar(
      page,
      unitSupervisor,
      unitId
    )
    await calendarPage.selectMode('week')
    const reservations = calendarPage.attendancesSection.childReservations
    await reservations.openInlineEditor(childId)
    await reservations.setReservationTimes(placementDate, '08:00', '16:00')
    await reservations.closeInlineEditor()

    await waitUntilEqual(
      () => reservations.getReservation(placementDate, 0),
      ['08:00 ', '16:00 ']
    )
    await waitUntilEqual(
      () => reservations.getBackupCareRequiredWarning(placementDate, 0),
      'Tee varasijoitus '
    )

    await calendarPage.selectMode('month')
    await calendarPage.assertDayTooltip(childId, placementDate, [
      'Ilta-/vuorohoito Odottaa varasijoitusta'
    ])
  })

  const addTestData = async (date: LocalDate) => {
    const area = await Fixture.careArea().save()
    const operationTime: TimeRange = {
      start: LocalTime.of(8, 0),
      end: LocalTime.of(16, 0)
    }
    const unit = await Fixture.daycare()
      .with({
        areaId: area.data.id,
        type: ['CENTRE'],
        providerType: 'MUNICIPAL',
        operationDays: [1, 2, 3, 4, 5],
        operationTimes: [
          operationTime,
          operationTime,
          operationTime,
          operationTime,
          operationTime,
          null,
          null
        ],
        roundTheClock: false,
        enabledPilotFeatures: ['RESERVATIONS']
      })
      .save()
    const group = await Fixture.daycareGroup()
      .with({ daycareId: unit.data.id })
      .save()
    const unitSupervisor = await Fixture.employeeUnitSupervisor(
      unit.data.id
    ).save()

    const child = await Fixture.person().save()
    await Fixture.child(child.data.id).save()
    const placement = await Fixture.placement()
      .with({
        type: 'DAYCARE',
        childId: child.data.id,
        unitId: unit.data.id,
        startDate: date,
        endDate: date
      })
      .save()
    const serviceNeedOption = await Fixture.serviceNeedOption()
      .with({ validPlacementType: 'DAYCARE' })
      .save()
    await Fixture.serviceNeed()
      .with({
        placementId: placement.data.id,
        startDate: placement.data.startDate,
        endDate: placement.data.endDate,
        optionId: serviceNeedOption.data.id,
        shiftCare: 'INTERMITTENT',
        confirmedBy: unitSupervisor.data.id,
        confirmedAt: date
      })
      .save()
    await Fixture.groupPlacement()
      .with({
        daycareGroupId: group.data.id,
        daycarePlacementId: placement.data.id,
        startDate: placement.data.startDate,
        endDate: placement.data.endDate
      })
      .save()

    return {
      areaId: area.data.id,
      unitSupervisor: unitSupervisor.data,
      unitId: unit.data.id,
      childId: child.data.id
    }
  }
})

const openPage = (time: HelsinkiDateTime) =>
  Page.open({
    mockedTime: time.toSystemTzDate(),
    employeeCustomizations: {
      featureFlags: { intermittentShiftCare: true }
    }
  })

const navigateToUnitCalendar = async (
  page: Page,
  user: EmployeeDetail,
  unitId: UUID
) => {
  await employeeLogin(page, user)
  const unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(unitId)
  return await unitPage.openCalendarPage()
}
*/
