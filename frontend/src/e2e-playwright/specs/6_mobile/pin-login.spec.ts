// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import LocalDate from 'lib-common/local-date'
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
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import {
  Child,
  DaycarePlacement,
  PersonDetail
} from 'e2e-test-common/dev-api/types'
import { newBrowserContext } from 'e2e-playwright/browser'
import { pairMobileDevice } from 'e2e-playwright/utils/mobile'
import MobileListPage from 'e2e-playwright/pages/mobile/list-page'
import MobileChildPage from 'e2e-playwright/pages/mobile/child-page'

let page: Page
let fixtures: AreaAndPersonFixtures
let listPage: MobileListPage
let childPage: MobileChildPage

const employeeId = uuidv4()
const daycareGroupPlacementId = uuidv4()

let daycarePlacementFixture: DaycarePlacement
let daycareGroup: DaycareGroupBuilder
let employee: EmployeeBuilder
let child: PersonDetail

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
      firstName: 'Yrjö',
      lastName: 'Yksikkö',
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

  page = await (await newBrowserContext()).newPage()
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)

  const mobileSignupUrl = await pairMobileDevice(
    employee.data.id!, // eslint-disable-line
    unit.id
  )
  await page.goto(mobileSignupUrl)
})
afterEach(async () => {
  await page.close()
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
    await childPage.openSensitiveInfoWithPinCode(
      `${employee.data.lastName} ${employee.data.firstName}`,
      pin
    )
    await childPage.assertSensitiveInfoIsShown(
      `${child.firstName} ${child.lastName}`
    )
    await childPage.assertSensitiveInfo(
      childAdditionalInfo,
      contacts,
      backupPickups
    )
  })

  test('Wrong pin shows error, and user can log in with correct pin after that', async () => {
    await listPage.selectChild(child.id)
    await childPage.openSensitiveInfoWithPinCode(
      `${employee.data.lastName} ${employee.data.firstName}`,
      '9999'
    )
    await childPage.assertWrongPinError()
    await childPage.goBack()
    await childPage.openSensitiveInfoWithPinCode(
      `${employee.data.lastName} ${employee.data.firstName}`,
      pin
    )
    await childPage.assertSensitiveInfoIsShown(
      `${child.firstName} ${child.lastName}`
    )
  })

  test('After successful PIN login user can log out after which new PIN is required', async () => {
    await listPage.selectChild(child.id)
    await childPage.openSensitiveInfoWithPinCode(
      `${employee.data.lastName} ${employee.data.firstName}`,
      pin
    )
    await childPage.assertSensitiveInfoIsShown(
      `${child.firstName} ${child.lastName}`
    )
    await childPage.goBack()
    await childPage.openSensitiveInfoWithPinCode(
      `${employee.data.lastName} ${employee.data.firstName}`,
      pin
    )
    await childPage.assertSensitiveInfoIsShown(
      `${child.firstName} ${child.lastName}`
    )
  })
})
