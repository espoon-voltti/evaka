// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import MobileChildPage from 'e2e-playwright/pages/mobile/child-page'
import MobileListPage from 'e2e-playwright/pages/mobile/list-page'
import PinLoginPage from 'e2e-playwright/pages/mobile/pin-login-page'
import TopNav from 'e2e-playwright/pages/mobile/top-nav'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { pairMobileDevice } from 'e2e-playwright/utils/mobile'
import {
  insertBackupPickups,
  insertChildFixtures,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertFamilyContacts,
  insertFridgeChildren,
  insertFridgePartners,
  resetDatabase,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  DaycareGroupBuilder,
  EmployeeBuilder,
  enduserChildFixtureJari,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import {
  Child,
  DaycarePlacement,
  PersonDetail
} from 'e2e-test-common/dev-api/types'
import LocalDate from 'lib-common/local-date'
import { Page } from '../../utils/page'

let page: Page
let fixtures: AreaAndPersonFixtures
let listPage: MobileListPage
let childPage: MobileChildPage
let pinLoginPage: PinLoginPage
let topNav: TopNav

const employeeId = uuidv4()
const daycareGroupPlacementId = uuidv4()

let daycarePlacementFixture: DaycarePlacement
let daycareGroup: DaycareGroupBuilder
let employee: EmployeeBuilder
let child: PersonDetail

const empFirstName = 'Yrjö'
const empLastName = 'Yksikkö'
const employeeName = `${empLastName} ${empFirstName}`
const childName =
  enduserChildFixtureJari.firstName + ' ' + enduserChildFixtureJari.lastName

const pin = '2580'

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  child = fixtures.enduserChildFixtureJari
  const unit = fixtures.daycareFixture

  employee = await Fixture.employee()
    .with({
      id: employeeId,
      externalId: `espooad: ${employeeId}`,
      firstName: empFirstName,
      lastName: empLastName,
      email: 'yy@example.com',
      roles: []
    })
    .save()

  await Fixture.employeePin().with({ userId: employee.data.id, pin }).save()
  daycareGroup = await Fixture.daycareGroup()
    .with({ daycareId: unit.id })
    .save()
  daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    child.id,
    unit.id
  )
  // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
  await setAclForDaycares(employee.data.externalId!, unit.id)
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
    await insertFridgePartners([
      {
        partnershipId: parentshipId,
        indx: 1,
        personId: fixtures.enduserGuardianFixture.id,
        startDate: LocalDate.today(),
        endDate: LocalDate.today()
      },
      {
        partnershipId: parentshipId,
        indx: 2,
        personId: fixtures.enduserChildJariOtherGuardianFixture.id,
        startDate: LocalDate.today(),
        endDate: LocalDate.today()
      }
    ])

    const contacts = [
      fixtures.enduserGuardianFixture,
      fixtures.enduserChildJariOtherGuardianFixture
    ]
    await insertFamilyContacts(
      contacts.map(({ id }, index) => ({
        id: uuidv4(),
        childId: child.id,
        contactPersonId: id,
        priority: index + 1
      }))
    )

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

    await insertBackupPickups(
      backupPickups.map(({ name, phone }) => ({
        id: uuidv4(),
        childId: child.id,
        name,
        phone
      }))
    )

    await insertFridgeChildren([
      {
        id: uuidv4(),
        childId: child.id,
        headOfChild: fixtures.enduserGuardianFixture.id,
        startDate: LocalDate.today(),
        endDate: LocalDate.today()
      }
    ])

    await listPage.selectChild(child.id)

    await childPage.openSensitiveInfo()
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
    await childPage.openSensitiveInfo()
    await pinLoginPage.login(employeeName, '9999')
    await pinLoginPage.assertWrongPinError()
    await pinLoginPage.submitPin(pin)
    await childPage.assertSensitiveInfoIsShown(childName)
  })

  test('PIN login is persistent', async () => {
    await listPage.selectChild(child.id)

    await childPage.openSensitiveInfo()
    // when user logs in
    await pinLoginPage.login(employeeName, pin)

    // then
    await childPage.assertSensitiveInfoIsShown(childName)
    await childPage.goBackFromSensitivePage()

    // when opened again, no login is required
    await childPage.openSensitiveInfo()
    await childPage.assertSensitiveInfoIsShown(childName)

    await childPage.goBackFromSensitivePage()
    await childPage.goBack()

    expect(await topNav.getUserInitials()).toEqual('YY')

    await topNav.openUserMenu()
    expect(await topNav.getFullName()).toEqual('Yrjö Yksikkö')

    // when user logs out
    await topNav.logout()
    await waitUntilEqual(() => topNav.getUserInitials(), '')

    await listPage.selectChild(child.id)
    await childPage.openSensitiveInfo()

    // then new login is required
    await pinLoginPage.login(employeeName, pin)
    await childPage.assertSensitiveInfoIsShown(childName)
  })
})
