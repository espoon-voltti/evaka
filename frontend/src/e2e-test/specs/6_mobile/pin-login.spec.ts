// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import { insertChildFixtures } from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  enduserChildFixtureJari,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { Child, PersonDetail } from '../../dev-api/types'
import {
  createBackupPickup,
  createFamilyContact,
  createFridgeChild,
  createFridgePartner,
  resetServiceState
} from '../../generated/api-clients'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import PinLoginPage from '../../pages/mobile/pin-login-page'
import TopNav from '../../pages/mobile/top-nav'
import { waitUntilEqual } from '../../utils'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let fixtures: AreaAndPersonFixtures
let listPage: MobileListPage
let childPage: MobileChildPage
let pinLoginPage: PinLoginPage
let topNav: TopNav

let child: PersonDetail

const empFirstName = 'Yrjö'
const empLastName = 'Yksikkö'
const employeeName = `${empLastName} ${empFirstName}`
const childName =
  enduserChildFixtureJari.firstName + ' ' + enduserChildFixtureJari.lastName

const pin = '2580'

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()
  child = fixtures.enduserChildFixtureJari
  const unit = fixtures.daycareFixture

  const employee = await Fixture.employee()
    .with({
      firstName: empFirstName,
      lastName: empLastName,
      email: 'yy@example.com',
      roles: []
    })
    .withDaycareAcl(unit.id, 'UNIT_SUPERVISOR')
    .save()
  await Fixture.employeePin().with({ userId: employee.data.id, pin }).save()
  const daycareGroup = await Fixture.daycareGroup()
    .with({ daycareId: unit.id })
    .save()
  const placementFixture = await Fixture.placement()
    .with({ childId: child.id, unitId: unit.id })
    .save()
  await Fixture.groupPlacement()
    .withGroup(daycareGroup)
    .withPlacement(placementFixture)
    .save()

  page = await Page.open()
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  pinLoginPage = new PinLoginPage(page)
  topNav = new TopNav(page)

  const mobileSignupUrl = await pairMobileDevice(unit.id)
  await page.goto(mobileSignupUrl)
})

describe('Mobile PIN login', () => {
  test('User can login with PIN and see child sensitive info', async () => {
    const childAdditionalInfo: Child = {
      id: child.id,
      allergies: 'Allergies',
      diet: 'Diets',
      medication: 'Medications'
    }

    await insertChildFixtures([childAdditionalInfo])

    const parentshipId = uuidv4()
    await createFridgePartner({
      body: [
        {
          partnershipId: parentshipId,
          indx: 1,
          otherIndx: 2,
          personId: fixtures.enduserGuardianFixture.id,
          startDate: LocalDate.todayInSystemTz(),
          endDate: LocalDate.todayInSystemTz(),
          createdAt: HelsinkiDateTime.now(),
          conflict: false
        },
        {
          partnershipId: parentshipId,
          indx: 2,
          otherIndx: 1,
          personId: fixtures.enduserChildJariOtherGuardianFixture.id,
          startDate: LocalDate.todayInSystemTz(),
          endDate: LocalDate.todayInSystemTz(),
          createdAt: HelsinkiDateTime.now(),
          conflict: false
        }
      ]
    })

    const contacts = [
      fixtures.enduserGuardianFixture,
      fixtures.enduserChildJariOtherGuardianFixture
    ]
    await createFamilyContact({
      body: contacts.map(({ id }, index) => ({
        id: uuidv4(),
        childId: child.id,
        contactPersonId: id,
        priority: index + 1
      }))
    })

    const backupPickups = [
      {
        name: 'Backup pickup 1',
        phone: '1'
      },
      {
        name: 'Backup pickup 2',
        phone: '2'
      }
    ]

    await createBackupPickup({
      body: backupPickups.map(({ name, phone }) => ({
        id: uuidv4(),
        childId: child.id,
        name,
        phone
      }))
    })

    await createFridgeChild({
      body: [
        {
          id: uuidv4(),
          childId: child.id,
          headOfChild: fixtures.enduserGuardianFixture.id,
          startDate: LocalDate.todayInSystemTz(),
          endDate: LocalDate.todayInSystemTz(),
          conflict: false
        }
      ]
    })

    await listPage.selectChild(child.id)

    await childPage.sensitiveInfoLink.click()
    await pinLoginPage.login(employeeName, pin)
    await childPage.assertSensitiveInfoIsShown(childName)
    await childPage.assertSensitiveInfo(
      childAdditionalInfo,
      contacts,
      backupPickups
    )
  })

  test('Wrong pin shows error, and user can log in with correct pin after that', async () => {
    await listPage.selectChild(child.id)
    await childPage.sensitiveInfoLink.click()
    await pinLoginPage.login(employeeName, '9999')
    await pinLoginPage.assertWrongPinError()
    await pinLoginPage.submitPin(pin)
    await childPage.assertSensitiveInfoIsShown(childName)
  })

  test('PIN login is persistent', async () => {
    await listPage.selectChild(child.id)

    await childPage.sensitiveInfoLink.click()
    // when user logs in
    await pinLoginPage.login(employeeName, pin)

    // then
    await childPage.assertSensitiveInfoIsShown(childName)
    await childPage.goBackFromSensitivePage.click()

    // when opened again, no login is required
    await childPage.sensitiveInfoLink.click()
    await childPage.assertSensitiveInfoIsShown(childName)

    await childPage.goBackFromSensitivePage.click()
    await childPage.goBack.click()

    expect(await topNav.getUserInitials()).toEqual('YY')

    await topNav.openUserMenu()
    expect(await topNav.getFullName()).toEqual('Yrjö Yksikkö')

    // when user logs out
    await topNav.logout()
    await waitUntilEqual(() => topNav.getUserInitials(), '')

    await listPage.selectChild(child.id)
    await childPage.sensitiveInfoLink.click()

    // then new login is required
    await pinLoginPage.login(employeeName, pin)
    await childPage.assertSensitiveInfoIsShown(childName)
  })
})
