// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import { mobileViewport } from '../../browser'
import {
  testChild,
  Fixture,
  uuidv4,
  testAdult2,
  testAdult,
  testDaycare,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createBackupPickup,
  createFamilyContact,
  createFridgeChild,
  createFridgePartner,
  resetServiceState
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import PinLoginPage from '../../pages/mobile/pin-login-page'
import TopNav from '../../pages/mobile/top-nav'
import { waitUntilEqual } from '../../utils'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let pinLoginPage: PinLoginPage
let topNav: TopNav

let child: DevPerson

const empFirstName = 'Yrjö'
const empLastName = 'Yksikkö'
const employeeName = `${empLastName} ${empFirstName}`
const childName = testChild.firstName + ' ' + testChild.lastName

const pin = '2580'

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  child = testChild
  const unit = testDaycare

  const employee = await Fixture.employee({
    firstName: empFirstName,
    lastName: empLastName,
    email: 'yy@example.com',
    roles: []
  })
    .withDaycareAcl(unit.id, 'UNIT_SUPERVISOR')
    .save()
  await Fixture.employeePin({ userId: employee.id, pin }).save()
  const daycareGroup = await Fixture.daycareGroup({ daycareId: unit.id }).save()
  const placementFixture = await Fixture.placement({
    childId: child.id,
    unitId: unit.id
  }).save()
  await Fixture.groupPlacement({
    daycareGroupId: daycareGroup.id,
    daycarePlacementId: placementFixture.id,
    startDate: placementFixture.startDate,
    endDate: placementFixture.endDate
  }).save()

  page = await Page.open({ viewport: mobileViewport })
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  pinLoginPage = new PinLoginPage(page)
  topNav = new TopNav(page)

  const mobileSignupUrl = await pairMobileDevice(unit.id)
  await page.goto(mobileSignupUrl)
})

describe('Mobile PIN login', () => {
  test('User can login with PIN and see child sensitive info', async () => {
    const childAdditionalInfo = await Fixture.childAdditionalInfo({
      id: child.id,
      allergies: 'Allergies',
      diet: 'Diets',
      medication: 'Medications',
      additionalInfo: ''
    }).save()
    await Fixture.person(testAdult2).saveAdult()

    const parentshipId = uuidv4()
    await createFridgePartner({
      body: [
        {
          partnershipId: parentshipId,
          indx: 1,
          otherIndx: 2,
          personId: testAdult.id,
          startDate: LocalDate.todayInSystemTz(),
          endDate: LocalDate.todayInSystemTz(),
          createdAt: HelsinkiDateTime.now(),
          conflict: false
        },
        {
          partnershipId: parentshipId,
          indx: 2,
          otherIndx: 1,
          personId: testAdult2.id,
          startDate: LocalDate.todayInSystemTz(),
          endDate: LocalDate.todayInSystemTz(),
          createdAt: HelsinkiDateTime.now(),
          conflict: false
        }
      ]
    })

    const contacts = [testAdult, testAdult2]
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
          headOfChild: testAdult.id,
          startDate: LocalDate.todayInSystemTz(),
          endDate: LocalDate.todayInSystemTz(),
          conflict: false
        }
      ]
    })

    await listPage.selectChild(child.id)

    await childPage.sensitiveInfoLink.click()
    await pinLoginPage.login(employeeName, pin)
    await childPage.assertBasicInfoIsShown(childName, contacts, backupPickups)
    await childPage.sensitiveInfo.diet.waitUntilHidden()
    await childPage.showSensitiveInfoButton.click()
    await childPage.sensitiveInfo.diet.waitUntilVisible()

    await childPage.assertSensitiveInfo(childAdditionalInfo)
  })

  test('Wrong pin shows error, and user can log in with correct pin after that', async () => {
    await listPage.selectChild(child.id)
    await childPage.sensitiveInfoLink.click()
    await pinLoginPage.login(employeeName, '9999')
    await pinLoginPage.assertWrongPinError()
    await pinLoginPage.submitPin(pin)
    await childPage.assertBasicInfoIsShown(childName, [], [])
  })

  test('PIN login is persistent', async () => {
    await listPage.selectChild(child.id)

    await childPage.sensitiveInfoLink.click()
    // when user logs in
    await pinLoginPage.login(employeeName, pin)

    // then
    await childPage.assertBasicInfoIsShown(childName, [], [])
    await childPage.goBackFromSensitivePage.click()

    // when opened again, no login is required
    await childPage.sensitiveInfoLink.click()
    await childPage.assertBasicInfoIsShown(childName, [], [])

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
    await childPage.assertBasicInfoIsShown(childName, [], [])
  })
})
