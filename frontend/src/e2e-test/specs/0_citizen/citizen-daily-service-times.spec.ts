// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import {
  careAreaFixture,
  DailyServiceTimeBuilder,
  DaycareBuilder,
  daycareFixture,
  daycareGroupFixture,
  enduserChildFixtureJari,
  enduserGuardianFixture,
  Fixture,
  PersonBuilder
} from '../../dev-api/fixtures'
import { resetDatabase } from '../../generated/api-clients'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page

const child = enduserChildFixtureJari
let daycare: DaycareBuilder
let guardian: PersonBuilder
let dailyServiceTime: DailyServiceTimeBuilder

beforeEach(async () => {
  await resetDatabase()
  page = await Page.open()

  daycare = await Fixture.daycare()
    .with(daycareFixture)
    .careArea(await Fixture.careArea().with(careAreaFixture).save())
    .save()
  await Fixture.daycareGroup().with(daycareGroupFixture).daycare(daycare).save()

  guardian = await Fixture.person().with(enduserGuardianFixture).save()
  const child1 = await Fixture.person().with(child).save()
  await Fixture.child(child1.data.id).save()
  await Fixture.guardian(child1, guardian).save()
  await Fixture.placement()
    .child(child1)
    .daycare(daycare)
    .with({
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2036, 6, 30)
    })
    .save()
  dailyServiceTime = await Fixture.dailyServiceTime(child1.data.id).save()
})

describe('Daily service times', () => {
  test('toast notifications are shown when non-destructive notifications exist', async () => {
    await Fixture.dailyServiceTimeNotification(
      guardian.data.id,
      dailyServiceTime.data.id
    )
      .with({
        dateFrom: LocalDate.of(2021, 3, 5),
        hasDeletedReservations: false
      })
      .save()

    await Fixture.dailyServiceTimeNotification(
      guardian.data.id,
      dailyServiceTime.data.id
    )
      .with({
        dateFrom: LocalDate.of(2021, 8, 11),
        hasDeletedReservations: false
      })
      .save()

    await enduserLogin(page)
    await new CitizenHeader(page).selectTab('calendar')
    const calendar = new CitizenCalendarPage(page, 'desktop')

    await waitUntilEqual(
      () => calendar.getDailyServiceTimeNotificationContent(0),
      'Varhaiskasvatussopimukseenne on päivitetty uusi läsnäoloaika 05.03.2021 alkaen.'
    )
    await waitUntilEqual(
      () => calendar.getDailyServiceTimeNotificationContent(1),
      'Varhaiskasvatussopimukseenne on päivitetty uusi läsnäoloaika 11.08.2021 alkaen.'
    )
  })

  test('modal notification is shown when destructive notification exists', async () => {
    await Fixture.dailyServiceTimeNotification(
      guardian.data.id,
      dailyServiceTime.data.id
    )
      .with({
        dateFrom: LocalDate.of(2021, 3, 5),
        hasDeletedReservations: true
      })
      .save()

    await enduserLogin(page)
    await new CitizenHeader(page).selectTab('calendar')
    const calendar = new CitizenCalendarPage(page, 'desktop')

    await waitUntilEqual(
      () => calendar.getDailyServiceTimeNotificationModalContent(),
      'Varhaiskasvatussopimukseenne on päivitetty uusi päivittäinen läsnäoloaika. Teethän uudet läsnäoloilmoitukset 05.03.2021 alkaen.'
    )
  })
})
