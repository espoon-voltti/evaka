// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  testCareArea,
  testDaycare,
  testDaycareGroup,
  testChild,
  testAdult,
  Fixture
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevDaycare, DevPerson } from '../../generated/api-types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page

const startDate = LocalDate.of(2024, 6, 3)
const period = new FiniteDateRange(startDate, LocalDate.of(2024, 8, 15))
const child = testChild
const mockedDate = LocalDate.of(2024, 5, 25)
let daycare: DevDaycare
let guardian: DevPerson

beforeEach(async () => {
  await resetServiceState()
  page = await Page.open({
    mockedTime: mockedDate.toHelsinkiDateTime(LocalTime.of(12, 0))
  })

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
      startDate: startDate,
      endDate: LocalDate.of(2026, 6, 30)
    })
    .save()
  await Fixture.holidayPeriod()
    .with({
      period,
      reservationDeadline: LocalDate.of(2024, 5, 10)
    })
    .save()
})

describe('Placement start after deadline end', () => {
  test('citizen can mark repeating attendances', async () => {
    await enduserLogin(page, guardian)
    await new CitizenHeader(page).selectTab('calendar')
    const calendar = new CitizenCalendarPage(page, 'desktop')

    await calendar.assertDay(startDate, [
      {
        childIds: [child.id],
        text: 'Ilmoitus puuttuu'
      }
    ])

    const reservationsModal = await calendar.openReservationModal()
    await reservationsModal.createRepeatingDailyReservation(
      new FiniteDateRange(startDate, LocalDate.of(2024, 6, 7)),
      '08:00',
      '16:00'
    )

    for (let index = 0; index <= 4; index++) {
      await calendar.assertDay(startDate.addDays(index), [
        {
          childIds: [child.id],
          text: '08:00–16:00'
        }
      ])
    }
  })
  test('citizen can mark single day attendances', async () => {
    await enduserLogin(page, guardian)
    await new CitizenHeader(page).selectTab('calendar')
    const calendar = new CitizenCalendarPage(page, 'desktop')

    await calendar.assertDay(startDate, [
      {
        childIds: [child.id],
        text: 'Ilmoitus puuttuu'
      }
    ])

    const dayView = await calendar.openDayView(startDate)
    const editor = await dayView.edit()
    const childView = editor.childSection(child.id)

    const reservation = {
      startTime: '08:00',
      endTime: '16:00'
    }

    await childView.reservationStart.fill(reservation.startTime)
    await childView.reservationEnd.fill(reservation.endTime)
    await editor.saveButton.click()

    await dayView.assertReservations(child.id, [reservation])
    await dayView.close()

    await calendar.assertDay(startDate, [
      {
        childIds: [child.id],
        text: '08:00–16:00'
      }
    ])
  })
})
