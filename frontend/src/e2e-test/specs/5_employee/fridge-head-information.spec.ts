// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import AdminHome from '../../pages/home'
import EmployeeHome from '../../pages/employee/home'
import FridgeHeadInformationPage from '../../pages/employee/fridge-head-information/fridge-head-information-page'
import ChildInformationPage from '../../pages/employee/child-information/child-information-page'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  clearPricing,
  deletePricing,
  insertPricing,
  runPendingAsyncJobs
} from 'e2e-test-common/dev-api'
import { t } from 'testcafe'
import { seppoAdminRole } from '../../config/users'
import { PersonDetail } from 'e2e-test-common/dev-api/types'

const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()
const fridgeHeadInformation = new FridgeHeadInformationPage()
const childInformation = new ChildInformationPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
let regularPerson: PersonDetail
let fridgePartner: PersonDetail
let child: PersonDetail

const PRICING_ID = '99f976c2-2c0d-4acc-b10b-d46e2ecd951d'

fixture('Employee - Head of family details')
  .meta({ type: 'regression', subType: 'childinformation' })
  .page(adminHome.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    regularPerson = fixtures.familyWithTwoGuardians.guardian
    fridgePartner = fixtures.familyWithTwoGuardians.otherGuardian
    child = fixtures.familyWithTwoGuardians.children[0]
    await clearPricing()
    await insertPricing({
      id: PRICING_ID,
      validFrom: '2020-01-01',
      validTo: null,
      multiplier: '0.107',
      maxThresholdDifference: 2697,
      minThreshold2: 2102,
      minThreshold3: 2713,
      minThreshold4: 3080,
      minThreshold5: 3447,
      minThreshold6: 3813,
      thresholdIncrease6Plus: 142
    })
  })
  .beforeEach(async () => {
    await t.useRole(seppoAdminRole)
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUp()
    await deletePricing(PRICING_ID)
  })

test('guardian has restriction details enabled', async () => {
  await employeeHome.navigateToGuardianInformation(
    fixtures.restrictedPersonFixture.id
  )
  await fridgeHeadInformation.verifyRestrictedDetails(true)
})

test('guardian does not have restriction details enabled', async () => {
  await employeeHome.navigateToGuardianInformation(regularPerson.id)
  await fridgeHeadInformation.verifyRestrictedDetails(false)
})

test('Zero-year-old child is shown as age 0', async () => {
  await employeeHome.navigateToGuardianInformation(regularPerson.id)
  await fridgeHeadInformation.addChild({
    searchWord: fixtures.personFixtureChildZeroYearOld.firstName,
    startDate: '01.01.2020'
  })

  await fridgeHeadInformation.verifyFridgeChildAge({
    age: 0
  })

  await fridgeHeadInformation.verifyFamilyPerson({
    personId: fixtures.personFixtureChildZeroYearOld.id,
    age: 0
  })
})

test('Retroactive fee decisions can start before the minimum fee decision date', async () => {
  await employeeHome.navigateToGuardianInformation(regularPerson.id)
  await fridgeHeadInformation.addChild({
    searchWord: child.firstName,
    startDate: '01.01.2020'
  })

  await employeeHome.navigateToChildInformation(child.id)
  await childInformation.createNewPlacement({
    unitName: fixtures.daycareFixture.name,
    startDate: '01.01.2020',
    endDate: '31.07.2020'
  })

  await runPendingAsyncJobs()
  await employeeHome.navigateToGuardianInformation(regularPerson.id)
  await fridgeHeadInformation.verifyFeeDecision({
    startDate: '01.03.2020',
    endDate: '31.07.2020',
    status: 'Luonnos'
  })

  await fridgeHeadInformation.createRetroactiveFeeDecisions('01.01.2020')
  await fridgeHeadInformation.verifyFeeDecision({
    startDate: '01.01.2020',
    endDate: '31.07.2020',
    status: 'Luonnos'
  })
})

test('Added partner is shown n family overview', async () => {
  await employeeHome.navigateToGuardianInformation(regularPerson.id)
  await fridgeHeadInformation.addPartner({
    searchWord: fridgePartner.firstName,
    startDate: '01.01.2020'
  })

  await fridgeHeadInformation.verifyFamilyPerson({ personId: fridgePartner.id })
})

test('Manually added income is shown in family overview', async () => {
  const income = 1000

  await employeeHome.navigateToGuardianInformation(regularPerson.id)
  await fridgeHeadInformation.addIncome({
    mainIncome: income
  })

  await fridgeHeadInformation.verifyFamilyPerson({
    personId: regularPerson.id,
    incomeCents: income * 100
  })
})
