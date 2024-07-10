// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import zip from 'lodash/zip'

import FiniteDateRange from 'lib-common/finite-date-range'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'
import { DeepPartial, FeatureFlags } from 'lib-customizations/types'

import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  careArea2Fixture,
  careAreaFixture,
  createDaycarePlacementFixture,
  daycare2Fixture,
  daycareFixture,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  Fixture,
  systemInternalUser,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  createDefaultServiceNeedOptions,
  getAbsences,
  resetServiceState
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader, { EnvType } from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const e: EnvType[] = ['desktop', 'mobile']
const today = LocalDate.of(2022, 1, 5)

let page: Page

async function openCalendarPage(
  envType: EnvType,
  options?: {
    featureFlags?: DeepPartial<FeatureFlags>
    mockedTime?: HelsinkiDateTime
  }
) {
  const viewport =
    envType === 'mobile'
      ? { width: 375, height: 812 }
      : { width: 1920, height: 1080 }

  page = await Page.open({
    viewport,
    mockedTime:
      options?.mockedTime ?? today.toHelsinkiDateTime(LocalTime.of(12, 0)),
    citizenCustomizations: {
      featureFlags: options?.featureFlags
    }
  })
  await enduserLogin(page)
  const header = new CitizenHeader(page, envType)
  await header.selectTab('calendar')
  return new CitizenCalendarPage(page, envType)
}

describe.each(e)('Citizen attendance reservations (%s)', (env) => {
  let children: DevPerson[]
  let fixtures: AreaAndPersonFixtures

  beforeEach(async () => {
    await resetServiceState()
    fixtures = await initializeAreaAndPersonData()

    children = [
      fixtures.enduserChildFixtureJari,
      fixtures.enduserChildFixtureKaarina,
      fixtures.enduserChildFixturePorriHatterRestricted
    ]
    const placementTypes = ['DAYCARE', 'PRESCHOOL_DAYCARE', 'DAYCARE'] as const

    const placementFixtures = zip(children, placementTypes).map(
      ([child, placementType]) =>
        createDaycarePlacementFixture(
          uuidv4(),
          child!.id,
          fixtures.daycareFixture.id,
          today,
          today.addYears(1),
          placementType
        )
    )
    await createDaycarePlacements({ body: placementFixtures })
    await createDefaultServiceNeedOptions()

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
  })

  test('Citizen creates a repeating reservation for all children', async () => {
    const calendarPage = await openCalendarPage(env)

    const firstReservationDay = today.addDays(14)

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      '08:00',
      '16:00'
    )

    await calendarPage.assertDay(firstReservationDay, [
      {
        childIds: children.map(({ id }) => id),
        text: '08:00–16:00'
      }
    ])
  })

  test('Citizen creates a repeating weekly reservation for all children', async () => {
    const calendarPage = await openCalendarPage(env)

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

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.createRepeatingWeeklyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservations
    )

    for (const index of weekdays) {
      await calendarPage.assertDay(firstReservationDay.addDays(index), [
        {
          childIds: children.map(({ id }) => id),
          text: `08:0${index}–16:0${index}`
        }
      ])
    }
  })

  test('Citizen cannot create reservation on day where staff has marked an absence', async () => {
    const employee = await Fixture.employeeStaff(
      fixtures.daycareFixture.id
    ).save()
    await Fixture.absence()
      .with({
        childId: fixtures.enduserChildFixturePorriHatterRestricted.id,
        absenceType: 'UNKNOWN_ABSENCE',
        date: today.addDays(35),
        absenceCategory: 'BILLABLE',
        modifiedBy: employee.id
      })
      .save()

    const calendarPage = await openCalendarPage(env)

    // This should be a monday
    const firstReservationDay = today
      .addDays(35)
      .subDays(today.getIsoDayOfWeek() - 1)

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.deselectAllChildren()
    await reservationsModal.selectChild(
      fixtures.enduserChildFixturePorriHatterRestricted.id
    )
    await reservationsModal.startDate.fill(firstReservationDay)
    await reservationsModal.endDate.fill(firstReservationDay.addDays(6))
    await reservationsModal.selectRepetition('WEEKLY')

    await reservationsModal.assertReadOnlyWeeklyDay(2, 'absentNotEditable')
  })

  test('Citizen cannot create reservation on not-yet-open holiday period', async () => {
    const start = today.addDays(35)
    const end = start.addDays(6)
    const opens = today.addDays(1)

    await Fixture.holidayPeriod()
      .with({
        period: new FiniteDateRange(start, end),
        reservationsOpenOn: opens,
        reservationDeadline: opens
      })
      .save()

    const calendarPage = await openCalendarPage(env)

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.deselectAllChildren()
    await reservationsModal.selectChild(
      fixtures.enduserChildFixturePorriHatterRestricted.id
    )
    await reservationsModal.startDate.fill(start)
    await reservationsModal.endDate.fill(end)
    await reservationsModal.selectRepetition('IRREGULAR')

    let date = start
    while (date.isEqualOrBefore(end)) {
      if (!date.isWeekend()) {
        await reservationsModal.assertReadOnlyIrregularDay(
          date,
          'notYetReservable'
        )
      }
      date = date.addDays(1)
    }
  })

  test('Citizen creates a repeating reservation and then marks an absence for one child', async () => {
    const calendarPage = await openCalendarPage(env)

    const firstReservationDay = today.addDays(14)
    const reservation = {
      startTime: '08:00',
      endTime: '16:00',
      absence: true,
      childIds: [children[0].id]
    }

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservation.startTime,
      reservation.endTime
    )

    const absencesModal = await calendarPage.openAbsencesModal()
    await absencesModal.toggleChildren(children)
    await absencesModal.markAbsence(
      children[0],
      children.length,
      new FiniteDateRange(firstReservationDay, firstReservationDay),
      'SICKLEAVE'
    )

    await calendarPage.assertDay(firstReservationDay, [
      { childIds: [children[1].id, children[2].id], text: '08:00–16:00' },
      { childIds: [children[0].id], text: 'Poissa' }
    ])
  })

  test('Citizen creates a repeating reservation and then overwrites it', async () => {
    const calendarPage = await openCalendarPage(env)

    const firstReservationDay = today.addDays(14)

    let reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      '08:00',
      '16:00'
    )

    await calendarPage.assertDay(firstReservationDay, [
      { childIds: children.map(({ id }) => id), text: '08:00–16:00' }
    ])

    reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      '09:00',
      '17:00'
    )

    await calendarPage.assertDay(firstReservationDay, [
      { childIds: children.map(({ id }) => id), text: '09:00–17:00' }
    ])
  })

  test('Citizen creates a reservation from day view', async () => {
    const calendarPage = await openCalendarPage(env)

    const reservationDay = today.addDays(14)

    const dayView = await calendarPage.openDayView(reservationDay)

    const reservation1 = {
      startTime: '08:00',
      endTime: '16:00'
    }

    expect(children.length).toEqual(3)
    await dayView.assertNoReservation(children[0].id)
    await dayView.assertNoReservation(children[1].id)
    await dayView.assertNoReservation(children[2].id)

    const editor = await dayView.edit()
    const child = editor.childSection(children[1].id)
    await child.reservationStart.fill(reservation1.startTime)
    await child.reservationEnd.fill(reservation1.endTime)
    await editor.saveButton.click()

    await dayView.assertNoReservation(children[0].id)
    await dayView.assertReservations(children[1].id, [reservation1])
    await dayView.assertNoReservation(children[2].id)
  })

  test('Citizen creates a part-day reservation for a preschooler from day view', async () => {
    const calendarPage = await openCalendarPage(env)

    const reservationDay = today.addDays(14)

    const reservation1 = {
      startTime: '09:00',
      endTime: '13:00'
    }
    const childIds = children.map((child) => child.id)

    await calendarPage.assertDay(reservationDay, [
      { childIds, text: 'Ilmoitus puuttuu' }
    ])

    const dayView = await calendarPage.openDayView(reservationDay)

    expect(children.length).toEqual(3)
    await dayView.assertNoReservation(children[0].id)
    await dayView.assertNoReservation(children[1].id)
    await dayView.assertNoReservation(children[2].id)

    const editor = await dayView.edit()
    const child = editor.childSection(children[1].id)
    await child.reservationStart.fill(reservation1.startTime)
    await child.reservationEnd.fill(reservation1.endTime)
    await editor.saveButton.click()

    await dayView.assertNoReservation(children[0].id)
    await dayView.assertReservations(children[1].id, [reservation1])
    await dayView.assertNoReservation(children[2].id)

    // There's no way for the citizen to see that an absence was created, so use DevApi for that
    const absences = await getAbsences({
      childId: children[1].id,
      date: reservationDay
    })
    expect(absences.map((a) => a.category)).toEqual(['BILLABLE'])

    await dayView.close()
    await calendarPage.assertDay(reservationDay, [
      { childIds: [children[0].id, children[2].id], text: 'Ilmoitus puuttuu' },
      { childIds: [children[1].id], text: '09:00–13:00' }
    ])

    // When the day with a part-day absence is in the past and the child has no attendances,
    // it should be shown as an absence
    const calendarPageInFuture = await openCalendarPage(env, {
      mockedTime: reservationDay
        .addDays(1)
        .toHelsinkiDateTime(LocalTime.of(12, 0))
    })

    await calendarPageInFuture.assertDay(reservationDay, [
      { childIds: [children[0].id, children[2].id], text: 'Ilmoitus puuttuu' },
      { childIds: [children[1].id], text: 'Poissa' }
    ])

    const dayViewInFuture =
      await calendarPageInFuture.openDayView(reservationDay)
    await dayViewInFuture.assertNoReservation(children[0].id)
    await dayViewInFuture.assertAbsence(children[1].id, 'Poissa')
    await dayViewInFuture.assertNoReservation(children[2].id)
  })

  test('If absence modal is opened from day view, that day is filled by default', async () => {
    const calendarPage = await openCalendarPage(env)

    const reservationDay = today.addDays(14)

    const dayView = await calendarPage.openDayView(reservationDay)
    const absencesModal = await dayView.createAbsence()

    await absencesModal.startDateInput.assertValueEquals(
      reservationDay.format()
    )
    await absencesModal.endDateInput.assertValueEquals(reservationDay.format())
  })

  test('Children are grouped correctly in calendar', async () => {
    const calendarPage = await openCalendarPage(env)

    const firstReservationDay = today.addDays(14)

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      '08:00',
      '16:00',
      [children[0].id],
      3
    )

    const reservationsModal2 = await calendarPage.openReservationModal()
    await reservationsModal2.createRepeatingDailyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      '09:00',
      '17:30',
      [children[1].id, children[2].id],
      3
    )

    await calendarPage.assertDay(firstReservationDay, [
      { childIds: [children[1].id, children[2].id], text: '09:00–17:30' },
      { childIds: [children[0].id], text: '08:00–16:00' }
    ])
  })

  test('Citizen creates a repeating weekly reservation for all children with absent day', async () => {
    const calendarPage = await openCalendarPage(env)

    // This should be a monday
    const firstReservationDay = today
      .addDays(14)
      .subDays(today.getIsoDayOfWeek() - 1)
    const weekdays = [0, 1, 2, 3, 4]

    const reservations: (
      | { startTime: string; endTime: string }
      | { absence: true }
    )[] = weekdays.map((index) => ({
      startTime: `08:0${index}`,
      endTime: `16:0${index}`
    }))
    reservations[1] = { absence: true }

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.createRepeatingWeeklyReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(6)),
      reservations
    )

    for (const index of weekdays) {
      await calendarPage.assertDay(firstReservationDay.addDays(index), [
        {
          childIds: children.map(({ id }) => id),
          text: index === 1 ? 'Poissa' : `08:0${index}–16:0${index}`
        }
      ])
    }
  })

  test('Citizen cannot create daily reservation outside unit opening hours, validation errors shown', async () => {
    const calendarPage = await openCalendarPage(env)

    // This should be a friday
    const reservationDay = today.addDays(16)

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.deselectAllChildren()
    await reservationsModal.selectChild(
      fixtures.enduserChildFixturePorriHatterRestricted.id
    )

    const reservation1 = {
      startTime: '00:00',
      endTime: '23:59',
      childIds: [fixtures.enduserChildFixturePorriHatterRestricted.id]
    }

    await reservationsModal.fillDailyReservationInfo(
      new FiniteDateRange(reservationDay, reservationDay),
      reservation1.startTime,
      reservation1.endTime,
      reservation1.childIds
    )

    await reservationsModal.assertSendButtonDisabled(true)
    await reservationsModal.assertDailyInputValidationWarningVisible('start', 0)
    await reservationsModal.assertDailyInputValidationWarningVisible('end', 0)
  })

  test('Citizen cannot create weekly reservation outside unit opening hours, validation errors shown', async () => {
    const calendarPage = await openCalendarPage(env)

    // This should be a monday
    const reservationDay = today.addDays(19)

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.deselectAllChildren()
    await reservationsModal.selectChild(
      fixtures.enduserChildFixturePorriHatterRestricted.id
    )

    //Reservations should cover entire work week from monday to friday
    const reservations = [0, 1, 2, 3, 4].map(() => ({
      startTime: '00:00',
      endTime: '23:59',
      childIds: [fixtures.enduserChildFixturePorriHatterRestricted.id]
    }))

    await reservationsModal.fillWeeklyReservationInfo(
      new FiniteDateRange(reservationDay, reservationDay.addDays(4)),
      reservations
    )

    await reservationsModal.assertSendButtonDisabled(true)
    await reservationsModal.assertWeeklyInputValidationWarningVisible(
      'start',
      4,
      0
    )
    await reservationsModal.assertWeeklyInputValidationWarningVisible(
      'end',
      4,
      0
    )
  })

  test('Citizen cannot create irregular reservation outside unit opening hours, validation errors shown', async () => {
    const calendarPage = await openCalendarPage(env)

    // This should be a friday
    const reservationDay = today.addDays(16)

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.deselectAllChildren()
    await reservationsModal.selectChild(
      fixtures.enduserChildFixturePorriHatterRestricted.id
    )

    const reservation1 = {
      startTime: '00:00',
      endTime: '23:59',
      childIds: [fixtures.enduserChildFixturePorriHatterRestricted.id]
    }

    await reservationsModal.fillIrregularReservationInfo(
      new FiniteDateRange(reservationDay, reservationDay),
      [
        {
          date: reservationDay,
          startTime: reservation1.startTime,
          endTime: reservation1.endTime
        }
      ]
    )

    await reservationsModal.assertSendButtonDisabled(true)
    await reservationsModal.assertIrregularInputValidationWarningVisible(
      'start',
      0,
      reservationDay
    )
    await reservationsModal.assertIrregularInputValidationWarningVisible(
      'end',
      0,
      reservationDay
    )
  })

  test('Citizen cannot edit calendar reservation from day view to end up outside unit opening hours, validation errors shown', async () => {
    const calendarPage = await openCalendarPage(env)

    // This should be a friday
    const reservationDay = today.addDays(16)

    const dayView = await calendarPage.openDayView(reservationDay)

    const editor = await dayView.edit()
    const child = editor.childSection(
      fixtures.enduserChildFixturePorriHatterRestricted.id
    )
    await child.reservationStart.fill('00:00')
    await child.reservationEnd.fill('23:59')
    await child.reservationEnd.blur()

    await editor.saveButton.assertDisabled(true)
    await editor
      .findByDataQa('edit-reservation-time-0-start-info')
      .waitUntilVisible()
    await editor
      .findByDataQa('edit-reservation-time-0-end-info')
      .waitUntilVisible()
  })

  test('Citizen creates an absence and turns it back to a reservation', async () => {
    const reservationDay = today.addWeeks(2)

    const calendarPage = await openCalendarPage(env)
    let dayView = await calendarPage.openDayView(reservationDay)

    let editor = await dayView.edit()
    let childSection = editor.childSection(fixtures.enduserChildFixtureJari.id)
    await childSection.absentButton.click()
    await editor.saveButton.click()

    await dayView.assertAbsence(fixtures.enduserChildFixtureJari.id, 'Poissa')
    await dayView.close()

    dayView = await calendarPage.openDayView(reservationDay)
    editor = await dayView.edit()
    childSection = editor.childSection(fixtures.enduserChildFixtureJari.id)
    await childSection.absentButton.click()
    await childSection.reservationStart.fill('08:00')
    await childSection.reservationEnd.fill('16:00')
    await editor.saveButton.click()

    await dayView.assertReservations(fixtures.enduserChildFixtureJari.id, [
      { startTime: '08:00', endTime: '16:00' }
    ])
  })

  test('Citizen creates a weekly reservation that spans a holiday', async () => {
    // Monday
    const reservationDay = today.addDays(19)
    const reservationRange = new FiniteDateRange(
      reservationDay,
      reservationDay.addWeeks(2)
    )

    // Wednesday
    const holiday = today.addDays(21)
    await Fixture.holiday().with({ date: holiday }).save()

    const calendarPage = await openCalendarPage(env)

    const reservationsModal = await calendarPage.openReservationModal()

    // Reservations should cover entire work week from monday to friday
    const reservations = [0, 1, 2, 3, 4].map(() => ({
      startTime: '08:00',
      endTime: '16:00',
      childIds: [fixtures.enduserChildFixtureJari.id]
    }))

    await reservationsModal.fillWeeklyReservationInfo(
      reservationRange,
      reservations
    )
    await reservationsModal.save()

    for (const date of reservationRange.dates()) {
      if (date.isWeekend()) continue
      if (date.isEqual(holiday)) continue
      await calendarPage.assertDay(date, [
        {
          childIds: [
            fixtures.enduserChildFixtureJari.id,
            fixtures.enduserChildFixtureKaarina.id,
            fixtures.enduserChildFixturePorriHatterRestricted.id
          ],
          text: '08:00–16:00'
        }
      ])
    }
  })
})

describe.each(e)('Calendar day content (%s)', (env) => {
  async function init(options?: { placementType?: PlacementType }) {
    await resetServiceState()

    await Fixture.careArea().with(careAreaFixture).save()
    await Fixture.daycare().with(daycareFixture).save()
    const child = await Fixture.person()
      .with(enduserChildFixtureKaarina)
      .saveChild({ updateMockVtj: true })
    const guardian = await Fixture.person()
      .with(enduserGuardianFixture)
      .saveAdult({ updateMockVtjWithDependants: [child] })
    await Fixture.child(enduserChildFixtureKaarina.id).save()
    await Fixture.guardian(child, guardian).save()

    await Fixture.placement()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        startDate: today,
        endDate: today.addYears(1),
        type: options?.placementType ?? 'DAYCARE'
      })
      .save()
  }

  it('No placements', async () => {
    await init()
    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today.subDays(1), [])
  })

  it('Holiday', async () => {
    await init()
    await Fixture.holiday().with({ date: today }).save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertHoliday(today)
    await calendarPage.assertDay(today, [])
  })

  it('Missing reservation', async () => {
    await init()
    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today, [
      { childIds: [enduserChildFixtureKaarina.id], text: 'Ilmoitus puuttuu' }
    ])
  })

  it('Reservation', async () => {
    await init()
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      date: today,
      childId: enduserChildFixtureKaarina.id,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
      secondReservation: null
    }).save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: '08:00–16:00'
      }
    ])
  })

  it('Two reservations', async () => {
    await init()
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      date: today,
      childId: enduserChildFixtureKaarina.id,
      reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(12, 0)),
      secondReservation: new TimeRange(
        LocalTime.of(18, 0),
        LocalTime.of(23, 59)
      )
    }).save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: '08:00–12:00, 18:00–23:59'
      }
    ])
  })

  it('Holiday period highlight', async () => {
    await init()
    await Fixture.holidayPeriod()
      .with({
        period: new FiniteDateRange(today, today),
        reservationsOpenOn: today,
        reservationDeadline: today
      })
      .save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDayHighlight(today, 'holidayPeriod')
  })

  it('Holiday period not yet open', async () => {
    await init()
    await Fixture.holidayPeriod()
      .with({
        period: new FiniteDateRange(today, today),
        reservationsOpenOn: today.addDays(1),
        reservationDeadline: today.addDays(1)
      })
      .save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: `Ilmoittautuminen avataan ${today.addDays(1).format()}`
      }
    ])
  })

  it('Reservation without times', async () => {
    await init()
    await Fixture.holidayPeriod()
      .with({
        period: new FiniteDateRange(today, today),
        reservationsOpenOn: today,
        reservationDeadline: today
      })
      .save()

    await Fixture.attendanceReservationRaw({
      childId: enduserChildFixtureKaarina.id,
      date: today,
      range: null
    }).save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today, [
      { childIds: [enduserChildFixtureKaarina.id], text: 'Läsnä' }
    ])
  })

  it('Fixed schedule', async () => {
    await init({ placementType: 'PRESCHOOL' })

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today, [
      { childIds: [enduserChildFixtureKaarina.id], text: 'Läsnä' }
    ])
  })

  it('Ongoing attendance', async () => {
    await init()

    await Fixture.childAttendance()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        date: today,
        arrived: LocalTime.of(8, 0),
        departed: null
      })
      .save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: '08:00–'
      }
    ])
  })

  it('Attendance', async () => {
    await init()

    await Fixture.childAttendance()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        date: today,
        arrived: LocalTime.of(8, 0),
        departed: LocalTime.of(15, 30)
      })
      .save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: '08:00–15:30'
      }
    ])
  })

  it('Three attendances', async () => {
    await init()

    const attendanceHours: [number, number][] = [
      [8, 12],
      [14, 18],
      [20, 23]
    ]
    for (const [start, end] of attendanceHours) {
      await Fixture.childAttendance()
        .with({
          childId: enduserChildFixtureKaarina.id,
          unitId: daycareFixture.id,
          date: today,
          arrived: LocalTime.of(start, 0),
          departed: LocalTime.of(end, 0)
        })
        .save()
    }

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: '08:00–12:00, 14:00–18:00, 20:00–23:00'
      }
    ])
  })

  it('Absent', async () => {
    await init()

    await Fixture.absence()
      .with({
        childId: enduserChildFixtureKaarina.id,
        date: today,
        modifiedBy: enduserGuardianFixture.id
      })
      .save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDayHighlight(today, 'none')
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: 'Poissa'
      }
    ])
  })

  it('Absent (marked by staff)', async () => {
    await init()

    await Fixture.absence()
      .with({
        childId: enduserChildFixtureKaarina.id,
        date: today,
        modifiedBy: systemInternalUser
      })
      .save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDayHighlight(today, 'nonEditableAbsence')
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: 'Poissa'
      }
    ])
  })

  it('Free absence', async () => {
    await init()

    await Fixture.absence()
      .with({
        childId: enduserChildFixtureKaarina.id,
        date: today,
        modifiedBy: systemInternalUser,
        absenceType: 'FREE_ABSENCE'
      })
      .save()

    const calendarPage = await openCalendarPage(env)
    await calendarPage.assertDayHighlight(today, 'nonEditableAbsence')
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: 'Maksuton poissaolo'
      }
    ])
  })

  it('Planned absence', async () => {
    await init()

    await Fixture.absence()
      .with({
        childId: enduserChildFixtureKaarina.id,
        date: today,
        modifiedBy: enduserGuardianFixture.id,
        absenceType: 'PLANNED_ABSENCE'
      })
      .save()

    const calendarPage = await openCalendarPage(env, {
      featureFlags: {
        citizenAttendanceSummary: true
      }
    })
    await calendarPage.assertDay(today, [
      {
        childIds: [enduserChildFixtureKaarina.id],
        text: 'Vuorotyöpoissaolo'
      }
    ])
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
  let fixtures: AreaAndPersonFixtures
  let child: DevPerson
  let child2: DevPerson

  beforeEach(async () => {
    await resetServiceState()
    fixtures = await initializeAreaAndPersonData()
    child = fixtures.enduserChildFixtureJari
    child2 = fixtures.enduserChildFixtureKaarina

    await createDaycarePlacements({
      body: [
        createDaycarePlacementFixture(
          uuidv4(),
          child.id,
          fixtures.daycareFixture.id,
          placement1start,
          placement1end
        ),
        createDaycarePlacementFixture(
          uuidv4(),
          child.id,
          fixtures.daycareFixture.id,
          placement2start,
          placement2end
        ),
        createDaycarePlacementFixture(
          uuidv4(),
          child2.id,
          fixtures.daycareFixture.id,
          placement1start.subYears(1),
          placement1start.subDays(2)
        )
      ]
    })

    page = await Page.open({
      mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
    await enduserLogin(page)
    header = new CitizenHeader(page, 'desktop')
    calendarPage = new CitizenCalendarPage(page, 'desktop')
  })

  test('Child visible only while placement is active', async () => {
    await header.selectTab('calendar')

    await calendarPage.assertChildCountOnDay(placement1start.subDays(1), 0)
    await calendarPage.assertChildCountOnDay(placement1start, 1)
    await calendarPage.assertChildCountOnDay(placement1end, 1)
    await calendarPage.assertChildCountOnDay(placement1end.addDays(1), 0)

    await calendarPage.assertChildCountOnDay(placement2start.subDays(1), 0)
    await calendarPage.assertChildCountOnDay(placement2start, 1)
    await calendarPage.assertChildCountOnDay(placement2start.addDays(1), 1)
    await calendarPage.assertChildCountOnDay(placement2end, 1)
    await calendarPage.assertChildCountOnDay(placement2end.addDays(1), 0)

    const absencesModal = await calendarPage.openAbsencesModal()
    await absencesModal.assertChildrenSelectable([child.id])
  })

  test('Day popup contains message if no children placements on that date', async () => {
    await header.selectTab('calendar')

    let dayView = await calendarPage.openDayView(today.subDays(1))
    await dayView.assertNoActivePlacementsMsgVisible()
    await dayView.close()
    dayView = await calendarPage.openDayView(today.addYears(1).addDays(1))
    await dayView.assertNoActivePlacementsMsgVisible()
  })

  test('If other child is in round the clock daycare, the other child is not required to fill in weekends', async () => {
    const careArea = await Fixture.careArea().with(careArea2Fixture).save()
    const daycare = await Fixture.daycare()
      .with(daycare2Fixture)
      .careArea(careArea)
      .save()

    // Sibling is in 24/7 daycare with shift care
    const placement = createDaycarePlacementFixture(
      uuidv4(),
      fixtures.enduserChildFixtureKaarina.id,
      daycare2Fixture.id,
      placement1start,
      placement1end
    )
    await createDaycarePlacements({
      body: [placement]
    })
    const serviceNeedOption = await Fixture.serviceNeedOption().save()
    const unitSupervisor = await Fixture.employeeUnitSupervisor(
      daycare.id
    ).save()
    await Fixture.serviceNeed()
      .with({
        placementId: placement.id,
        startDate: placement.startDate,
        endDate: placement.endDate,
        optionId: serviceNeedOption.id,
        confirmedBy: unitSupervisor.id,
        shiftCare: 'FULL'
      })
      .save()

    await header.selectTab('calendar')
    // Saturday
    await calendarPage.assertChildCountOnDay(today.addDays(3), 1)
  })

  test('Citizen sees only children with ongoing placements', async () => {
    const calendarPage = await openCalendarPage('desktop')
    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.assertChildrenSelectable([child.id])
  })

  test('Citizen creates a reservation for a child in round the clock daycare for holidays', async () => {
    const careArea = await Fixture.careArea().with(careArea2Fixture).save()
    const daycare = await Fixture.daycare()
      .with(daycare2Fixture)
      .careArea(careArea)
      .save()
    const child = fixtures.enduserChildFixtureKaarina

    // Child is in 24/7 daycare with shift care
    const placement = createDaycarePlacementFixture(
      uuidv4(),
      fixtures.enduserChildFixtureKaarina.id,
      daycare2Fixture.id,
      placement1start,
      placement1end
    )
    await createDaycarePlacements({
      body: [placement]
    })
    const serviceNeedOption = await Fixture.serviceNeedOption().save()
    const unitSupervisor = await Fixture.employeeUnitSupervisor(
      daycare.id
    ).save()
    await Fixture.serviceNeed()
      .with({
        placementId: placement.id,
        startDate: placement.startDate,
        endDate: placement.endDate,
        optionId: serviceNeedOption.id,
        confirmedBy: unitSupervisor.id,
        shiftCare: 'FULL'
      })
      .save()

    const firstReservationDay = today.addDays(14)
    await Fixture.holiday()
      .with({ date: firstReservationDay, description: 'Test holiday 1' })
      .save()

    const calendarPage = await openCalendarPage('desktop')
    await calendarPage.assertChildCountOnDay(firstReservationDay, 1)

    const holidayDayModal = await calendarPage.openDayModal(firstReservationDay)
    await holidayDayModal.childName.assertCount(1)
    await holidayDayModal.closeModal.click()

    const reservationsModal = await calendarPage.openReservationModal()
    await reservationsModal.createIrregularReservation(
      new FiniteDateRange(firstReservationDay, firstReservationDay.addDays(1)),
      [
        {
          date: firstReservationDay,
          startTime: '08:00',
          endTime: '16:00'
        },
        {
          date: firstReservationDay.addDays(1),
          startTime: '10:00',
          endTime: '14:00'
        }
      ]
    )

    await calendarPage.assertDay(firstReservationDay, [
      { childIds: [child.id], text: '08:00–16:00' }
    ])
  })
})

describe('Citizen calendar visibility', () => {
  let page: Page
  const today = LocalDate.todayInSystemTz()
  let child: DevPerson
  let daycareId: string

  beforeEach(async () => {
    await resetServiceState()
    const fixtures = await initializeAreaAndPersonData()
    child = fixtures.enduserChildFixtureJari
    daycareId = fixtures.daycareFixture.id
  })

  test('Child is visible when placement starts within 2 weeks', async () => {
    await createDaycarePlacements({
      body: [
        createDaycarePlacementFixture(
          uuidv4(),
          child.id,
          daycareId,
          today.addDays(13),
          today.addYears(1)
        )
      ]
    })

    page = await Page.open({
      mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
    await enduserLogin(page)

    await page.findByDataQa('nav-calendar-desktop').waitUntilVisible()
  })

  test('Child is not visible when placement starts later than 2 weeks', async () => {
    await createDaycarePlacements({
      body: [
        createDaycarePlacementFixture(
          uuidv4(),
          child.id,
          daycareId,
          today.addDays(15),
          today.addYears(1)
        )
      ]
    })

    page = await Page.open({
      mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
    })

    await enduserLogin(page)

    // Ensure page has loaded
    await page.findByDataQa('nav-children-desktop').waitUntilVisible()
    await page.findByDataQa('nav-calendar-desktop').waitUntilHidden()
  })

  test('Child is not visible when placement is in the past', async () => {
    await createDaycarePlacements({
      body: [
        createDaycarePlacementFixture(
          uuidv4(),
          child.id,
          daycareId,
          today.subYears(1),
          today.subDays(1)
        )
      ]
    })

    page = await Page.open({
      mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
    })

    await enduserLogin(page)

    // Ensure page has loaded
    await page.findByDataQa('applications-list').waitUntilVisible()
    await page.findByDataQa('nav-children-desktop').waitUntilHidden()
  })
})

describe.each(e)('Citizen calendar shift care reservations', (env) => {
  let page: Page
  const today = LocalDate.of(2022, 1, 5)
  const placement1start = today
  const placement1end = today.addMonths(6)
  let fixtures: AreaAndPersonFixtures

  beforeEach(async () => {
    await resetServiceState()
    fixtures = await initializeAreaAndPersonData()
    const careArea = await Fixture.careArea().with(careArea2Fixture).save()
    await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()

    await createDaycarePlacements({
      body: [
        createDaycarePlacementFixture(
          uuidv4(),
          fixtures.enduserChildFixtureKaarina.id,
          daycare2Fixture.id,
          placement1start,
          placement1end
        )
      ]
    })
    page = await Page.open({
      mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
    await enduserLogin(page)
  })

  test(`Citizen creates 2 reservations from day view: ${env}`, async () => {
    const calendarPage = await openCalendarPage(env)

    const reservationDay = today.addDays(14)

    const reservation1 = {
      startTime: '08:00',
      endTime: '16:00'
    }

    const reservation2 = {
      startTime: '17:00',
      endTime: '19:00'
    }

    const dayView = await calendarPage.openDayView(reservationDay)

    await dayView.assertNoReservation(fixtures.enduserChildFixtureKaarina.id)

    let editor = await dayView.edit()
    let child = editor.childSection(fixtures.enduserChildFixtureKaarina.id)
    await child.reservationStart.fill(reservation1.startTime)
    await child.reservationEnd.fill(reservation1.endTime)
    await editor.saveButton.click()

    await dayView.assertReservations(fixtures.enduserChildFixtureKaarina.id, [
      reservation1
    ])

    editor = await dayView.edit()
    child = editor.childSection(fixtures.enduserChildFixtureKaarina.id)
    await child.addSecondReservationButton.click()
    await child.reservation2Start.fill(reservation2.startTime)
    await child.reservation2End.fill(reservation2.endTime)
    await editor.saveButton.click()

    await dayView.assertReservations(fixtures.enduserChildFixtureKaarina.id, [
      reservation1,
      reservation2
    ])
  })

  test(`Citizen creates 2 reservations from reservation modal: ${env}`, async () => {
    const calendarPage = await openCalendarPage(env)
    const reservationDay = today.addDays(14)

    const reservation1 = {
      startTime: '10:00',
      endTime: '16:00',
      childIds: [fixtures.enduserChildFixtureKaarina.id]
    }

    const reservation2 = {
      startTime: '17:00',
      endTime: '18:00'
    }
    const reservationsModal = await calendarPage.openReservationModal()

    await reservationsModal.fillDailyReservationInfo(
      new FiniteDateRange(reservationDay, reservationDay),
      reservation1.startTime,
      reservation1.endTime,
      reservation1.childIds,
      1
    )

    await reservationsModal.fillDaily2ndReservationInfo(
      reservation2.startTime,
      reservation2.endTime
    )

    await reservationsModal.save()

    const dayView = await calendarPage.openDayView(reservationDay)

    await dayView.assertReservations(fixtures.enduserChildFixtureKaarina.id, [
      reservation1,
      reservation2
    ])
  })
})
