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
  deleteMobileDevice,
  deletePairing,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertEmployeeFixture,
  postDaycareDailyNote,
  postMobileDevice
} from '../../dev-api'
import { mobileAutoSignInRole } from '../../config/users'
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
import MobileGroupsPage from '../../pages/employee/mobile/mobile-groups'
import { DaycareDailyNote, DaycarePlacement } from '../../dev-api/types'
import LocalDate from '@evaka/lib-common/src/local-date'

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

const employeeId = uuidv4()
const mobileDeviceId = employeeId
const mobileLongTermToken = uuidv4()
const pairingId = uuidv4()
const daycareGroupPlacementId = uuidv4()

let daycarePlacementFixture: DaycarePlacement
let daycareGroup: DaycareGroupBuilder
let daycare: DaycareBuilder
let careArea: CareAreaBuilder

fixture('Mobile daily notes')
  .meta({ type: 'regression', subType: 'mobile' })
  .page(config.adminUrl)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()

    await Promise.all([
      insertEmployeeFixture({
        id: employeeId,
        externalId: `espooad: ${employeeId}`,
        firstName: 'Yrjö',
        lastName: 'Yksikkö',
        email: 'yy@example.com',
        roles: ['MOBILE']
      })
    ])

    careArea = await Fixture.careArea().save()
    daycare = await Fixture.daycare().careArea(careArea).save()
    daycareGroup = await Fixture.daycareGroup().daycare(daycare).save()
    daycarePlacementFixture = createDaycarePlacementFixture(
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
  })
  .beforeEach(async () => {
    await t.useRole(mobileAutoSignInRole(mobileLongTermToken))
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await deletePairing(pairingId)
    await deleteMobileDevice(mobileDeviceId)
    await cleanUp()
    await deleteEmployeeById(employeeId)
  })

const mobileGroupsPage = new MobileGroupsPage()

test('Daycare groups are shown', async (t) => {
  const daycareDailyNote: DaycareDailyNote = {
    id: uuidv4(),
    groupId: daycareGroup.data.id,
    childId: enduserChildFixtureJari.id,
    date: LocalDate.today(),
    note: 'Testi viesti',
    feedingNote: 'MEDIUM',
    sleepingNote: 'NONE',
    reminders: ['DIAPERS'],
    reminderNote: 'Ei enää pähkinöitä antaa saa',
    modifiedBy: 'e2e-test'
  }

  await postDaycareDailyNote(daycareDailyNote)

  await t.expect(mobileGroupsPage.allGroups.visible).ok()

  await t
    .expect(mobileGroupsPage.groupButton(daycareGroup.data.id).visible)
    .ok()

  await t.click(mobileGroupsPage.groupButton(daycareGroup.data.id))

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
      mobileGroupsPage.childStatus(fixtures.enduserChildFixtureJari.id)
        .textContent
    )
    .contains('Tulossa')

  await t
    .expect(
      mobileGroupsPage.childDailyNoteLink(fixtures.enduserChildFixtureJari.id)
        .visible
    )
    .ok()
})
