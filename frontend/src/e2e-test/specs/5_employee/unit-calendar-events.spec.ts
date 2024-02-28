// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  careArea2Fixture,
  daycare2Fixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { Child, Daycare } from '../../dev-api/types'
import {
  createDefaultServiceNeedOptions,
  resetDatabase
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import { UnitCalendarPage, UnitPage } from '../../pages/employee/units/unit'
import { waitUntilEqual, waitUntilFalse, waitUntilTrue } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage
let calendarPage: UnitCalendarPage
let child1Fixture: Child
let child2Fixture: Child
let child1DaycarePlacementId: UUID
let daycare: Daycare
let unitSupervisor: DevEmployee

// monday
const mockedToday = LocalDate.of(2022, 3, 7)
const placementStartDate = mockedToday.subWeeks(4)
const placementEndDate = mockedToday.addWeeks(4)
const groupId: UUID = uuidv4()
const groupId2 = uuidv4()

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  daycare = (
    await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()
  ).data

  unitSupervisor = (await Fixture.employeeUnitSupervisor(daycare.id).save())
    .data

  await createDefaultServiceNeedOptions()

  await Fixture.daycareGroup()
    .with({
      id: groupId,
      daycareId: daycare.id,
      name: 'Testailijat'
    })
    .save()

  await Fixture.daycareGroup()
    .with({
      id: groupId2,
      daycareId: daycare.id,
      name: 'Testailijat 2'
    })
    .save()

  child1Fixture = fixtures.familyWithTwoGuardians.children[0]
  child2Fixture = fixtures.familyWithRestrictedDetailsGuardian.children[0]

  child1DaycarePlacementId = uuidv4()
  await Fixture.placement()
    .with({
      id: child1DaycarePlacementId,
      childId: child1Fixture.id,
      unitId: daycare.id,
      startDate: placementStartDate,
      endDate: placementEndDate
    })
    .save()

  await Fixture.groupPlacement()
    .with({
      daycareGroupId: groupId,
      daycarePlacementId: child1DaycarePlacementId,
      startDate: placementStartDate,
      endDate: placementEndDate
    })
    .save()

  const child2DaycarePlacementId = uuidv4()
  await Fixture.placement()
    .with({
      id: child2DaycarePlacementId,
      childId: child2Fixture.id,
      unitId: daycare.id,
      startDate: placementStartDate,
      endDate: placementEndDate
    })
    .save()

  await Fixture.groupPlacement()
    .with({
      daycareGroupId: groupId,
      daycarePlacementId: child2DaycarePlacementId,
      startDate: placementStartDate,
      endDate: placementEndDate
    })
    .save()

  page = await Page.open({
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
