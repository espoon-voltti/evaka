// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { DaycareDailyNoteBody } from 'lib-common/generated/api-types/messaging'
import { logConsoleMessages } from '../../utils/fixture'
import {
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertEmployeeFixture,
  postDaycareDailyNote,
  postMobileDevice,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { mobileLogin } from '../../config/users'
import { t } from 'testcafe'
import {
  CareAreaBuilder,
  createDaycarePlacementFixture,
  DaycareBuilder,
  DaycareGroupBuilder,
  enduserChildFixtureJari,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import MobileGroupsPage from '../../pages/employee/mobile/mobile-groups'
import ChildPage from '../../pages/employee/mobile/child-page'
import { DaycarePlacement } from 'e2e-test-common/dev-api/types'
import LocalDate from 'lib-common/local-date'

let fixtures: AreaAndPersonFixtures

const employeeId = uuidv4()
const mobileDeviceId = employeeId
const mobileLongTermToken = uuidv4()
const daycareGroupPlacementId = uuidv4()

let daycarePlacementFixture: DaycarePlacement
let daycareGroup: DaycareGroupBuilder
let daycare: DaycareBuilder
let careArea: CareAreaBuilder

fixture('Mobile daily notes')
  .meta({ type: 'regression', subType: 'mobile' })
  .beforeEach(async () => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()

    await Promise.all([
      insertEmployeeFixture({
        id: employeeId,
        externalId: `espooad: ${employeeId}`,
        firstName: 'Yrjö',
        lastName: 'Yksikkö',
        email: 'yy@example.com',
        roles: []
      })
    ])

    careArea = await Fixture.careArea().save()
    daycare = await Fixture.daycare().careArea(careArea).save()
    daycareGroup = await Fixture.daycareGroup().daycare(daycare).save()
    daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      fixtures.enduserChildFixtureJari.id,
      daycare.data.id
    )

    await insertDaycarePlacementFixtures([daycarePlacementFixture])
    await insertDaycareGroupPlacementFixtures([
      {
        id: daycareGroupPlacementId,
        daycareGroupId: daycareGroup.data.id,
        daycarePlacementId: daycarePlacementFixture.id,
        startDate: daycarePlacementFixture.startDate,
        endDate: daycarePlacementFixture.endDate
      }
    ])

    await postMobileDevice({
      id: mobileDeviceId,
      unitId: daycare.data.id,
      name: 'testMobileDevice',
      deleted: false,
      longTermToken: mobileLongTermToken
    })
    await mobileLogin(t, mobileLongTermToken)
  })
  .afterEach(logConsoleMessages)

const mobileGroupsPage = new MobileGroupsPage()
const childPage = new ChildPage()

test('Daycare groups are shown', async (t) => {
  const daycareDailyNote: DaycareDailyNoteBody = {
    groupId: daycareGroup.data.id,
    childId: enduserChildFixtureJari.id,
    date: LocalDate.today(),
    note: 'Testi viesti',
    feedingNote: 'MEDIUM',
    sleepingMinutes: null,
    sleepingNote: 'NONE',
    reminders: ['DIAPERS'],
    reminderNote: 'Ei enää pähkinöitä antaa saa'
  }

  await postDaycareDailyNote(daycareDailyNote)

  await t
    .expect(
      mobileGroupsPage.childName(fixtures.enduserChildFixtureJari.id)
        .textContent
    )
    .eql(
      `${fixtures.enduserChildFixtureJari.firstName} ${fixtures.enduserChildFixtureJari.lastName}`
    )

  await t
    .expect(
      mobileGroupsPage.childDailyNoteLink(fixtures.enduserChildFixtureJari.id)
        .visible
    )
    .ok()
})

test('Daycare group empty list indicator is shown', async (t) => {
  await t.click(mobileGroupsPage.presentTab)
  await t.expect(mobileGroupsPage.noChildrenIndicator.visible).ok()

  await t.click(mobileGroupsPage.comingTab)
  await t.expect(mobileGroupsPage.noChildrenIndicator.visible).ok()

  await t.click(mobileGroupsPage.departedTab)
  await t.expect(mobileGroupsPage.noChildrenIndicator.visible).ok()
})

test('User can delete a daily note for a child', async (t) => {
  const daycareDailyNote: DaycareDailyNoteBody = {
    groupId: null,
    childId: enduserChildFixtureJari.id,
    date: LocalDate.today(),
    note: 'Testi viesti',
    feedingNote: 'MEDIUM',
    sleepingMinutes: null,
    sleepingNote: 'NONE',
    reminders: ['DIAPERS'],
    reminderNote: 'Ei enää pähkinöitä antaa saa'
  }

  await postDaycareDailyNote(daycareDailyNote)

  await t.click(
    mobileGroupsPage.childDailyNoteLink(fixtures.enduserChildFixtureJari.id)
  )

  await childPage.deleteDailyNote()

  await t
    .expect(
      mobileGroupsPage.childDailyNoteLink(fixtures.enduserChildFixtureJari.id)
        .visible
    )
    .notOk()
})
