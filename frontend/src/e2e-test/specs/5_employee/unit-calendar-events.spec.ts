// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import { startTest } from '../../browser'
import {
  testCareArea2,
  testDaycare2,
  familyWithRestrictedDetailsGuardian,
  Fixture,
  uuidv4,
  familyWithTwoGuardians
} from '../../dev-api/fixtures'
import { createDefaultServiceNeedOptions } from '../../generated/api-clients'
import { DevDaycare, DevEmployee, DevPerson } from '../../generated/api-types'
import { UnitCalendarPage, UnitPage } from '../../pages/employee/units/unit'
import { DiscussionSurveyReadView } from '../../pages/employee/units/unit-discussion-survey-page'
import { waitUntilEqual, waitUntilFalse, waitUntilTrue } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
let calendarPage: UnitCalendarPage
let child1Fixture: DevPerson
let child2Fixture: DevPerson
let child1DaycarePlacementId: UUID
let daycare: DevDaycare
let unitSupervisor: DevEmployee

// monday
const mockedToday = LocalDate.of(2022, 3, 7)
const placementStartDate = mockedToday.subWeeks(4)
const placementEndDate = mockedToday.addWeeks(4)
const groupId: UUID = uuidv4()
const groupId2 = uuidv4()
const testSurveyId = uuidv4()
const eventTimeId = uuidv4()

beforeEach(async () => {
  await startTest()

  await Fixture.family(familyWithTwoGuardians).save()
  await Fixture.family(familyWithRestrictedDetailsGuardian).save()
  const careArea = await Fixture.careArea(testCareArea2).save()
  daycare = await Fixture.daycare({
    ...testDaycare2,
    areaId: careArea.id
  }).save()

  unitSupervisor = await Fixture.employee().unitSupervisor(daycare.id).save()

  await createDefaultServiceNeedOptions()

  await Fixture.daycareGroup({
    id: groupId,
    daycareId: daycare.id,
    name: 'Testailijat'
  }).save()

  await Fixture.daycareGroup({
    id: groupId2,
    daycareId: daycare.id,
    name: 'Testailijat 2'
  }).save()

  child1Fixture = familyWithTwoGuardians.children[0]
  child2Fixture = familyWithRestrictedDetailsGuardian.children[0]

  child1DaycarePlacementId = uuidv4()
  await Fixture.placement({
    id: child1DaycarePlacementId,
    childId: child1Fixture.id,
    unitId: daycare.id,
    startDate: placementStartDate,
    endDate: placementEndDate
  }).save()

  await Fixture.groupPlacement({
    daycareGroupId: groupId,
    daycarePlacementId: child1DaycarePlacementId,
    startDate: placementStartDate,
    endDate: placementEndDate
  }).save()

  const child2DaycarePlacementId = uuidv4()
  await Fixture.placement({
    id: child2DaycarePlacementId,
    childId: child2Fixture.id,
    unitId: daycare.id,
    startDate: placementStartDate,
    endDate: placementEndDate
  }).save()

  await Fixture.groupPlacement({
    daycareGroupId: groupId,
    daycarePlacementId: child2DaycarePlacementId,
    startDate: placementStartDate,
    endDate: placementEndDate
  }).save()

  await Fixture.calendarEvent({
    id: testSurveyId,
    title: 'Survey title',
    description: 'Survey description',
    period: new FiniteDateRange(mockedToday, mockedToday),
    eventType: 'DISCUSSION_SURVEY'
  }).save()

  await Fixture.calendarEventAttendee({
    calendarEventId: testSurveyId,
    unitId: daycare.id,
    groupId: groupId
  }).save()

  await Fixture.calendarEventTime({
    id: eventTimeId,
    calendarEventId: testSurveyId,
    date: mockedToday,
    modifiedAt: mockedToday.toHelsinkiDateTime(LocalTime.MIN)
  }).save()

  page = await Page.open({
    employeeCustomizations: {
      featureFlags: { discussionReservations: true }
    },
    mockedTime: mockedToday.toHelsinkiDateTime(LocalTime.of(12, 0))
  })
  await employeeLogin(page, unitSupervisor)
  unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(daycare.id)
  calendarPage = await unitPage.openCalendarPage()
})

describe('Calendar events', () => {
  test('Employee can add event for group', async () => {
    await calendarPage.weekModeButton.click()
    const creationModal =
      await calendarPage.calendarEventsSection.openEventCreationModal()
    await creationModal.title.fill('Test event (G)')
    await creationModal.startDateInput.fill(mockedToday.addDays(1).format())
    await creationModal.endDateInput.fill(mockedToday.addDays(1).format())
    await creationModal.description.fill('Test event description')
    await creationModal.attendees.open()
    await creationModal.attendees.option(groupId).check()
    await creationModal.attendees.close()
    await creationModal.submit()

    await calendarPage.calendarEventsSection
      .getEventOfDay(mockedToday.addDays(1), 0)
      .assertTextEquals('Testailijat: Test event (G)')
  })

  test('Employee can add multi-day event for individual child and edit and delete it', async () => {
    const startDate = mockedToday.addDays(1)
    const endDate = mockedToday.addDays(2)

    await calendarPage.weekModeButton.click()
    const creationModal =
      await calendarPage.calendarEventsSection.openEventCreationModal()
    await waitUntilTrue(() => creationModal.submitButton.disabled)
    await creationModal.title.fill('Test event (P)')
    await creationModal.startDateInput.fill(startDate.format())
    await creationModal.endDateInput.fill(endDate.format())
    await creationModal.description.fill('Test event description')
    await waitUntilFalse(() => creationModal.submitButton.disabled)
    await creationModal.attendees.open()
    await creationModal.attendees.option(groupId).uncheck()
    await waitUntilTrue(() => creationModal.submitButton.disabled)
    await creationModal.attendees.expandOption(groupId)
    await creationModal.attendees.option(child1Fixture.id).check()
    await creationModal.attendees.option(child2Fixture.id).uncheck()
    await creationModal.attendees.close()
    await waitUntilFalse(() => creationModal.submitButton.disabled)
    await creationModal.submit()

    await calendarPage.calendarEventsSection
      .getEventOfDay(startDate, 0)
      .assertTextEquals('Osa ryhmästä: Test event (P)')
    await calendarPage.calendarEventsSection
      .getEventOfDay(endDate, 0)
      .assertTextEquals('Osa ryhmästä: Test event (P)')

    await calendarPage.calendarEventsSection.getEventOfDay(startDate, 0).click()

    const editModal = calendarPage.calendarEventsSection.eventEditModal
    await waitUntilEqual(() => editModal.title.inputValue, 'Test event (P)')
    await waitUntilEqual(
      () => editModal.description.inputValue,
      'Test event description'
    )
    await editModal.title.fill('Edited event title')
    await editModal.description.fill('Edited event description')
    await editModal.submit()

    await calendarPage.calendarEventsSection
      .getEventOfDay(startDate, 0)
      .assertTextEquals('Osa ryhmästä: Edited event title')
    await calendarPage.calendarEventsSection
      .getEventOfDay(endDate, 0)
      .assertTextEquals('Osa ryhmästä: Edited event title')

    await calendarPage.calendarEventsSection.getEventOfDay(startDate, 0).click()

    await calendarPage.calendarEventsSection.eventEditModal.delete()
    await calendarPage.calendarEventsSection.eventDeleteModal.submit()

    await calendarPage.calendarEventsSection.assertNoEventsForDay(startDate)
    await calendarPage.calendarEventsSection.assertNoEventsForDay(endDate)
  })
})

describe('Discussion surveys', () => {
  test('Employee can see existing discussion survey in survey list', async () => {
    await calendarPage.weekModeButton.click()

    const surveyPage =
      await calendarPage.calendarEventsSection.openDiscussionSurveyPage()

    await surveyPage.assertDiscussionSurveyInList({
      id: testSurveyId,
      title: 'Survey title',
      status: 'Lähetetty'
    })
  })

  test('Employee can delete a survey', async () => {
    await calendarPage.weekModeButton.click()

    const surveyListPage =
      await calendarPage.calendarEventsSection.openDiscussionSurveyPage()
    const surveyView = await surveyListPage.openDiscussionSurvey(testSurveyId)
    await surveyView.waitUntilLoaded()
    const confirmDeleteModal = await surveyView.deleteSurvey()
    await confirmDeleteModal.submit()

    await surveyListPage.assertDiscussionSurveyNotInList(testSurveyId)
  })

  test('Employee can edit a survey', async () => {
    await calendarPage.weekModeButton.click()

    const surveyListPage =
      await calendarPage.calendarEventsSection.openDiscussionSurveyPage()
    const surveyView = await surveyListPage.openDiscussionSurvey(testSurveyId)
    await surveyView.waitUntilLoaded()
    const surveyEditor = await surveyView.openSurveyEditor()
    await surveyEditor.waitUntilLoaded()
    const newTitle = 'Test change for title'
    const newDescription = 'Test change for description'
    await surveyEditor.titleInput.fill(newTitle)
    await surveyEditor.descriptionInput.fill(newDescription)
    await surveyEditor.submit()

    await surveyView.waitUntilLoaded()
    await surveyView.assertSurveyTitle(newTitle)
    await surveyView.assertSurveyDescription(newDescription)
  })

  test('Employee can add event times for survey', async () => {
    const testDay = mockedToday.addDays(1)
    await calendarPage.weekModeButton.click()

    const surveyListPage =
      await calendarPage.calendarEventsSection.openDiscussionSurveyPage()
    const surveyView = await surveyListPage.openDiscussionSurvey(testSurveyId)
    await surveyView.waitUntilLoaded()

    await surveyView.addEventTimeForDay(testDay, {
      startTime: '09:00',
      endTime: '09:30'
    })
    await surveyView.waitUntilLoaded()

    await surveyView.assertEventTimeExists(0, testDay, {
      startTime: '09:00',
      endTime: '09:30'
    })

    await surveyView.addEventTimeForDay(testDay, {
      startTime: '10:00',
      endTime: '10:30'
    })
    await surveyView.waitUntilLoaded()

    await surveyView.assertEventTimeExists(1, testDay, {
      startTime: '10:00',
      endTime: '10:30'
    })
  })

  test('Employee can delete event time', async () => {
    const testDay = mockedToday.addDays(1)
    await calendarPage.weekModeButton.click()

    const surveyListPage =
      await calendarPage.calendarEventsSection.openDiscussionSurveyPage()
    const surveyView = await surveyListPage.openDiscussionSurvey(testSurveyId)
    await surveyView.waitUntilLoaded()

    await surveyView.addEventTimeForDay(testDay, {
      startTime: '09:00',
      endTime: '09:30'
    })
    await surveyView.waitUntilLoaded()

    await surveyView.assertEventTimeExists(0, testDay, {
      startTime: '09:00',
      endTime: '09:30'
    })

    const reserationModal = await surveyView.openReservationModal(0, testDay)
    await reserationModal.deleteEventTime()
    await surveyView.waitUntilLoaded()

    await surveyView.assertNoTimesExist(testDay)
  })

  test('Employee can reserve a time', async () => {
    const childData = {
      lastName: 'Högfors',
      firstName: 'Antero Onni Leevi Aatu'
    }
    const testDay = mockedToday.addDays(1)
    await calendarPage.weekModeButton.click()

    const surveyListPage =
      await calendarPage.calendarEventsSection.openDiscussionSurveyPage()
    const surveyView = await surveyListPage.openDiscussionSurvey(testSurveyId)
    await surveyView.waitUntilLoaded()

    await surveyView.addEventTimeForDay(testDay, {
      startTime: '09:00',
      endTime: '09:30'
    })
    await surveyView.waitUntilLoaded()

    await surveyView.assertEventTimeExists(0, testDay, {
      startTime: '09:00',
      endTime: '09:30'
    })

    const reserationModal = await surveyView.openReservationModal(0, testDay)
    await reserationModal.reserveEventTimeForChild(
      `${childData.lastName} ${childData.firstName}`
    )
    await surveyView.waitUntilLoaded()

    await surveyView.assertReservationExists(
      testDay,
      0,
      `${childData.firstName} ${childData.lastName}`
    )
    await surveyView.assertReservedAttendeeExists(child1Fixture.id)
  })

  test('Employee can create new survey', async () => {
    const testDay = mockedToday.addDays(1)
    await calendarPage.weekModeButton.click()

    const newSurvey = {
      title: 'New survey title',
      description: 'New survey description'
    }
    const surveyListPage =
      await calendarPage.calendarEventsSection.openDiscussionSurveyPage()
    const surveyEditor = await surveyListPage.openNewDiscussionSurveyEditor()
    await surveyEditor.titleInput.fill(newSurvey.title)
    await surveyEditor.descriptionInput.fill(newSurvey.description)
    await surveyEditor.attendeeSelect.open()
    await surveyEditor.attendeeSelect.option(groupId).uncheck()
    await waitUntilTrue(() => surveyEditor.submitSurveyButton.disabled)
    await surveyEditor.attendeeSelect.option(child1Fixture.id).check()
    await surveyEditor.attendeeSelect.option(child2Fixture.id).uncheck()
    await surveyEditor.attendeeSelect.close()

    await surveyEditor.addEventTime(testDay, 0, {
      startTime: '10:00',
      endTime: '10:20'
    })
    await waitUntilFalse(() => surveyEditor.submitSurveyButton.disabled)
    await surveyEditor.submit()

    const surveyView = new DiscussionSurveyReadView(page)
    await surveyView.waitUntilLoaded()
    await surveyView.assertSurveyTitle(newSurvey.title)
    await surveyView.assertSurveyDescription(newSurvey.description)
    await surveyView.assertUnreservedAttendeeExists(child1Fixture.id)
    await surveyView.assertEventTimeExists(0, testDay, {
      startTime: '10:00',
      endTime: '10:20'
    })
  })
})
