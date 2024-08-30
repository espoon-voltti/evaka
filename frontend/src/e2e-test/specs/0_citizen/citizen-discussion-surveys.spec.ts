// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DevCalendarEventTime, DevPerson } from 'e2e-test/generated/api-types'
import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import {
  createDaycarePlacementFixture,
  Fixture,
  testAdult,
  testAdult2,
  testCareArea,
  testChild,
  testChild2,
  testDaycare,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import { DiscussionSurveyModal } from '../../pages/citizen/citizen-discussion-surveys'
import CitizenHeader, { EnvType } from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Modal, Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const e: EnvType[] = ['desktop', 'mobile']

let page: Page
let header: CitizenHeader
let calendarPage: CitizenCalendarPage
let children: DevPerson[]
const today = LocalDate.of(2022, 1, 3)

const groupId = uuidv4()
const groupEventId = uuidv4()
const unitEventId = uuidv4()
const individualEventId = uuidv4()
const reservationId = uuidv4()
const groupReservationId = uuidv4()
const restrictedEventId = uuidv4()
const restrictedEventTimeId = uuidv4()
const noncancellableEventTimeId = uuidv4()

let reservationData: DevCalendarEventTime

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare({ ...testDaycare, areaId: testCareArea.id }).save()

  children = [testChild]
  await Fixture.family({
    guardian: testAdult,
    children: children
  }).save()

  const placementIds = new Map(children.map((child) => [child.id, uuidv4()]))

  await createDaycarePlacements({
    body: children.map((child) =>
      createDaycarePlacementFixture(
        placementIds.get(child.id) ?? '',
        child.id,
        testDaycare.id,
        today,
        today.addYears(1)
      )
    )
  })

  const daycareGroup = await Fixture.daycareGroup({
    id: groupId,
    daycareId: testDaycare.id,
    name: 'Group 1'
  }).save()

  for (const child of children) {
    await Fixture.groupPlacement({
      startDate: today,
      endDate: today.addYears(1),
      daycareGroupId: daycareGroup.id,
      daycarePlacementId: placementIds.get(child.id) ?? ''
    }).save()
  }

  const groupEvent = await Fixture.calendarEvent({
    id: groupEventId,
    title: 'Group-wide survey',
    description: 'Whole group',
    period: new FiniteDateRange(today.addDays(1), today.addDays(3)),
    modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN),
    eventType: 'DISCUSSION_SURVEY'
  }).save()

  await Fixture.calendarEventAttendee({
    calendarEventId: groupEvent.id,
    unitId: testDaycare.id,
    groupId: daycareGroup.id
  }).save()

  await Fixture.calendarEventTime({
    calendarEventId: groupEvent.id,
    date: today.addDays(3),
    start: LocalTime.of(8, 0),
    end: LocalTime.of(8, 30),
    childId: null
  }).save()

  await Fixture.calendarEventTime({
    id: restrictedEventTimeId,
    calendarEventId: groupEvent.id,
    date: today.addDays(1),
    start: LocalTime.of(9, 0),
    end: LocalTime.of(9, 30),
    childId: null
  }).save()
  await Fixture.calendarEventTime({
    id: groupReservationId,
    calendarEventId: groupEvent.id,
    date: today.addDays(3),
    start: LocalTime.of(9, 0),
    end: LocalTime.of(9, 30),
    childId: null
  }).save()

  const individualEvent = await Fixture.calendarEvent({
    id: individualEventId,
    title: 'Individual survey',
    description: 'Just Jari',
    period: new FiniteDateRange(today.addDays(3), today.addDays(3)),
    modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN),
    eventType: 'DISCUSSION_SURVEY'
  }).save()

  await Fixture.calendarEventAttendee({
    calendarEventId: individualEvent.id,
    unitId: testDaycare.id,
    groupId: daycareGroup.id,
    childId: testChild.id
  }).save()

  reservationData = await Fixture.calendarEventTime({
    id: reservationId,
    calendarEventId: individualEvent.id,
    date: today.addDays(3),
    start: LocalTime.of(12, 0),
    end: LocalTime.of(12, 30),
    childId: testChild.id
  }).save()

  const unitEvent = await Fixture.calendarEvent({
    id: unitEventId,
    title: 'Unit event',
    description: 'For everyone in the unit',
    period: new FiniteDateRange(today, today.addDays(4)),
    modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN)
  }).save()

  await Fixture.calendarEventAttendee({
    calendarEventId: unitEvent.id,
    unitId: testDaycare.id
  }).save()

  const restrictedEvent = await Fixture.calendarEvent({
    id: restrictedEventId,
    title: 'Restricted survey',
    description: 'Too late',
    period: new FiniteDateRange(today.addDays(1), today.addDays(3)),
    modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN),
    eventType: 'DISCUSSION_SURVEY'
  }).save()

  await Fixture.calendarEventAttendee({
    calendarEventId: restrictedEvent.id,
    unitId: testDaycare.id,
    groupId: daycareGroup.id
  }).save()

  await Fixture.calendarEventTime({
    calendarEventId: restrictedEvent.id,
    date: today.addDays(3),
    start: LocalTime.of(8, 0),
    end: LocalTime.of(8, 30),
    childId: null
  }).save()
  await Fixture.calendarEventTime({
    id: noncancellableEventTimeId,
    calendarEventId: restrictedEvent.id,
    date: today.addDays(1),
    start: LocalTime.of(10, 0),
    end: LocalTime.of(10, 30),
    childId: testChild.id
  }).save()
})

describe.each(e)('Citizen calendar discussion surveys (%s)', (env) => {
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
    await calendarPage.assertEventCount(today, 1) // unit event (1 attendee)
    await calendarPage.assertEventCount(today.addDays(1), 2) // unit event + restricted event
    await calendarPage.assertEventCount(today.addDays(3), 2) // unit event + reservation for individual survey
    await calendarPage.assertEventCount(today.addDays(4), 1) // unit event
  })

  test('Citizen sees discussions toast message', async () => {
    await waitUntilEqual(
      () => calendarPage.getActiveDiscussionsCtaContent(),
      'Sinua on pyydetty varaamaan aika lastasi koskevaan keskusteluun.\nSiirry ajanvaraukseen'
    )
  })

  test('Day modals have correct events', async () => {
    let dayView = await calendarPage.openDayView(today)

    await dayView.assertEvent(testChild.id, unitEventId, {
      title: 'Unit event / Alkuräjähdyksen päiväkoti',
      description: 'For everyone in the unit'
    })
    await calendarPage.closeToasts()
    await dayView.close()

    dayView = await calendarPage.openDayView(today.addDays(1))
    await dayView.assertEvent(testChild.id, unitEventId, {
      title: 'Unit event / Alkuräjähdyksen päiväkoti',
      description: 'For everyone in the unit'
    })
    await dayView.assertDiscussionReservation(
      testChild.id,
      restrictedEventId,
      noncancellableEventTimeId,
      false,
      {
        title: 'Restricted survey',
        description: 'Too late',
        reservationText: `klo 10:00 - 10:30`
      }
    )
    await dayView.close()

    dayView = await calendarPage.openDayView(today.addDays(3))
    await dayView.assertEvent(testChild.id, unitEventId, {
      title: 'Unit event / Alkuräjähdyksen päiväkoti',
      description: 'For everyone in the unit'
    })
    await dayView.assertDiscussionReservation(
      testChild.id,
      individualEventId,
      reservationId,
      true,
      {
        title: 'Individual survey',
        description: 'Just Jari',
        reservationText: `klo ${reservationData.start.format()} - ${reservationData.end.format()}`
      }
    )
    await dayView.assertEventNotShown(testChild.id, restrictedEventId)
    await dayView.close()

    dayView = await calendarPage.openDayView(today.addDays(4))
    await dayView.assertEvent(testChild.id, unitEventId, {
      title: 'Unit event / Alkuräjähdyksen päiväkoti',
      description: 'For everyone in the unit'
    })
  })

  test('Citizen can see open discussion survey details', async () => {
    const surveyModal = await calendarPage.openDiscussionSurveyModal()
    await calendarPage.closeToasts()
    await surveyModal.assertChildSurvey(
      groupEventId,
      testChild.id,
      'Group-wide survey'
    )

    await surveyModal.assertChildReservation(
      individualEventId,
      testChild.id,
      'Individual survey',
      reservationId,
      `to 6.1. klo 12:00 - 12:30`,
      true
    )

    await surveyModal.assertChildReservation(
      restrictedEventId,
      testChild.id,
      'Restricted survey',
      noncancellableEventTimeId,
      `ti 4.1. klo 10:00 - 10:30`,
      false
    )
  })

  test('Citizen can reserve a discussion time', async () => {
    const reservationModal = await calendarPage.openDiscussionReservationModal(
      groupEventId,
      testChild.id
    )
    await reservationModal.reserveEventTime(groupReservationId)

    const surveyModal = new DiscussionSurveyModal(
      calendarPage.discussionSurveyModal
    )
    await surveyModal.assertChildReservation(
      groupEventId,
      testChild.id,
      'Group-wide survey',
      groupReservationId,
      `to 6.1. klo 09:00 - 09:30`,
      true
    )
  })

  test('Citizen can cancel reservation from day view', async () => {
    const dayView = await calendarPage.openDayView(today.addDays(3))
    const assertionStrings = {
      title: 'Individual survey',
      description: 'Just Jari',
      reservationText: `klo ${reservationData.start.format()} - ${reservationData.end.format()}`
    }

    await dayView.assertDiscussionReservation(
      testChild.id,
      individualEventId,
      reservationId,
      true,
      assertionStrings
    )

    await dayView.cancelDiscussionReservation(
      testChild.id,
      individualEventId,
      reservationId
    )

    const confirmationModal = new Modal(calendarPage.cancelConfirmModal)
    await confirmationModal.waitUntilVisible()
    await confirmationModal.submit()

    await dayView.assertEventNotShown(testChild.id, individualEventId)
  })

  test('Citizen can cancel a discussion time from survey modal', async () => {
    const surveyModal = await calendarPage.openDiscussionSurveyModal()
    await surveyModal.assertChildReservation(
      individualEventId,
      testChild.id,
      'Individual survey',
      reservationId,
      `to 6.1. klo 12:00 - 12:30`,
      true
    )

    await surveyModal.cancelReservation(individualEventId, testChild.id)
    const confirmationModal = new Modal(calendarPage.cancelConfirmModal)
    await confirmationModal.waitUntilVisible()
    await confirmationModal.submit()

    await surveyModal.assertChildSurvey(
      individualEventId,
      testChild.id,
      'Individual survey'
    )
  })

  test('Citizen is not offered times for the next day', async () => {
    const reservationModal = await calendarPage.openDiscussionReservationModal(
      groupEventId,
      testChild.id
    )
    await reservationModal.assertEventTimeNotShown(restrictedEventTimeId)
  })
})

const visibleEventTimeId = uuidv4()
const unavailableEventTimeId = uuidv4()
const visibleLaterEventTimeId = uuidv4()
const multiPlacementEventId = uuidv4()

describe.each(e)('Citizen calendar event time visibility (%s)', (env) => {
  beforeEach(async () => {
    await Fixture.family({
      guardian: testAdult2,
      children: [testChild2]
    }).save()

    const multiPlacementEvent = await Fixture.calendarEvent({
      id: multiPlacementEventId,
      title: 'Duplication test event',
      description: 'Whole group',
      period: new FiniteDateRange(today, today.addDays(4)),
      modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN),
      eventType: 'DISCUSSION_SURVEY'
    }).save()

    await Fixture.calendarEventAttendee({
      calendarEventId: multiPlacementEvent.id,
      unitId: testDaycare.id,
      groupId: groupId
    }).save()

    await Fixture.calendarEventTime({
      id: visibleEventTimeId,
      calendarEventId: multiPlacementEvent.id,
      date: today.addDays(2),
      start: LocalTime.of(8, 0),
      end: LocalTime.of(8, 30),
      childId: null
    }).save()

    await Fixture.calendarEventTime({
      id: unavailableEventTimeId,
      calendarEventId: multiPlacementEvent.id,
      date: today.addDays(3),
      start: LocalTime.of(9, 0),
      end: LocalTime.of(9, 30),
      childId: null
    }).save()

    await Fixture.calendarEventTime({
      id: visibleLaterEventTimeId,
      calendarEventId: multiPlacementEvent.id,
      date: today.addDays(4),
      start: LocalTime.of(10, 0),
      end: LocalTime.of(10, 30),
      childId: null
    }).save()
  })
  test('Citizen receives only one correct set of event times despite 2 placements', async () => {
    const placement1 = createDaycarePlacementFixture(
      uuidv4(),
      testChild2.id,
      testDaycare.id,
      today,
      today.addDays(2)
    )

    const placement2 = createDaycarePlacementFixture(
      uuidv4(),
      testChild2.id,
      testDaycare.id,
      today.addDays(4),
      today.addDays(4)
    )

    await createDaycarePlacements({
      body: [placement1, placement2]
    })

    await Fixture.groupPlacement({
      startDate: today,
      endDate: today.addDays(2),
      daycareGroupId: groupId,
      daycarePlacementId: placement1.id
    }).save()

    await Fixture.groupPlacement({
      startDate: today.addDays(4),
      endDate: today.addDays(4),
      daycareGroupId: groupId,
      daycarePlacementId: placement2.id
    }).save()

    calendarPage = await loginAndOpenCalendar(env, testAdult2)

    const reservationModal = await calendarPage.openDiscussionReservationModal(
      multiPlacementEventId,
      testChild2.id
    )

    await reservationModal.assertEventTimes([
      visibleEventTimeId,
      visibleLaterEventTimeId
    ])
  })

  test('Citizen is receives only one correct set of event times despite 2 group placements', async () => {
    const placement1 = createDaycarePlacementFixture(
      uuidv4(),
      testChild2.id,
      testDaycare.id,
      today,
      today.addDays(4)
    )

    await createDaycarePlacements({
      body: [placement1]
    })

    await Fixture.groupPlacement({
      startDate: today,
      endDate: today.addDays(2),
      daycareGroupId: groupId,
      daycarePlacementId: placement1.id
    }).save()

    await Fixture.groupPlacement({
      startDate: today.addDays(4),
      endDate: today.addDays(4),
      daycareGroupId: groupId,
      daycarePlacementId: placement1.id
    }).save()

    calendarPage = await loginAndOpenCalendar(env, testAdult2)

    const reservationModal = await calendarPage.openDiscussionReservationModal(
      multiPlacementEventId,
      testChild2.id
    )

    await reservationModal.assertEventTimes([
      visibleEventTimeId,
      visibleLaterEventTimeId
    ])
  })
})

async function loginAndOpenCalendar(
  env: 'desktop' | 'mobile',
  user: DevPerson
): Promise<CitizenCalendarPage> {
  const viewport =
    env === 'mobile'
      ? { width: 375, height: 812 }
      : { width: 1920, height: 1080 }

  const page = await Page.open({
    viewport,
    screen: viewport,
    mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await enduserLogin(page, user)
  const calendarPage = new CitizenCalendarPage(page, env)
  const header = new CitizenHeader(page, env)
  await header.selectTab('calendar')
  return calendarPage
}
