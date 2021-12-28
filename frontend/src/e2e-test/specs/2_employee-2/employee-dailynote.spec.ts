// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  insertDefaultServiceNeedOptions,
  resetDatabase,
  postChildDailyNote
} from 'e2e-test-common/dev-api'
import { employeeLogin, seppoAdmin } from '../../config/users'
import { t } from 'testcafe'
import {
  CareAreaBuilder,
  DaycareBuilder,
  DaycareGroupBuilder,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import EmployeeHome from '../../pages/employee/home'
import UnitPage from '../../pages/employee/units/unit-page'
import { ChildDailyNoteBody } from 'lib-common/generated/api-types/note'

const employeeId = uuidv4()

let daycareGroup: DaycareGroupBuilder
let daycare: DaycareBuilder
let careArea: CareAreaBuilder

const employeeHome = new EmployeeHome()

fixture('Mobile employee daily notes')
  .meta({ type: 'regression', subType: 'mobile' })
  .beforeEach(async () => {
    await resetDatabase()
    const fixtures = await initializeAreaAndPersonData()

    await insertDefaultServiceNeedOptions()
    await Fixture.employee()
      .with({
        id: employeeId,
        externalId: `espooad: ${employeeId}`,
        roles: []
      })
      .save()

    careArea = await Fixture.careArea().save()
    daycare = await Fixture.daycare().careArea(careArea).save()
    daycareGroup = await Fixture.daycareGroup().daycare(daycare).save()

    const placementFixtureJari = await Fixture.placement()
      .with({
        childId: fixtures.enduserChildFixtureJari.id,
        unitId: daycare.data.id
      })
      .save()
    await Fixture.groupPlacement()
      .withGroup(daycareGroup)
      .withPlacement(placementFixtureJari)
      .save()

    const placementFixtureKaarina = await Fixture.placement()
      .with({
        childId: fixtures.enduserChildFixtureKaarina.id,
        unitId: daycare.data.id
      })
      .save()
    await Fixture.groupPlacement()
      .withGroup(daycareGroup)
      .withPlacement(placementFixtureKaarina)
      .save()

    await employeeLogin(t, seppoAdmin, config.employeeUrl)
    await employeeHome.navigateToUnits()
  })
  .afterEach(logConsoleMessages)

const unitPage = new UnitPage()

test('Child daycare daily note indicators are shown on group view and can be edited', async (t) => {
  const childId = enduserChildFixtureJari.id
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

  await postChildDailyNote(childId, daycareDailyNote)

  await unitPage.navigateHere(daycare.data.id)
  await unitPage.openTabGroups()
  await unitPage.openGroups()

  await t.click(unitPage.group(daycareGroup.data.id))

  await t
    .expect(
      unitPage
        .childInGroup(enduserChildFixtureJari.id)
        .find('[data-qa="daily-note"]').visible
    )
    .ok()

  await t
    .hover(unitPage.childDaycareDailyNoteIcon(enduserChildFixtureJari.id))
    .expect(
      unitPage.childDaycareDailyNoteHover(enduserChildFixtureJari.id)
        .textContent
    )
    .contains(daycareDailyNote.note || 'expected text not found')

  await t.click(unitPage.childDaycareDailyNoteIcon(enduserChildFixtureJari.id))
  await t
    .expect(unitPage.notesModal.noteInput.value)
    .eql(daycareDailyNote.note)
    .expect(unitPage.notesModal.sleepingHoursInput.value)
    .eql(hours)
    .expect(unitPage.notesModal.sleepingMinutesInput.value)
    .eql(minutes)
    .expect(unitPage.notesModal.reminderNoteInput.value)
    .eql(daycareDailyNote.reminderNote)

  await t.typeText(unitPage.notesModal.reminderNoteInput, 'aardvark', {
    replace: true
  })
  await t.click(unitPage.notesModal.submit)

  await t.click(unitPage.childDaycareDailyNoteIcon(enduserChildFixtureJari.id))
  await t.expect(unitPage.notesModal.reminderNoteInput.value).eql('aardvark')
})

test('Group daycare daily notes can be written and are shown on group notes tab', async (t) => {
  const childId = enduserChildFixtureJari.id
  const daycareDailyNote: ChildDailyNoteBody = {
    note: 'Toinen viesti',
    feedingNote: 'NONE',
    sleepingMinutes: null,
    sleepingNote: 'NONE',
    reminders: ['DIAPERS'],
    reminderNote: 'Muistakaa muistakaa!'
  }

  await postChildDailyNote(childId, daycareDailyNote)

  await unitPage.navigateHere(daycare.data.id)
  await unitPage.openTabGroups()
  await unitPage.openGroups()

  await t.click(unitPage.group(daycareGroup.data.id))
  await t.click(unitPage.groupDaycareDailyNoteLink)

  await t.typeText(unitPage.stickyNote.input, 'Ryhmälle viesti', {
    replace: true
  })
  await t.click(unitPage.stickyNote.save)
  await t.click(unitPage.notesModal.close)

  await t.click(unitPage.childDaycareDailyNoteIcon(enduserChildFixtureJari.id))
  await t.click(unitPage.notesModal.tab('group'))
  await t.expect(unitPage.stickyNote.note.textContent).eql('Ryhmälle viesti')
  await t.click(unitPage.notesModal.close)

  await t.click(
    unitPage.childDaycareDailyNoteIcon(enduserChildFixtureKaarina.id)
  )
  await t.click(unitPage.notesModal.tab('group'))
  await t.expect(unitPage.stickyNote.note.textContent).eql('Ryhmälle viesti')

  // Delete group note
  await t.click(unitPage.notesModal.close)
  await t.click(unitPage.groupDaycareDailyNoteLink)
  await t.click(unitPage.stickyNote.delete)
  await t.click(unitPage.notesModal.close)

  await t.click(unitPage.childDaycareDailyNoteIcon(enduserChildFixtureJari.id))
  await t.click(unitPage.notesModal.tab('group'))
  await t
    .expect(unitPage.stickyNote.note.with({ timeout: 1000 }).visible)
    .notOk()

  await t.click(unitPage.notesModal.close)

  await t.click(
    unitPage.childDaycareDailyNoteIcon(enduserChildFixtureKaarina.id)
  )
  await t
    .expect(unitPage.stickyNote.note.with({ timeout: 1000 }).visible)
    .notOk()
})
