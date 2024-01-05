// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import { resetDatabase } from '../../dev-api'
import {
  DaycareBuilder,
  Fixture,
  PersonBuilder,
  fullDayTimeRange
} from '../../dev-api/fixtures'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import MobileReservationsPage from '../../pages/mobile/reservations-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

beforeEach(resetDatabase)

describe('when placement is ending tomorrow', () => {
  const mockedToday = LocalDate.of(2023, 11, 13)
  const mockedTomorrow = mockedToday.addDays(1)
  let unit: DaycareBuilder
  let child: PersonBuilder

  beforeEach(async () => {
    const area = await Fixture.careArea().save()
    unit = await Fixture.daycare()
      .with({
        areaId: area.data.id,
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
      })
      .save()
    const group = await Fixture.daycareGroup()
      .with({ daycareId: unit.data.id, startDate: mockedToday })
      .save()
    child = await Fixture.person().save()
    await Fixture.child(child.data.id).save()
    const placement = await Fixture.placement()
      .with({
        childId: child.data.id,
        unitId: unit.data.id,
        startDate: mockedToday,
        endDate: mockedTomorrow
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
  })

  test('reservation times can be added', async () => {
    const page = await loginToMobile(mockedToday, unit.data.id)
    const reservationsPage = await navigateToReservations(page, child.data.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Läsnäoloilmoitus puuttuu' }
    ])
    await reservationsPage.editButton.click()
    await reservationsPage.fillTime(mockedTomorrow, 0, '08:00', '16:00')
    await reservationsPage.addTime(mockedTomorrow)
    await reservationsPage.fillTime(mockedTomorrow, 1, '16:00', '18:00')
    await reservationsPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '08:00–16:00,16:00–18:00' }
    ])
  })

  test('reservation time can be removed', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: child.data.id,
      date: mockedTomorrow,
      reservation: { start: LocalTime.of(8, 0), end: LocalTime.of(16, 0) },
      secondReservation: {
        start: LocalTime.of(16, 0),
        end: LocalTime.of(18, 0)
      }
    }).save()

    const page = await loginToMobile(mockedToday, unit.data.id)
    const reservationsPage = await navigateToReservations(page, child.data.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '08:00–16:00,16:00–18:00' }
    ])
    await reservationsPage.editButton.click()
    await reservationsPage.removeTime(mockedTomorrow, 1)
    await reservationsPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '08:00–16:00' }
    ])
  })

  test('reservation with time can be overridden', async () => {
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: child.data.id,
      date: mockedTomorrow,
      reservation: { start: LocalTime.of(8, 0), end: LocalTime.of(16, 0) },
      secondReservation: null
    }).save()

    const page = await loginToMobile(mockedToday, unit.data.id)
    const reservationsPage = await navigateToReservations(page, child.data.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '08:00–16:00' }
    ])
    await reservationsPage.editButton.click()
    await reservationsPage.fillTime(mockedTomorrow, 0, '07:45', '15:45')
    await reservationsPage.confirmButton.click()
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
      childId: child.data.id,
      date: mockedTomorrow
    }).save()

    const page = await loginToMobile(mockedToday, unit.data.id)
    const reservationsPage = await navigateToReservations(page, child.data.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Läsnäoloilmoitus puuttuu' }
    ])
    await reservationsPage.editButton.click()
    await reservationsPage.fillTime(mockedTomorrow, 0, '07:45', '15:45')
    await reservationsPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '07:45–15:45' }
    ])
  })

  test('absence can be overridden', async () => {
    await Fixture.absence()
      .with({ childId: child.data.id, date: mockedTomorrow })
      .save()

    const page = await loginToMobile(mockedToday, unit.data.id)
    const reservationsPage = await navigateToReservations(page, child.data.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Poissa' }
    ])
    await reservationsPage.editButton.click()
    await reservationsPage.removeAbsence(mockedTomorrow)
    await reservationsPage.fillTime(mockedTomorrow, 0, '07:45', '15:45')
    await reservationsPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '07:45–15:45' }
    ])
  })

  test('daily service time can be overridden', async () => {
    await Fixture.dailyServiceTime(child.data.id)
      .with({
        validityPeriod: new DateRange(mockedTomorrow, mockedTomorrow),
        type: 'REGULAR',
        regularTimes: {
          start: LocalTime.of(8, 0),
          end: LocalTime.of(16, 0)
        }
      })
      .save()

    const page = await loginToMobile(mockedToday, unit.data.id)
    const reservationsPage = await navigateToReservations(page, child.data.id)
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: 'Varhaiskasvatusaika tänään 08:00-16:00' }
    ])
    await reservationsPage.editButton.click()
    await reservationsPage.fillTime(mockedTomorrow, 0, '07:45', '15:45')
    await reservationsPage.confirmButton.click()
    await reservationsPage.assertReservations([
      { date: mockedTomorrow, text: '07:45–15:45' }
    ])
  })
})

describe('when unit is open every day', () => {
  const mockedToday = LocalDate.of(2023, 11, 14) // Tue
  let unit: DaycareBuilder
  let child: PersonBuilder

  beforeEach(async () => {
    const area = await Fixture.careArea().save()
    unit = await Fixture.daycare()
      .with({
        areaId: area.data.id,
        enabledPilotFeatures: ['RESERVATIONS'],
        operationTimes: [
          fullDayTimeRange,
          fullDayTimeRange,
          fullDayTimeRange,
          fullDayTimeRange,
          fullDayTimeRange,
          fullDayTimeRange,
          fullDayTimeRange
        ]
      })
      .save()
    const group = await Fixture.daycareGroup()
      .with({ daycareId: unit.data.id, startDate: mockedToday })
      .save()
    child = await Fixture.person().save()
    await Fixture.child(child.data.id).save()
    const placement = await Fixture.placement()
      .with({
        childId: child.data.id,
        unitId: unit.data.id,
        startDate: mockedToday,
        endDate: mockedToday.addYears(1)
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
  })

  test('reservation can be added to every non-reservable day', async () => {
    const page = await loginToMobile(mockedToday, unit.data.id)
    const reservationsPage = await navigateToReservations(page, child.data.id)
    await reservationsPage.editButton.click()
    await reservationsPage.fillTime(mockedToday.addDays(1), 0, '08:01', '16:01')
    await reservationsPage.fillTime(mockedToday.addDays(2), 0, '08:02', '16:02')
    await reservationsPage.fillTime(mockedToday.addDays(3), 0, '08:03', '16:03')
    await reservationsPage.fillTime(mockedToday.addDays(4), 0, '08:04', '16:04')
    await reservationsPage.fillTime(mockedToday.addDays(5), 0, '08:05', '16:05')
    await reservationsPage.fillTime(mockedToday.addDays(6), 0, '08:06', '16:06')
    await reservationsPage.fillTime(mockedToday.addDays(7), 0, '08:07', '16:07')
    await reservationsPage.fillTime(mockedToday.addDays(8), 0, '08:08', '16:08')
    await reservationsPage.fillTime(mockedToday.addDays(9), 0, '08:09', '16:09')
    await reservationsPage.fillTime(
      mockedToday.addDays(10),
      0,
      '08:10',
      '16:10'
    )
    await reservationsPage.fillTime(
      mockedToday.addDays(11),
      0,
      '08:11',
      '16:11'
    )
    await reservationsPage.fillTime(
      mockedToday.addDays(12),
      0,
      '08:12',
      '16:12'
    )
    await reservationsPage.confirmButton.click()
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
    mockedTime: HelsinkiDateTime.fromLocal(
      today,
      LocalTime.of(14, 50)
    ).toSystemTzDate()
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
