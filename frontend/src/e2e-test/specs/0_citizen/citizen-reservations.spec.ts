// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

import {
  insertAbsence,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { PersonDetail } from '../../dev-api/types'
import CitizenCalendarPage, {
  AbsenceReservation,
  StartAndEndTimeReservation
} from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const e = ['desktop', 'mobile'] as const
describe.each(e)('Citizen attendance reservations (%s)', (env) => {
  let children: PersonDetail[]
  let fixtures: AreaAndPersonFixtures
  const today = LocalDate.of(2022, 1, 5)

  beforeEach(async () => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
    children = [
      fixtures.enduserChildFixtureJari,
      fixtures.enduserChildFixtureKaarina,
      fixtures.enduserChildFixturePorriHatterRestricted
    ]
    const placementFixtures = children.map((child) =>
      createDaycarePlacementFixture(
        uuidv4(),
        child.id,
        fixtures.daycareFixture.id,
        today.formatIso(),
        today.addYears(1).formatIso()
      )
    )
    await insertDaycarePlacementFixtures(placementFixtures)
    await insertDefaultServiceNeedOptions()

    const group = await Fixture.daycareGroup()
      .with({ daycareId: fixtures.daycareFixture.id })
      .save()

    await Promise.all(
      placementFixtures.map((placement) =>
        Fixture.groupPlacement()
          .withGroup(group)
          .with({
            daycarePlacementId: placement.id,
            startDate: placement.startDate,
            endDate: placement.endDate
          })
          .save()
      )
    )

    const employee = await Fixture.employeeStaff(fixtures.daycareFixture.id)
      .save()
      .then((e) => e.data)
    await insertAbsence(
      fixtures.enduserChildFixturePorriHatterRestricted.id,
      'UNKNOWN_ABSENCE',
      today.addDays(35),
      'BILLABLE',
      employee.id
    )
  })

  async function openCalendarPage() {
    const viewport =
      env === 'mobile'
        ? { width: 375, height: 812 }
        : { width: 1920, height: 1080 }

    const page = await Page.open({
      viewport,
      screen: viewport,
      mockedTime: today.toSystemTzDate()
    })
    await enduserLogin(page)
    const header = new CitizenHeader(page, env)
    await header.selectTab('calendar')
    return new CitizenCalendarPage(page, env)
  }

  test('Citizen creates a repeating reservation for all children', async () => {
    const calendarPage = await openCalendarPage()

    const firstReservationDay = today.addDays(14)
    const reservation = {
      startTime: '08:00',
      endTime: '16:00',
      childIds: children.map(({ id }) => id)
    }

    const reservationsModal = await calendarPage.openReservationsModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservation.startTime,
      reservation.endTime
    )

    await calendarPage.assertReservations(firstReservationDay, [reservation])
  })

  test('Citizen creates a reservation for all children, but some days are not reservable', async () => {
    // Holiday period overlaps with the reservations
    await Fixture.holidayPeriod()
      .with({
        period: new FiniteDateRange(today.addDays(15), today.addDays(16)),
        reservationDeadline: today.subDays(1)
      })
      .save()

    const calendarPage = await openCalendarPage()

    const firstReservationDay = today.addDays(14)
    const reservation = {
      startTime: '08:00',
      endTime: '16:00',
      childIds: children.map(({ id }) => id)
    }

    const reservationsModal = await calendarPage.openReservationsModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservation.startTime,
      reservation.endTime
    )

    await calendarPage.nonReservableDaysWarningModal.waitUntilVisible()
    await calendarPage.nonReservableDaysWarningModal.okButton.click()
    await calendarPage.nonReservableDaysWarningModal.waitUntilHidden()

    await calendarPage.assertReservations(firstReservationDay, [reservation])
  })

  test('Citizen creates a repeating weekly reservation for all children', async () => {
    const calendarPage = await openCalendarPage()

    // This should be a monday
    const firstReservationDay = today
      .addDays(14)
      .subDays(today.getIsoDayOfWeek() - 1)
    const weekdays = [0, 1, 2, 3, 4]
    const reservations = weekdays.map((index) => ({
      startTime: `08:0${index}`,
      endTime: `16:0${index}`,
      childIds: children.map(({ id }) => id)
    }))

    const reservationsModal = await calendarPage.openReservationsModal()
    await reservationsModal.createRepeatingWeeklyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservations
    )

    await weekdays.reduce(async (promise, index) => {
      await promise
      await calendarPage.assertReservations(
        firstReservationDay.addDays(index),
        [reservations[index]]
      )
    }, Promise.resolve())
  })

  test('Citizen cannot create reservation on day where staff has marked an absence', async () => {
    const calendarPage = await openCalendarPage()

    // This should be a monday
    const firstReservationDay = today
      .addDays(35)
      .subDays(today.getIsoDayOfWeek() - 1)

    const reservationsModal = await calendarPage.openReservationsModal()
    await reservationsModal.deselectAllChildren()
    await reservationsModal.selectChild(
      fixtures.enduserChildFixturePorriHatterRestricted.id
    )
    await reservationsModal.assertUnmodifiableDayExists(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      'WEEKLY'
    )
  })

  test('Citizen creates a repeating reservation and then marks an absence for one child', async () => {
    const calendarPage = await openCalendarPage()

    const firstReservationDay = today.addDays(14)
    const reservation = {
      startTime: '08:00',
      endTime: '16:00',
      absence: true,
      childIds: [children[0].id]
    }

    const reservationsModal = await calendarPage.openReservationsModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservation.startTime,
      reservation.endTime
    )

    const absencesModal = await calendarPage.openAbsencesModal()
    await absencesModal.markAbsence(
      children[0],
      children.length,
      new FiniteDateRange(firstReservationDay, firstReservationDay),
      'SICKLEAVE'
    )

    await calendarPage.assertReservations(firstReservationDay, [reservation])
  })

  test('Citizen creates a repeating reservation and then overwrites it', async () => {
    const calendarPage = await openCalendarPage()

    const firstReservationDay = today.addDays(14)
    const initialReservation = {
      startTime: '08:00',
      endTime: '16:00',
      childIds: children.map(({ id }) => id)
    }

    let reservationsModal = await calendarPage.openReservationsModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      initialReservation.startTime,
      initialReservation.endTime
    )

    await calendarPage.assertReservations(firstReservationDay, [
      initialReservation
    ])

    const newReservation = {
      startTime: '09:00',
      endTime: '17:00',
      childIds: children.map(({ id }) => id)
    }

    reservationsModal = await calendarPage.openReservationsModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      newReservation.startTime,
      newReservation.endTime
    )

    await calendarPage.assertReservations(firstReservationDay, [newReservation])
  })

  test('Citizen creates a reservation from day view', async () => {
    const calendarPage = await openCalendarPage()

    const reservationDay = today.addDays(14)

    const dayView = await calendarPage.openDayView(reservationDay)

    expect(children.length).toEqual(3)
    await dayView.assertNoReservation(children[0].id)
    await dayView.assertNoReservation(children[1].id)
    await dayView.assertNoReservation(children[2].id)

    const editor = await dayView.edit()
    await editor.fillReservationTimes(children[1].id, '08:00', '16:00')
    await editor.save()

    await dayView.assertNoReservation(children[0].id)
    await dayView.assertReservations(children[1].id, '08:00 – 16:00')
    await dayView.assertNoReservation(children[2].id)
  })

  test('If absence modal is opened from day view, that day is filled by default', async () => {
    const calendarPage = await openCalendarPage()

    const reservationDay = today.addDays(14)

    const dayView = await calendarPage.openDayView(reservationDay)
    const absencesModal = await dayView.createAbsence()

    await absencesModal.startDateInput.assertValueEquals(
      reservationDay.format()
    )
    await absencesModal.endDateInput.assertValueEquals(reservationDay.format())
  })

  test('Children are grouped correctly in calendar', async () => {
    const calendarPage = await openCalendarPage()

    const firstReservationDay = today.addDays(14)
    const reservation1 = {
      startTime: '08:00',
      endTime: '16:00',
      childIds: [children[0].id]
    }

    const reservation2 = {
      startTime: '09:00',
      endTime: '17:30',
      childIds: [children[1].id, children[2].id]
    }

    const reservationsModal = await calendarPage.openReservationsModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservation1.startTime,
      reservation1.endTime,
      reservation1.childIds,
      3
    )

    const reservationsModal2 = await calendarPage.openReservationsModal()
    await reservationsModal2.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservation2.startTime,
      reservation2.endTime,
      reservation2.childIds,
      3
    )

    await calendarPage.assertReservations(firstReservationDay, [
      reservation1,
      reservation2
    ])
  })

  test('Citizen creates a repeating weekly reservation for all children with absent day', async () => {
    const calendarPage = await openCalendarPage()

    // This should be a monday
    const firstReservationDay = today
      .addDays(14)
      .subDays(today.getIsoDayOfWeek() - 1)
    const weekdays = [0, 1, 2, 3, 4]
    const childIds = children.map(({ id }) => id)
    const reservations = weekdays.map<
      AbsenceReservation | StartAndEndTimeReservation
    >((index) =>
      index === 1
        ? {
            absence: true,
            childIds
          }
        : {
            startTime: `08:0${index}`,
            endTime: `16:0${index}`,
            childIds
          }
    )

    const reservationsModal = await calendarPage.openReservationsModal()
    await reservationsModal.createRepeatingWeeklyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservations
    )

    await weekdays.reduce(async (promise, index) => {
      await promise
      await calendarPage.assertReservations(
        firstReservationDay.addDays(index),
        [reservations[index]]
      )
    }, Promise.resolve())
  })
})

describe('Citizen calendar child visibility', () => {
  let page: Page
  let header: CitizenHeader
  let calendarPage: CitizenCalendarPage
  const today = LocalDate.of(2022, 1, 5)
  const placement1start = today
  const placement1end = today.addMonths(6)
  const placement2start = today.addMonths(8)
  const placement2end = today.addMonths(12)

  beforeEach(async () => {
    await resetDatabase()
    const fixtures = await initializeAreaAndPersonData()
    const child = fixtures.enduserChildFixtureJari

    await insertDaycarePlacementFixtures([
      createDaycarePlacementFixture(
        uuidv4(),
        child.id,
        fixtures.daycareFixture.id,
        placement1start.formatIso(),
        placement1end.formatIso()
      ),
      createDaycarePlacementFixture(
        uuidv4(),
        child.id,
        fixtures.daycareFixture.id,
        placement2start.formatIso(),
        placement2end.formatIso()
      )
    ])

    page = await Page.open({
      mockedTime: today.toSystemTzDate()
    })
    await enduserLogin(page)
    header = new CitizenHeader(page, 'desktop')
    calendarPage = new CitizenCalendarPage(page, 'desktop')
    await header.selectTab('calendar')
  })

  test('Child visible only while placement is active', async () => {
    await calendarPage.assertChildCountOnDay(placement1start.subDays(1), 0)
    await calendarPage.assertChildCountOnDay(placement1start, 1)
    await calendarPage.assertChildCountOnDay(placement1end, 1)
    await calendarPage.assertChildCountOnDay(placement1end.addDays(1), 0)

    await calendarPage.assertChildCountOnDay(placement2start.subDays(1), 0)
    await calendarPage.assertChildCountOnDay(placement2start, 1)
    await calendarPage.assertChildCountOnDay(placement2start.addDays(1), 1)
    await calendarPage.assertChildCountOnDay(placement2end, 1)
    await calendarPage.assertChildCountOnDay(placement2end.addDays(1), 0)
  })

  test('Day popup contains message if no children placements on that date', async () => {
    let dayView = await calendarPage.openDayView(today.subDays(1))
    await dayView.assertNoActivePlacementsMsgVisible()
    await dayView.close()
    dayView = await calendarPage.openDayView(today.addYears(1).addDays(1))
    await dayView.assertNoActivePlacementsMsgVisible()
  })
})

describe('Citizen calendar visibility', () => {
  let page: Page
  const today = LocalDate.todayInSystemTz()
  let child: PersonDetail
  let daycareId: string

  beforeEach(async () => {
    await resetDatabase()
    const fixtures = await initializeAreaAndPersonData()
    child = fixtures.enduserChildFixtureJari
    daycareId = fixtures.daycareFixture.id
  })

  test('Child is visible when placement starts within 2 weeks', async () => {
    await insertDaycarePlacementFixtures([
      createDaycarePlacementFixture(
        uuidv4(),
        child.id,
        daycareId,
        today.addDays(13).formatIso(),
        today.addYears(1).formatIso()
      )
    ])

    page = await Page.open({
      mockedTime: today.toSystemTzDate()
    })
    await enduserLogin(page)

    await page.find('[data-qa="nav-calendar-desktop"]').waitUntilVisible()
  })

  test('Child is not visible when placement starts later than 2 weeks', async () => {
    await insertDaycarePlacementFixtures([
      createDaycarePlacementFixture(
        uuidv4(),
        child.id,
        daycareId,
        today.addDays(15).formatIso(),
        today.addYears(1).formatIso()
      )
    ])

    page = await Page.open({
      mockedTime: today.toSystemTzDate()
    })

    await enduserLogin(page)

    // Ensure page has loaded
    await page.find('[data-qa="nav-children-desktop"]').waitUntilVisible()
    await page.find('[data-qa="nav-calendar-desktop"]').waitUntilHidden()
  })
})
