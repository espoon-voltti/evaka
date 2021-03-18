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
  deleteAbsences,
  deleteAttendances,
  deleteEmployeeById,
  deleteMobileDevice,
  deletePairing,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertEmployeeFixture,
  postMobileDevice
} from '../../dev-api'
import { mobileAutoSignInRole } from '../../config/users'
import { t } from 'testcafe'
import {
  CareAreaBuilder,
  createDaycarePlacementFixture,
  createPreschoolDaycarePlacementFixture,
  DaycareBuilder,
  DaycareGroupBuilder,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import MobileGroupsPage from '../../pages/employee/mobile/mobile-groups'
import ChildPage from '../../pages/employee/mobile/child-page'
import { DaycarePlacement } from '../../dev-api/types'
import LocalDate from '@evaka/lib-common/local-date'

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

const employeeId = uuidv4()
const mobileDeviceId = employeeId
const mobileLongTermToken = uuidv4()
const pairingId = uuidv4()

let daycarePlacementFixture: DaycarePlacement
let preschoolDaycarePlacementFixture: DaycarePlacement
let daycareGroup: DaycareGroupBuilder
let daycare: DaycareBuilder
let careArea: CareAreaBuilder

fixture('Mobile attendances')
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
      uuidv4(),
      fixtures.enduserChildFixtureJari.id,
      daycare.data.id,
      LocalDate.today().addMonths(3).addDays(1).formatIso(),
      LocalDate.today().addMonths(8).formatIso()
    )
    preschoolDaycarePlacementFixture = createPreschoolDaycarePlacementFixture(
      uuidv4(),
      fixtures.enduserChildFixtureJari.id,
      daycare.data.id,
      LocalDate.today().formatIso(),
      LocalDate.today().addMonths(3).formatIso()
    )

    await insertDaycarePlacementFixtures([
      daycarePlacementFixture,
      preschoolDaycarePlacementFixture
    ])
    await insertDaycareGroupPlacementFixtures([
      {
        id: uuidv4(),
        daycareGroupId: daycareGroup.data.id,
        daycarePlacementId: daycarePlacementFixture.id,
        startDate: daycarePlacementFixture.startDate,
        endDate: daycarePlacementFixture.endDate
      },
      {
        id: uuidv4(),
        daycareGroupId: daycareGroup.data.id,
        daycarePlacementId: preschoolDaycarePlacementFixture.id,
        startDate: preschoolDaycarePlacementFixture.startDate,
        endDate: preschoolDaycarePlacementFixture.endDate
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
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await deleteAttendances()
    await deleteAbsences()
  })
  .after(async () => {
    await deletePairing(pairingId)
    await deleteMobileDevice(mobileDeviceId)
    await cleanUp()
    await deleteEmployeeById(employeeId)
  })

const mobileGroupsPage = new MobileGroupsPage()
const childPage = new ChildPage()

test('Child is shown in the coming list in the beginning', async (t) => {
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
})

test('The basic case of marking child as present at 08:30 and leaving at 16:00 works', async () => {
  await childPage.markPresent(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage,
    '08:30'
  )
  await childPage.markDeparted(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage,
    '16:00'
  )
})

test('Child can be marked as absent for the whole day', async () => {
  await childPage.markAbsent(fixtures.enduserChildFixtureJari, mobileGroupsPage)
})

test('Child can be marked as present and returned to coming', async () => {
  await childPage.markPresent(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage,
    '08:30'
  )
  await childPage.returnToComing(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage
  )
})

test('User can undo the whole flow of marking present at 08:30 and leaving at 16:00', async () => {
  await childPage.markPresent(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage,
    '08:30'
  )
  await childPage.markDeparted(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage,
    '16:00'
  )
  await childPage.returnToPresent(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage
  )
  await childPage.returnToComing(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage
  )
})

test('User has to mark an absence if child arrives at 08:30 and leaves at 09:00', async () => {
  await childPage.markPresent(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage,
    '08:30'
  )
  await childPage.markDepartedAbsence(
    fixtures.enduserChildFixtureJari,
    mobileGroupsPage,
    '09:00'
  )
})
