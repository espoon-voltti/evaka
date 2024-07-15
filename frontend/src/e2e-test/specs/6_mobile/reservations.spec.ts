// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'
import { UUID } from 'lib-common/types'

import {
  Fixture,
  fullDayTimeRange,
  preschoolTerm2023
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevDaycare, DevPerson } from '../../generated/api-types'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import MobileReservationsPage from '../../pages/mobile/reservations-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

beforeEach(async (): Promise<void> => resetServiceState())

describe('when placement is ending tomorrow', () => {
  const mockedToday = LocalDate.of(2023, 11, 13)
  const mockedTomorrow = mockedToday.addDays(1)
  let unit: DevDaycare
  let child: DevPerson

  beforeEach(async () => {
    const area = await Fixture.careArea().save()
    unit = await Fixture.daycare({
      areaId: area.id,
      enabledPilotFeatures: ['RESERVATIONS']
    }).save()
    const group = await Fixture.daycareGroup({
      daycareId: unit.id,
      startDate: mockedToday
    }).save()
    child = await Fixture.person().saveChild()
    const placement = await Fixture.placement({
      childId: child.id,
      unitId: unit.id,
      startDate: mockedToday,
      endDate: mockedTomorrow
    }).save()
    await Fixture.groupPlacement({
      daycareGroupId: group.id,
      daycarePlacementId: placement.id,
      startDate: placement.startDate,
      endDate: placement.endDate
    }).save()
  })

  test('reservation times can be added', async () => {
    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Läsnäoloilmoitus puuttuu' }
    ])
    const editPage = await reservationsPage.edit()
    await editPage.fillTime(mockedTomorrow, 0, '08:00', '16:00')
    await editPage.addTime(mockedTomorrow)
    await editPage.fillTime(mockedTomorrow, 1, '16:00', '18:00')
    await editPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '08:00–16:00,16:00–18:00' }
    ])
  })

  test('reservation time can be removed', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: child.id,
      date: mockedTomorrow,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
      secondReservation: new TimeRange(LocalTime.of(16, 0), LocalTime.of(18, 0))
    }).save()

    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '08:00–16:00,16:00–18:00' }
    ])
    const editPage = await reservationsPage.edit()
    await editPage.removeTime(mockedTomorrow, 1)
    await editPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '08:00–16:00' }
    ])
  })

  test('reservation with time can be overridden', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: child.id,
      date: mockedTomorrow,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
      secondReservation: null
    }).save()

    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '08:00–16:00' }
    ])
    const editPage = await reservationsPage.edit()
    await editPage.fillTime(mockedTomorrow, 0, '07:45', '15:45')
    await editPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '07:45–15:45' }
    ])
  })

  test('reservation without time can be overridden', async () => {
    await Fixture.holidayPeriod()
      .with({ period: new FiniteDateRange(mockedTomorrow, mockedTomorrow) })
      .save()
    await Fixture.attendanceReservation({
      type: 'PRESENT',
      childId: child.id,
      date: mockedTomorrow
    }).save()

    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Läsnäoloilmoitus puuttuu' }
    ])
    const editPage = await reservationsPage.edit()
    await editPage.fillTime(mockedTomorrow, 0, '07:45', '15:45')
    await editPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '07:45–15:45' }
    ])
  })

  test('absence can be overridden', async () => {
    await Fixture.absence({ childId: child.id, date: mockedTomorrow }).save()

    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Poissa' }
    ])
    const editPage = await reservationsPage.edit()
    await editPage.removeAbsence(mockedTomorrow)
    await editPage.fillTime(mockedTomorrow, 0, '07:45', '15:45')
    await editPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '07:45–15:45' }
    ])
  })

  test('daily service time can be overridden', async () => {
    await Fixture.dailyServiceTime(child.id)
      .with({
        validityPeriod: new DateRange(mockedTomorrow, mockedTomorrow),
        type: 'REGULAR',
        regularTimes: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0))
      })
      .save()

    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Varhaiskasvatusaika tänään 08:00-16:00' }
    ])
    const editPage = await reservationsPage.edit()
    await editPage.fillTime(mockedTomorrow, 0, '07:45', '15:45')
    await editPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '07:45–15:45' }
    ])
  })
})

describe('when child is in preschool only', () => {
  const mockedToday = LocalDate.of(2023, 11, 13)
  const mockedTomorrow = mockedToday.addDays(1)
  let unit: DevDaycare
  let child: DevPerson

  beforeEach(async () => {
    const area = await Fixture.careArea().save()
    unit = await Fixture.daycare({
      areaId: area.id,
      enabledPilotFeatures: ['RESERVATIONS'],
      operationTimes: [
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        null,
        null
      ]
    }).save()
    const group = await Fixture.daycareGroup({
      daycareId: unit.id,
      startDate: mockedToday
    }).save()
    child = await Fixture.person().saveChild()
    const placement = await Fixture.placement({
      type: 'PRESCHOOL',
      childId: child.id,
      unitId: unit.id,
      startDate: mockedToday,
      endDate: mockedTomorrow
    }).save()
    await Fixture.groupPlacement({
      daycareGroupId: group.id,
      daycarePlacementId: placement.id,
      startDate: placement.startDate,
      endDate: placement.endDate
    }).save()
  })

  test('fixed schedule day', async () => {
    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Läsnä' }
    ])
    const editPage = await reservationsPage.edit()
    await editPage.assertFixedSchedule(mockedTomorrow)
  })

  test('absence can be removed from fixed schedule day', async () => {
    await Fixture.absence({ childId: child.id, date: mockedTomorrow }).save()

    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Poissa' }
    ])
    const editPage = await reservationsPage.edit()
    await editPage.removeAbsence(mockedTomorrow)
    await editPage.assertFixedSchedule(mockedTomorrow)
    await editPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Läsnä' }
    ])
  })

  test('term break day', async () => {
    await Fixture.preschoolTerm({
      ...preschoolTerm2023,
      finnishPreschool: new FiniteDateRange(mockedTomorrow, mockedTomorrow),
      termBreaks: [new FiniteDateRange(mockedTomorrow, mockedTomorrow)]
    }).save()

    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Ei toimintaa tänään' }
    ])
    const editPage = await reservationsPage.edit()
    await editPage.assertTermBreak(mockedTomorrow)
  })
})

describe('when unit is open every day for shift care child', () => {
  const mockedToday = LocalDate.of(2023, 11, 14) // Tue
  let unit: DevDaycare
  let child: DevPerson

  beforeEach(async () => {
    const area = await Fixture.careArea().save()
    unit = await Fixture.daycare({
      areaId: area.id,
      enabledPilotFeatures: ['RESERVATIONS'],
      operationTimes: [
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        null,
        null
      ],
      shiftCareOperationTimes: [
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange
      ]
    }).save()
    const group = await Fixture.daycareGroup({
      daycareId: unit.id,
      startDate: mockedToday
    }).save()
    child = await Fixture.person().saveChild()
    const placement = await Fixture.placement({
      childId: child.id,
      unitId: unit.id,
      startDate: mockedToday,
      endDate: mockedToday.addYears(1)
    }).save()
    const serviceNeedOption = await Fixture.serviceNeedOption().save()
    const unitSupervisor = await Fixture.employeeUnitSupervisor(unit.id).save()
    await Fixture.serviceNeed({
      placementId: placement.id,
      startDate: placement.startDate,
      endDate: placement.endDate,
      optionId: serviceNeedOption.id,
      confirmedBy: unitSupervisor.id,
      shiftCare: 'FULL'
    }).save()
    await Fixture.groupPlacement({
      daycareGroupId: group.id,
      daycarePlacementId: placement.id,
      startDate: placement.startDate,
      endDate: placement.endDate
    }).save()
  })

  test('reservation can be added to every non-reservable day', async () => {
    const page = await loginToMobile(mockedToday, unit.id)
    const reservationsPage = await navigateToReservations(page, child.id)
    const editPage = await reservationsPage.edit()
    await editPage.fillTime(mockedToday.addDays(1), 0, '08:01', '16:01')
    await editPage.fillTime(mockedToday.addDays(2), 0, '08:02', '16:02')
    await editPage.fillTime(mockedToday.addDays(3), 0, '08:03', '16:03')
    await editPage.fillTime(mockedToday.addDays(4), 0, '08:04', '16:04')
    await editPage.fillTime(mockedToday.addDays(5), 0, '08:05', '16:05')
    await editPage.fillTime(mockedToday.addDays(6), 0, '08:06', '16:06')
    await editPage.fillTime(mockedToday.addDays(7), 0, '08:07', '16:07')
    await editPage.fillTime(mockedToday.addDays(8), 0, '08:08', '16:08')
    await editPage.fillTime(mockedToday.addDays(9), 0, '08:09', '16:09')
    await editPage.fillTime(mockedToday.addDays(10), 0, '08:10', '16:10')
    await editPage.fillTime(mockedToday.addDays(11), 0, '08:11', '16:11')
    await editPage.fillTime(mockedToday.addDays(12), 0, '08:12', '16:12')
    await editPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedToday.addDays(1), text: '08:01–16:01' },
      { date: mockedToday.addDays(2), text: '08:02–16:02' },
      { date: mockedToday.addDays(3), text: '08:03–16:03' },
      { date: mockedToday.addDays(4), text: '08:04–16:04' },
      { date: mockedToday.addDays(5), text: '08:05–16:05' },
      { date: mockedToday.addDays(6), text: '08:06–16:06' },
      { date: mockedToday.addDays(7), text: '08:07–16:07' },
      { date: mockedToday.addDays(8), text: '08:08–16:08' },
      { date: mockedToday.addDays(9), text: '08:09–16:09' },
      { date: mockedToday.addDays(10), text: '08:10–16:10' },
      { date: mockedToday.addDays(11), text: '08:11–16:11' },
      { date: mockedToday.addDays(12), text: '08:12–16:12' }
    ])
  })
})

const loginToMobile = async (today: LocalDate, unitId: UUID) => {
  const page = await Page.open({
    mockedTime: HelsinkiDateTime.fromLocal(today, LocalTime.of(14, 50))
  })
  await page.goto(await pairMobileDevice(unitId))
  return page
}

const navigateToReservations = async (page: Page, childId: UUID) => {
  const listPage = new MobileListPage(page)
  await listPage.selectChild(childId)
  const childPage = new MobileChildPage(page)
  await childPage.markReservationsLink.click()
  return new MobileReservationsPage(page)
}
