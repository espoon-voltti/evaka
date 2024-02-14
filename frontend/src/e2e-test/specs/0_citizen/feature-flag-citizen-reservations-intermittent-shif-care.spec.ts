// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { careAreaFixture, Fixture } from '../../dev-api/fixtures'
import { PersonDetail } from '../../dev-api/types'
import { resetDatabase } from '../../generated/api-clients'
import CitizenCalendarPage, {
  FormatterReservation,
  TwoPartReservation
} from '../../pages/citizen/citizen-calendar'
import CitizenHeader, { EnvType } from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const e: EnvType[] = ['desktop', 'mobile']
const june7th2023 = LocalDate.of(2023, 6, 7)

describe.each(e)(
  'Citizen reservations with intermittent shift care (%s)',
  (env) => {
    beforeEach(async (): Promise<void> => resetDatabase())

    test('Citizen creates a repeating reservation outside placement unit times', async () => {
      const { child, parent } = await addTestData(june7th2023)
      const calendarPage = await openCalendarPage(
        june7th2023.toHelsinkiDateTime(LocalTime.of(8, 0)),
        env,
        parent
      )

      const firstReservationDay = june7th2023.addDays(14).addDays(1) //Thursday

      const reservationsModal = await calendarPage.openReservationsModal()
      await reservationsModal.createRepeatingDailyReservation(
        new FiniteDateRange(
          firstReservationDay,
          firstReservationDay.addDays(6)
        ),
        '08:00',
        '22:00'
      )

      await calendarPage.assertDay(firstReservationDay, [
        { childIds: [child.id], text: '08:00–22:00 Ilta-/vuorohoito' }
      ])
    })

    test('Citizen creates a repeating reservation on a bank holiday', async () => {
      const firstReservationDay = june7th2023.addDays(14).addDays(2) //Friday, holiday

      await Fixture.holiday()
        .with({ date: firstReservationDay, description: 'Test holiday 1' })
        .save()

      const { child, parent } = await addTestData(june7th2023)
      const calendarPage = await openCalendarPage(
        june7th2023.toHelsinkiDateTime(LocalTime.of(8, 0)),
        env,
        parent
      )

      const reservationsModal = await calendarPage.openReservationsModal()
      await reservationsModal.createRepeatingDailyReservation(
        new FiniteDateRange(firstReservationDay, firstReservationDay),
        '08:00',
        '16:00'
      )

      await calendarPage.assertDay(firstReservationDay, [
        { childIds: [child.id], text: '08:00–16:00 Ilta-/vuorohoito' }
      ])
    })

    test('Citizen creates a repeating reservation for a weekend', async () => {
      const { child, parent } = await addTestData(june7th2023)
      const calendarPage = await openCalendarPage(
        june7th2023.toHelsinkiDateTime(LocalTime.of(8, 0)),
        env,
        parent
      )

      const firstReservationDay = june7th2023.addDays(14).addDays(3) //Sunday, weekend

      const reservationsModal = await calendarPage.openReservationsModal()
      await reservationsModal.createRepeatingDailyReservation(
        new FiniteDateRange(firstReservationDay, firstReservationDay),
        '08:00',
        '16:00'
      )

      await calendarPage.assertDay(firstReservationDay, [
        { childIds: [child.id], text: '08:00–16:00 Ilta-/vuorohoito' }
      ])
    })

    test('Citizen creates a repeating 2-part reservation outside placement unit times', async () => {
      const { child, parent } = await addTestData(june7th2023)
      const calendarPage = await openCalendarPage(
        june7th2023.toHelsinkiDateTime(LocalTime.of(8, 0)),
        env,
        parent
      )

      const firstReservationDay = june7th2023.addDays(14).addDays(1) //Thursday

      const reservation1 = {
        startTime: '08:00',
        endTime: '16:00',
        isOverdraft: false
      }

      const reservation2 = {
        startTime: '18:00',
        endTime: '22:00',
        isOverdraft: true
      }

      const reservationsModal = await calendarPage.openReservationsModal()

      await reservationsModal.fillDailyReservationInfo(
        new FiniteDateRange(firstReservationDay, firstReservationDay),
        reservation1.startTime,
        reservation1.endTime,
        [child.id]
      )

      await reservationsModal.fillDaily2ndReservationInfo(
        reservation2.startTime,
        reservation2.endTime
      )

      await reservationsModal.save()

      await calendarPage.assertTwoPartReservationFromDayCellGroup(
        firstReservationDay,
        [reservation1, reservation2],
        child.id,
        getTwoPartReservationOutput
      )
    })

    test('Citizen creates a day view 2-part reservation outside placement unit times', async () => {
      const { child, parent } = await addTestData(june7th2023)
      const calendarPage = await openCalendarPage(
        june7th2023.toHelsinkiDateTime(LocalTime.of(8, 0)),
        env,
        parent
      )

      const reservation1 = {
        startTime: '05:00',
        endTime: '10:00',
        isOverdraft: true
      }

      const reservation2 = {
        startTime: '10:00',
        endTime: '16:00',
        isOverdraft: false
      }

      const firstReservationDay = june7th2023.addDays(14).addDays(1) //Thursday

      const dayView = await calendarPage.openDayView(firstReservationDay)

      await dayView.assertNoReservation(child.id)

      const editor = await dayView.edit()
      const childSection = editor.childSection(child.id)
      await childSection.reservationStart.fill(reservation1.startTime)
      await childSection.reservationEnd.fill(reservation1.endTime)
      await childSection.addSecondReservationButton.click()
      await childSection.reservation2Start.fill(reservation2.startTime)
      await childSection.reservation2End.fill(reservation2.endTime)
      await editor.saveButton.click()

      await dayView.assertReservations(
        child.id,
        [reservation1, reservation2],
        getFormatterReservationOutput
      )
    })
  }
)

async function openCalendarPage(
  time: HelsinkiDateTime,
  env: EnvType,
  endUser?: PersonDetail
) {
  const viewport =
    env === 'mobile'
      ? { width: 375, height: 812 }
      : { width: 1920, height: 1080 }

  const page = await Page.open({
    viewport,
    screen: viewport,
    mockedTime: time,
    citizenCustomizations: {
      featureFlags: { intermittentShiftCare: true }
    }
  })
  await enduserLogin(page, endUser?.ssn)
  const header = new CitizenHeader(page, env)
  await header.selectTab('calendar')
  return new CitizenCalendarPage(page, env)
}

const addTestData = async (date: LocalDate) => {
  const bulkFixtures = await initializeAreaAndPersonData()
  const operationTime = new TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0))

  const unit = await Fixture.daycare()
    .with({
      name: '10h/5d',
      areaId: bulkFixtures.careAreaFixture.id,
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

  const child = bulkFixtures.enduserChildFixtureJari
  const parent = bulkFixtures.enduserGuardianFixture

  const unitSupervisor = await Fixture.employeeUnitSupervisor(
    unit.data.id
  ).save()

  const placement = await Fixture.placement()
    .with({
      type: 'DAYCARE',
      childId: child.id,
      unitId: unit.data.id,
      startDate: date,
      endDate: date.addDays(30)
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
    areaId: careAreaFixture.id,
    parent,
    unitId: unit.data.id,
    child
  }
}

const getFormatterReservationOutput = (res: FormatterReservation) =>
  res.isOverdraft
    ? `${res.startTime}–${res.endTime} Ilta-/vuorohoito`
    : `${res.startTime}–${res.endTime}`

const getTwoPartReservationOutput = (reservations: TwoPartReservation) =>
  reservations
    .map(
      (r) =>
        `${r.startTime}–${r.endTime}${r.isOverdraft ? ' Ilta-/vuorohoito' : ''}`
    )
    .join(', ')
