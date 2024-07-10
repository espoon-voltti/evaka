// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import {
  testCareArea,
  testDaycare,
  testDaycareGroup,
  testChild,
  testAdult,
  Fixture
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import {
  DevDailyServiceTimes,
  DevDaycare,
  DevPerson
} from '../../generated/api-types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page

const child = testChild
let daycare: DevDaycare
let guardian: DevPerson
let dailyServiceTime: DevDailyServiceTimes

beforeEach(async () => {
  await resetServiceState()
  page = await Page.open()

  daycare = await Fixture.daycare()
    .with(testDaycare)
    .careArea(await Fixture.careArea().with(testCareArea).save())
    .save()
  await Fixture.daycareGroup().with(testDaycareGroup).daycare(daycare).save()

  const child1 = await Fixture.person()
    .with(child)
    .saveChild({ updateMockVtj: true })
  guardian = await Fixture.person()
    .with(testAdult)
    .saveAdult({ updateMockVtjWithDependants: [child1] })
  await Fixture.child(child1.id).save()
  await Fixture.guardian(child1, guardian).save()
  await Fixture.placement()
    .child(child1)
    .daycare(daycare)
    .with({
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2036, 6, 30)
    })
    .save()
  dailyServiceTime = await Fixture.dailyServiceTime(child1.id).save()
})

describe('Daily service times', () => {
  test('toast notifications are shown when non-destructive notifications exist', async () => {
    await Fixture.dailyServiceTimeNotification(guardian.id, dailyServiceTime.id)
      .with({
        dateFrom: LocalDate.of(2021, 3, 5),
        hasDeletedReservations: false
      })
      .save()

    await Fixture.dailyServiceTimeNotification(guardian.id, dailyServiceTime.id)
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
    await Fixture.dailyServiceTimeNotification(guardian.id, dailyServiceTime.id)
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
