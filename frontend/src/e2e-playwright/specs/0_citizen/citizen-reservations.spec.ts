// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import config from 'e2e-test-common/config'
import {
  insertDaycarePlacementFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { PersonDetail } from 'e2e-test-common/dev-api/types'
import CitizenHeader from 'e2e-playwright/pages/citizen/citizen-header'
import CitizenCalendarPage from 'e2e-playwright/pages/citizen/citizen-calendar'
import { newBrowserContext } from '../../browser'

describe('Citizen attendance reservations (desktop)', () => {
  citizenReservationTests('desktop')
})

describe('Citizen attendance reservations (mobile)', () => {
  citizenReservationTests('mobile')
})

function citizenReservationTests(env: 'desktop' | 'mobile') {
  let page: Page
  let header: CitizenHeader
  let calendarPage: CitizenCalendarPage
  let children: PersonDetail[]

  beforeEach(async () => {
    await resetDatabase()
    const fixtures = await initializeAreaAndPersonData()
    children = [
      fixtures.enduserChildFixtureJari,
      fixtures.enduserChildFixtureKaarina,
      fixtures.enduserChildFixturePorriHatterRestricted
    ]
    await insertDaycarePlacementFixtures(
      children.map((child) =>
        createDaycarePlacementFixture(
          uuidv4(),
          child.id,
          fixtures.daycareFixture.id,
          LocalDate.today().formatIso(),
          LocalDate.today().addYears(1).formatIso()
        )
      )
    )

    const viewport =
      env === 'mobile'
        ? { width: 375, height: 812 }
        : { width: 1920, height: 1080 }

    page = await (
      await newBrowserContext({ viewport, screen: viewport })
    ).newPage()
    await page.goto(config.enduserUrl)
    header = new CitizenHeader(page, env)
    calendarPage = new CitizenCalendarPage(page, env)
    await header.logIn()
    await header.selectTab('calendar')
  })

  afterEach(async () => {
    await page.close()
  })

  test('Citizen creates a repeating reservation for all children', async () => {
    const firstReservationDay = LocalDate.today().addDays(14)
    const reservation = { startTime: '08:00', endTime: '16:00' }

    await calendarPage.createReperatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservation.startTime,
      reservation.endTime
    )

    await calendarPage.assertReservations(firstReservationDay, false, [
      reservation
    ])
  })

  test('Citizen creates a repeating weekly reservation for all children', async () => {
    // This should be a monday
    const firstReservationDay = LocalDate.today()
      .addDays(14)
      .subDays(LocalDate.today().getIsoDayOfWeek() - 1)
    const weekdays = [0, 1, 2, 3, 4]
    const reservations = weekdays.map((index) => ({
      startTime: `08:0${index}`,
      endTime: `16:0${index}`
    }))

    await calendarPage.createReperatingWeeklyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservations
    )

    await weekdays.reduce(async (promise, index) => {
      await promise
      await calendarPage.assertReservations(
        firstReservationDay.addDays(index),
        false,
        [reservations[index]]
      )
    }, Promise.resolve())
  })

  test('Citizen creates a repeating reservation and then marks an absence for one child', async () => {
    const firstReservationDay = LocalDate.today().addDays(14)
    const reservation = { startTime: '08:00', endTime: '16:00' }

    await calendarPage.createReperatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservation.startTime,
      reservation.endTime
    )

    await calendarPage.markAbsence(
      children[0],
      children.length,
      new FiniteDateRange(firstReservationDay, firstReservationDay),
      'SICKLEAVE'
    )

    await calendarPage.assertReservations(firstReservationDay, true, [
      reservation
    ])
  })

  test('Citizen creates a repeating reservation and then overwrites it', async () => {
    const firstReservationDay = LocalDate.today().addDays(14)
    const initialReservation = { startTime: '08:00', endTime: '16:00' }

    await calendarPage.createReperatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      initialReservation.startTime,
      initialReservation.endTime
    )

    await calendarPage.assertReservations(firstReservationDay, false, [
      initialReservation
    ])

    const newReservation = { startTime: '09:00', endTime: '17:00' }

    await calendarPage.createReperatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      newReservation.startTime,
      newReservation.endTime
    )

    await calendarPage.assertReservations(firstReservationDay, false, [
      newReservation
    ])
  })
}
