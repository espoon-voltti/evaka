// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'

import {
  Fixture,
  testAdult,
  testChild,
  testChild2
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  postChildDailyNote,
  resetServiceState
} from '../../generated/api-clients'
import { DevDaycare, DevDaycareGroup } from '../../generated/api-types'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let daycare: DevDaycare
let daycareGroup: DevDaycareGroup
let unitPage: UnitPage

beforeEach(async () => {
  await resetServiceState()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  const admin = await Fixture.employeeAdmin().save()

  await createDefaultServiceNeedOptions()

  const careArea = await Fixture.careArea().save()
  daycare = await Fixture.daycare({ areaId: careArea.id }).save()
  daycareGroup = await Fixture.daycareGroup({ daycareId: daycare.id }).save()

  const placementFixtureJari = await Fixture.placement({
    childId: testChild.id,
    unitId: daycare.id
  }).save()
  await Fixture.groupPlacement({
    daycareGroupId: daycareGroup.id,
    daycarePlacementId: placementFixtureJari.id,
    startDate: placementFixtureJari.startDate,
    endDate: placementFixtureJari.endDate
  }).save()

  const placementFixtureKaarina = await Fixture.placement({
    childId: testChild2.id,
    unitId: daycare.id
  }).save()
  await Fixture.groupPlacement({
    daycareGroupId: daycareGroup.id,
    daycarePlacementId: placementFixtureKaarina.id,
    startDate: placementFixtureKaarina.startDate,
    endDate: placementFixtureKaarina.endDate
  }).save()

  page = await Page.open()
  await employeeLogin(page, admin)

  unitPage = new UnitPage(page)
})

describe('Mobile employee daily notes', () => {
  test('Child daycare daily note indicators are shown on group view and can be edited', async () => {
    const childId = testChild.id
    const daycareDailyNote: ChildDailyNoteBody = {
      note: 'Testi viesti',
      feedingNote: 'MEDIUM',
      sleepingMinutes: 130,
      sleepingNote: 'NONE',
      reminders: ['DIAPERS'],
      reminderNote: 'Ei enää pähkinöitä antaa saa'
    }

    const hours =
      Math.floor(Number(daycareDailyNote.sleepingMinutes) / 60) > 0
        ? Math.floor(Number(daycareDailyNote.sleepingMinutes) / 60).toString()
        : ''
    const minutes =
      Number(daycareDailyNote.sleepingMinutes) % 60 > 0
        ? (Number(daycareDailyNote.sleepingMinutes) % 60).toString()
        : ''

    await postChildDailyNote({ childId, body: daycareDailyNote })

    await unitPage.navigateToUnit(daycare.id)
    const group = await unitPage.openGroupsPage()
    const groupCollapsible = await group.openGroupCollapsible(daycareGroup.id)

    const childRow = groupCollapsible.childRow(childId)
    await childRow.assertDailyNoteContainsText(daycareDailyNote.note)

    const noteModal = await childRow.openDailyNoteModal()
    await noteModal.noteInput.assertValueEquals(daycareDailyNote.note)
    await noteModal.sleepingHoursInput.assertValueEquals(hours)
    await noteModal.sleepingMinutesInput.assertValueEquals(minutes)
    await noteModal.reminderNoteInput.assertValueEquals(
      daycareDailyNote.reminderNote
    )

    await noteModal.noteInput.fill('aardvark')
    await noteModal.submitButton.click()

    await childRow.assertDailyNoteContainsText('aardvark')
  })

  test('Group daycare daily notes can be written and are shown on group notes tab', async () => {
    const childId1 = testChild.id
    const childId2 = testChild2.id
    const daycareDailyNote: ChildDailyNoteBody = {
      note: 'Toinen viesti',
      feedingNote: 'NONE',
      sleepingMinutes: null,
      sleepingNote: 'NONE',
      reminders: ['DIAPERS'],
      reminderNote: 'Muistakaa muistakaa!'
    }

    await postChildDailyNote({ childId: childId1, body: daycareDailyNote })

    await unitPage.navigateToUnit(daycare.id)
    const groupsSection = await unitPage.openGroupsPage()
    const group = await groupsSection.openGroupCollapsible(daycareGroup.id)
    let groupNoteModal = await group.openGroupDailyNoteModal()
    await groupNoteModal.fillNote('Ryhmälle viesti')
    await groupNoteModal.save()
    await groupNoteModal.close()

    let childRow = group.childRow(childId1)
    let noteModal = await childRow.openDailyNoteModal()
    await noteModal.openTab('group')
    await noteModal.assertGroupNote('Ryhmälle viesti')
    await noteModal.close()

    childRow = group.childRow(childId2)
    noteModal = await childRow.openDailyNoteModal()
    await noteModal.openTab('group')
    await noteModal.assertGroupNote('Ryhmälle viesti')
    await noteModal.close()

    // Delete group note
    groupNoteModal = await group.openGroupDailyNoteModal()
    await groupNoteModal.deleteNote()
    await groupNoteModal.close()

    childRow = group.childRow(childId1)
    noteModal = await childRow.openDailyNoteModal()
    await noteModal.openTab('group')
    await noteModal.assertNoGroupNote()
    await noteModal.close()

    childRow = group.childRow(childId2)
    noteModal = await childRow.openDailyNoteModal()
    await noteModal.openTab('group')
    await noteModal.assertNoGroupNote()
  })
})
