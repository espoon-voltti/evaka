// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  deleteEmployeeById,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertEmployeeFixture,
  postDaycareDailyNote
} from 'e2e-test-common/dev-api'
import { seppoAdminRole } from '../../config/users'
import { t } from 'testcafe'
import {
  CareAreaBuilder,
  createDaycarePlacementFixture,
  DaycareBuilder,
  DaycareGroupBuilder,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import {
  DaycareDailyNote,
  DaycarePlacement
} from 'e2e-test-common/dev-api/types'
import LocalDate from 'lib-common/local-date'
import EmployeeHome from '../../pages/employee/home'
import UnitPage from '../../pages/employee/units/unit-page'

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

const employeeId = uuidv4()

let daycarePlacementFixtureJari: DaycarePlacement
let daycarePlacementFixtureKaarina: DaycarePlacement

let daycareGroup: DaycareGroupBuilder
let daycare: DaycareBuilder
let careArea: CareAreaBuilder

const employeeHome = new EmployeeHome()

fixture('Mobile daily notes')
  .meta({ type: 'regression', subType: 'mobile' })
  .page(config.adminUrl)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()

    await insertEmployeeFixture({
      id: employeeId,
      externalId: `espooad: ${employeeId}`,
      firstName: 'Yrjö',
      lastName: 'Yksikkö',
      email: 'yy@example.com',
      roles: ['MOBILE']
    })

    careArea = await Fixture.careArea().save()
    daycare = await Fixture.daycare().careArea(careArea).save()
    daycareGroup = await Fixture.daycareGroup().daycare(daycare).save()
    daycarePlacementFixtureJari = createDaycarePlacementFixture(
      uuidv4(),
      fixtures.enduserChildFixtureJari.id,
      daycare.data.id
    )

    daycarePlacementFixtureKaarina = createDaycarePlacementFixture(
      uuidv4(),
      fixtures.enduserChildFixtureKaarina.id,
      daycare.data.id
    )

    await insertDaycarePlacementFixtures([
      daycarePlacementFixtureJari,
      daycarePlacementFixtureKaarina
    ])
    await insertDaycareGroupPlacementFixtures([
      {
        id: uuidv4(),
        daycareGroupId: daycareGroup.data.id,
        daycarePlacementId: daycarePlacementFixtureJari.id,
        startDate: daycarePlacementFixtureJari.startDate,
        endDate: daycarePlacementFixtureJari.endDate
      },
      {
        id: uuidv4(),
        daycareGroupId: daycareGroup.data.id,
        daycarePlacementId: daycarePlacementFixtureKaarina.id,
        startDate: daycarePlacementFixtureKaarina.startDate,
        endDate: daycarePlacementFixtureKaarina.endDate
      }
    ])
  })
  .beforeEach(async () => {
    await t.useRole(seppoAdminRole)
    await employeeHome.navigateToUnits()
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUp()
    await deleteEmployeeById(employeeId)
    await Fixture.cleanup()
  })

const unitPage = new UnitPage()

test('Child daycare daily note indicators are shown on group view and can be edited', async (t) => {
  const daycareDailyNote: DaycareDailyNote = {
    id: uuidv4(),
    groupId: null,
    childId: enduserChildFixtureJari.id,
    date: LocalDate.today(),
    note: 'Testi viesti',
    feedingNote: 'MEDIUM',
    sleepingHours: '2',
    sleepingNote: 'NONE',
    reminders: ['DIAPERS'],
    reminderNote: 'Ei enää pähkinöitä antaa saa',
    modifiedBy: 'e2e-test'
  }

  await postDaycareDailyNote(daycareDailyNote)

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
    .expect(unitPage.daycareDailyNoteModal.noteInput.value)
    .eql(daycareDailyNote.note)
    .expect(unitPage.daycareDailyNoteModal.sleepingHoursInput.value)
    .eql(daycareDailyNote.sleepingHours)
    .expect(unitPage.daycareDailyNoteModal.reminderNoteInput.value)
    .eql(daycareDailyNote.reminderNote)

  await t.typeText(
    unitPage.daycareDailyNoteModal.reminderNoteInput,
    'aardvark',
    { replace: true }
  )
  await t.click(unitPage.daycareDailyNoteModal.submit)

  await t.click(unitPage.childDaycareDailyNoteIcon(enduserChildFixtureJari.id))
  await t
    .expect(unitPage.daycareDailyNoteModal.reminderNoteInput.value)
    .eql('aardvark')
})

test('Group daycare daily notes can be written and are shown on child notes', async (t) => {
  const daycareDailyNote: DaycareDailyNote = {
    id: uuidv4(),
    groupId: null,
    childId: enduserChildFixtureJari.id,
    date: LocalDate.today(),
    note: 'Toinen viesti',
    feedingNote: 'NONE',
    sleepingHours: '',
    sleepingNote: 'NONE',
    reminders: ['DIAPERS'],
    reminderNote: 'Muistakaa muistakaa!',
    modifiedBy: 'e2e-test'
  }

  await postDaycareDailyNote(daycareDailyNote)

  await unitPage.navigateHere(daycare.data.id)
  await unitPage.openTabGroups()
  await unitPage.openGroups()

  await t.click(unitPage.group(daycareGroup.data.id))
  await t.click(unitPage.groupDaycareDailyNoteLink)

  await t.typeText(
    unitPage.daycareDailyNoteModal.groupNoteInput,
    'Ryhmälle viesti',
    { replace: true }
  )
  await t.click(unitPage.daycareDailyNoteModal.submit)

  await t.click(unitPage.childDaycareDailyNoteIcon(enduserChildFixtureJari.id))
  await t
    .expect(unitPage.daycareDailyNoteModal.childGroupNote.textContent)
    .eql('Ryhmälle viesti')

  await t.click(unitPage.daycareDailyNoteModal.cancel)

  await t.click(
    unitPage.childDaycareDailyNoteIcon(enduserChildFixtureKaarina.id)
  )
  await t
    .expect(unitPage.daycareDailyNoteModal.childGroupNote.textContent)
    .eql('Ryhmälle viesti')

  // Delete group note
  await t.click(unitPage.daycareDailyNoteModal.cancel)
  await t.click(unitPage.groupDaycareDailyNoteLink)
  await t.click(unitPage.daycareDailyNoteModal.delete)

  await t.click(unitPage.childDaycareDailyNoteIcon(enduserChildFixtureJari.id))
  await t
    .expect(
      unitPage.daycareDailyNoteModal.childGroupNote.with({ timeout: 2000 })
        .visible
    )
    .notOk()

  await t.click(unitPage.daycareDailyNoteModal.cancel)

  await t.click(
    unitPage.childDaycareDailyNoteIcon(enduserChildFixtureKaarina.id)
  )
  await t
    .expect(
      unitPage.daycareDailyNoteModal.childGroupNote.with({ timeout: 2000 })
        .visible
    )
    .notOk()
})
