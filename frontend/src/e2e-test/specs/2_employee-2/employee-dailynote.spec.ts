// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  deleteEmployeeById,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertEmployeeFixture,
  postDaycareDailyNote
} from '../../dev-api'
import { seppoAdminRole } from '../../config/users'
import { t } from 'testcafe'
import {
  CareAreaBuilder,
  createDaycarePlacementFixture,
  DaycareBuilder,
  DaycareGroupBuilder,
  enduserChildFixtureJari,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { DaycareDailyNote, DaycarePlacement } from '../../dev-api/types'
import LocalDate from '@evaka/lib-common/local-date'
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

test('Child daycare daily note indicators are shown on group view', async (t) => {
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
})
