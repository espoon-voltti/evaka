// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import GuardianInformationPage from '../../pages/employee/guardian-information'
import ChildInformationPage from '../../pages/employee/child-information'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  insertDefaultServiceNeedOptions,
  insertFeeThresholds,
  insertVoucherValues,
  resetDatabase,
  runPendingAsyncJobs
  // runPendingAsyncJobs
} from 'e2e-test-common/dev-api'
import { PersonDetail } from 'e2e-test-common/dev-api/types'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import { Fixture } from 'e2e-test-common/dev-api/fixtures'
import { employeeLogin } from '../../utils/user'
import { Page } from 'e2e-playwright/utils/page'

let page: Page
let guardianInformation: GuardianInformationPage
let childInformation: ChildInformationPage

let fixtures: AreaAndPersonFixtures
let regularPerson: PersonDetail
let fridgePartner: PersonDetail
let child: PersonDetail

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertVoucherValues()
  regularPerson = fixtures.familyWithTwoGuardians.guardian
  fridgePartner = fixtures.familyWithTwoGuardians.otherGuardian
  child = fixtures.familyWithTwoGuardians.children[0]
  await insertFeeThresholds({
    validDuring: new DateRange(LocalDate.of(2020, 1, 1), null),
    minIncomeThreshold2: 210200,
    minIncomeThreshold3: 271300,
    minIncomeThreshold4: 308000,
    minIncomeThreshold5: 344700,
    minIncomeThreshold6: 381300,
    maxIncomeThreshold2: 479900,
    maxIncomeThreshold3: 541000,
    maxIncomeThreshold4: 577700,
    maxIncomeThreshold5: 614400,
    maxIncomeThreshold6: 651000,
    incomeMultiplier2: 0.107,
    incomeMultiplier3: 0.107,
    incomeMultiplier4: 0.107,
    incomeMultiplier5: 0.107,
    incomeMultiplier6: 0.107,
    incomeThresholdIncrease6Plus: 14200,
    siblingDiscount2: 0.5,
    siblingDiscount2Plus: 0.8,
    minFee: 2700,
    maxFee: 28900
  })

  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open()
  await employeeLogin(page, admin.data)

  guardianInformation = new GuardianInformationPage(page)
  childInformation = new ChildInformationPage(page)
})

describe('Employee - Head of family details', () => {
  test('guardian has restriction details enabled', async () => {
    await guardianInformation.navigateToGuardian(
      fixtures.restrictedPersonFixture.id
    )
    await guardianInformation.waitUntilLoaded()
    await guardianInformation.assertRestrictedDetails(true)
  })

  test('guardian does not have restriction details enabled', async () => {
    await guardianInformation.navigateToGuardian(regularPerson.id)
    await guardianInformation.waitUntilLoaded()
    await guardianInformation.assertRestrictedDetails(false)
  })

  test('Zero-year-old child is shown as age 0', async () => {
    await guardianInformation.navigateToGuardian(regularPerson.id)
    await guardianInformation.waitUntilLoaded()
    const children = await guardianInformation.openCollapsible('children')

    await children.addChild(
      fixtures.personFixtureChildZeroYearOld.firstName,
      '01.01.2020'
    )

    await children.verifyChildAge(0)

    const familyOverview = await guardianInformation.openCollapsible(
      'familyOverview'
    )
    await familyOverview.assertPerson({
      personId: fixtures.personFixtureChildZeroYearOld.id,
      age: 0
    })
  })

  test('Retroactive fee decisions can start before the minimum fee decision date', async () => {
    await guardianInformation.navigateToGuardian(regularPerson.id)
    const children = await guardianInformation.openCollapsible('children')
    await children.addChild(child.firstName, '01.01.2020')

    await childInformation.navigateToChild(child.id)
    const placements = await childInformation.openCollapsible('placements')
    await placements.createNewPlacement({
      unitName: fixtures.daycareFixture.name,
      startDate: '01.01.2020',
      endDate: '31.07.2020'
    })
    await runPendingAsyncJobs()

    await guardianInformation.navigateToGuardian(regularPerson.id)
    const feeDecisions = await guardianInformation.openCollapsible(
      'feeDecisions'
    )
    await feeDecisions.assertFeeDecision(0, {
      startDate: '01.03.2020',
      endDate: '31.07.2020',
      status: 'Luonnos'
    })

    await feeDecisions.createRetroactiveFeeDecisions('01.01.2020')
    await feeDecisions.assertFeeDecision(0, {
      startDate: '01.01.2020',
      endDate: '31.07.2020',
      status: 'Luonnos'
    })
  })

  test('Added partner is shown in family overview', async () => {
    await guardianInformation.navigateToGuardian(regularPerson.id)
    const partnersSection = await guardianInformation.openCollapsible(
      'partners'
    )
    await partnersSection.addPartner(fridgePartner.firstName, '01.01.2020')

    const familyOverview = await guardianInformation.openCollapsible(
      'familyOverview'
    )
    await familyOverview.assertPerson({ personId: fridgePartner.id })
  })

  test('Manually added income is shown in family overview', async () => {
    const totalIncome = 100000
    const employee = await Fixture.employee().save()
    await Fixture.income()
      .with({
        personId: regularPerson.id,
        effect: 'INCOME',
        data: {
          MAIN_INCOME: {
            amount: totalIncome,
            monthlyAmount: totalIncome,
            coefficient: 'MONTHLY_NO_HOLIDAY_BONUS'
          }
        },
        updatedBy: employee.data.id
      })
      .save()

    await guardianInformation.navigateToGuardian(regularPerson.id)
    const familyOverview = await guardianInformation.openCollapsible(
      'familyOverview'
    )
    await familyOverview.assertPerson({
      personId: regularPerson.id,
      incomeCents: totalIncome
    })
  })
})
