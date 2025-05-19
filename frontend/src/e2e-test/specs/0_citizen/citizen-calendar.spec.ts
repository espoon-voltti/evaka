// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  CalendarEventId,
  PlacementId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import type { UUID } from 'lib-common/types'

import {
  createDaycarePlacementFixture,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testChild2,
  testChildRestricted,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import type { DevPerson } from '../../generated/api-types'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { envs, Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let calendarPage: CitizenCalendarPage
let children: DevPerson[]
const today = LocalDate.of(2022, 1, 12)

const groupEventId = randomId<CalendarEventId>()
const unitEventId = randomId<CalendarEventId>()
const individualEventId = randomId<CalendarEventId>()

let jariId: UUID

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  children = [testChild, testChild2, testChildRestricted]
  jariId = testChild.id
  await Fixture.family({ guardian: testAdult, children }).save()
  const placementIds = new Map(
    children.map((child) => [child.id, randomId<PlacementId>()])
  )
  await createDaycarePlacements({
    body: children.map((child) =>
      createDaycarePlacementFixture(
        placementIds.get(child.id)!,
        child.id,
        testDaycare.id,
        today,
        today.addYears(1)
      )
    )
  })

  const daycareGroup = await Fixture.daycareGroup({
    daycareId: testDaycare.id,
    name: 'Group 1'
  }).save()

  for (const child of children) {
    await Fixture.groupPlacement({
      startDate: today,
      endDate: today.addYears(1),
      daycareGroupId: daycareGroup.id,
      daycarePlacementId: placementIds.get(child.id)!
    }).save()
  }

  const groupEvent = await Fixture.calendarEvent({
    id: groupEventId,
    title: 'Group-wide event',
    description: 'Whole group',
    period: new FiniteDateRange(today, today),
    modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN)
  }).save()

  await Fixture.calendarEventAttendee({
    calendarEventId: groupEvent.id,
    unitId: testDaycare.id,
    groupId: daycareGroup.id
  }).save()

  const individualEvent = await Fixture.calendarEvent({
    id: individualEventId,
    title: 'Individual event',
    description: 'Just Jari',
    period: new FiniteDateRange(today, today),
    modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN)
  }).save()

  await Fixture.calendarEventAttendee({
    calendarEventId: individualEvent.id,
    unitId: testDaycare.id,
    groupId: daycareGroup.id,
    childId: testChild.id
  }).save()

  const unitEvent = await Fixture.calendarEvent({
    id: unitEventId,
    title: 'Unit event',
    description: 'For everyone in the unit',
    period: new FiniteDateRange(today.addDays(1), today.addDays(2)),
    modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN)
  }).save()

  await Fixture.calendarEventAttendee({
    calendarEventId: unitEvent.id,
    unitId: testDaycare.id
  }).save()
})

describe.each(envs)('Citizen calendar (%s)', (env) => {
  beforeEach(async () => {
    const viewport =
      env === 'mobile'
        ? { width: 375, height: 812 }
        : { width: 1920, height: 1080 }

    page = await Page.open({
      viewport,
      screen: viewport,
      mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
    await enduserLogin(page, testAdult)
    header = new CitizenHeader(page, env)
    calendarPage = new CitizenCalendarPage(page, env)
    await header.selectTab('calendar')
  })

  test('Citizen sees correct amount of event counts', async () => {
    await calendarPage.assertEventCount(today, 4) // group event (3 attendees) + individual event for Jari
    await calendarPage.assertEventCount(today.addDays(1), 3) // unit event (3 attendees)
    await calendarPage.assertEventCount(today.addDays(2), 3) // unit event (3 attendees)
  })

  test('Day modals have correct events', async () => {
    let dayView = await calendarPage.openDayView(today)

    for (const child of children) {
      await dayView.assertEvent(child.id, groupEventId, {
        title: 'Group-wide event / Group 1',
        description: 'Whole group'
      })
    }

    await dayView.assertEvent(jariId, individualEventId, {
      title:
        'Individual event / Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani',
      description: 'Just Jari'
    })

    await dayView.close()

    dayView = await calendarPage.openDayView(today.addDays(1))

    for (const child of children) {
      await dayView.assertEvent(child.id, unitEventId, {
        title: 'Unit event / Alkuräjähdyksen päiväkoti',
        description: 'For everyone in the unit'
      })
    }

    await dayView.close()

    dayView = await calendarPage.openDayView(today.addDays(2))

    for (const child of children) {
      await dayView.assertEvent(child.id, unitEventId, {
        title: 'Unit event / Alkuräjähdyksen päiväkoti',
        description: 'For everyone in the unit'
      })
    }

    await dayView.close()
  })

  test('After close, focus is set to the correct day and style', async () => {
    const todayId = `calendar-day-${today.formatIso()}`
    const dayView = await calendarPage.openDayView(today)

    await dayView.close()

    await calendarPage.assertDayIsFocusedAndStyled(todayId)
  })
})
