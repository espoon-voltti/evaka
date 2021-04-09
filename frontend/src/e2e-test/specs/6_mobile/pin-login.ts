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
  deleteDaycareDailyNotes,
  deleteEmployeeById,
  deleteMobileDevice,
  deletePairing,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  postMobileDevice,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import { mobileAutoSignInRole } from '../../config/users'
import { t } from 'testcafe'
import {
  CareAreaBuilder,
  createDaycarePlacementFixture,
  DaycareBuilder,
  DaycareGroupBuilder,
  EmployeeBuilder,
  enduserChildFixtureJari,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import MobileGroupsPage from '../../pages/employee/mobile/mobile-groups'
import ChildPage from '../../pages/employee/mobile/child-page'
import {
  ApplicationPersonDetail,
  DaycarePlacement
} from 'e2e-test-common/dev-api/types'

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
let employee: EmployeeBuilder

let child: ApplicationPersonDetail
const pin = '2580'

fixture('Mobile PIN login')
  .meta({ type: 'regression', subType: 'mobile' })
  .page(config.adminUrl)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()

    child = fixtures.enduserChildFixtureJari

    employee = await Fixture.employee()
      .with({
        id: employeeId,
        externalId: `espooad: ${employeeId}`,
        firstName: 'Yrjö',
        lastName: 'Yksikkö',
        email: 'yy@example.com',
        roles: [],
        pin: pin
      })
      .save()

    careArea = await Fixture.careArea().save()
    daycare = await Fixture.daycare().careArea(careArea).save()
    daycareGroup = await Fixture.daycareGroup().daycare(daycare).save()
    daycarePlacementFixture = createDaycarePlacementFixture(
      uuidv4(),
      child.id,
      daycare.data.id
    )

    await setAclForDaycares(employee.data.externalId, daycare.data.id)

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
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await deleteDaycareDailyNotes()
  })
  .after(async () => {
    await deletePairing(pairingId)
    await deleteMobileDevice(mobileDeviceId)
    await cleanUp()
    await deleteEmployeeById(employeeId)
  })

const mobileGroupsPage = new MobileGroupsPage()

test('User can login with PIN', async (t) => {
  await t
    .expect(mobileGroupsPage.childName(child.id).textContent)
    .eql(`${child.firstName} ${child.lastName}`)

  await t.click(mobileGroupsPage.childRow(enduserChildFixtureJari.id))
  await t.click(mobileGroupsPage.childSensitiveInfoLink)

  await t.click(mobileGroupsPage.pinLoginStaffSelector)
  await t
    .typeText(mobileGroupsPage.pinLoginStaffSelector, employee.data.lastName)
    .pressKey('tab')

  await t.typeText(mobileGroupsPage.pinInput, pin)
  await t.click(mobileGroupsPage.submitPin)

  await t
    .expect(mobileGroupsPage.childInfoName.textContent)
    .eql(`${child.firstName} ${child.lastName}`)

  await t
    .expect(mobileGroupsPage.childInfoChildAddress.textContent)
    .eql(child.streetAddress)
})
