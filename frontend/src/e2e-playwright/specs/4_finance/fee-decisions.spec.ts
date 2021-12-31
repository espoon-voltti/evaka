// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  careArea2Fixture,
  daycare2Fixture,
  daycareFixture,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  familyWithTwoGuardians,
  feeDecisionsFixture,
  Fixture
} from 'e2e-test-common/dev-api/fixtures'
import {
  insertFeeDecisionFixtures,
  insertGuardianFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import config from 'e2e-test-common/config'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import {
  FeeDecisionDetailsPage,
  FeeDecisionsPage,
  FinancePage
} from 'e2e-playwright/pages/employee/finance/finance-page'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { PersonDetail } from '../../../e2e-test-common/dev-api/types'
import { Page } from '../../utils/page'

let page: Page
let feeDecisionsPage: FeeDecisionsPage

beforeEach(async () => {
  await resetDatabase()
  await initializeAreaAndPersonData()
  const careArea = await Fixture.careArea().with(careArea2Fixture).save()
  await Fixture.daycare().with(daycare2Fixture).careArea(careArea).save()
  const financeAdmin = await Fixture.employeeFinanceAdmin().save()

  page = await Page.open({ acceptDownloads: true })
  await employeeLogin(page, financeAdmin.data)
  await page.goto(config.employeeUrl)
})

const insertFeeDecisionFixtureAndNavigateToIt = async (
  headOfFamily: PersonDetail,
  child: PersonDetail,
  partner: PersonDetail | null = null
) => {
  await insertFeeDecisionFixtures([
    feeDecisionsFixture(
      'DRAFT',
      headOfFamily,
      child,
      daycareFixture.id,
      partner
    )
  ])

  await new EmployeeNav(page).openTab('finance')
  feeDecisionsPage = await new FinancePage(page).selectFeeDecisionsTab()
}

describe('Fee decisions', () => {
  test('List of fee decision drafts shows at least one row', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      enduserGuardianFixture,
      enduserChildFixtureKaarina
    )
    await waitUntilEqual(() => feeDecisionsPage.getFeeDecisionCount(), 1)
  })

  test('Navigate to and from decision details page', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      enduserGuardianFixture,
      enduserChildFixtureKaarina
    )
    await feeDecisionsPage.openFirstFeeDecision()
    await feeDecisionsPage.navigateBackFromDetails()
  })

  test('Fee decisions are toggled and sent', async () => {
    await insertFeeDecisionFixtureAndNavigateToIt(
      enduserGuardianFixture,
      enduserChildFixtureKaarina
    )
    await feeDecisionsPage.toggleAllFeeDecisions(true)
    await feeDecisionsPage.sendFeeDecisions()
    await feeDecisionsPage.assertSentDecisionsCount(1)
  })

  test('Partner is shown for elementary family', async () => {
    const partner = familyWithTwoGuardians.otherGuardian
    await insertGuardianFixtures([
      {
        guardianId: familyWithTwoGuardians.guardian.id,
        childId: familyWithTwoGuardians.children[0].id
      },
      { guardianId: partner.id, childId: familyWithTwoGuardians.children[0].id }
    ])
    await insertFeeDecisionFixtureAndNavigateToIt(
      familyWithTwoGuardians.guardian,
      familyWithTwoGuardians.children[0],
      partner
    )
    await feeDecisionsPage.openFirstFeeDecision()
    await new FeeDecisionDetailsPage(page).assertPartnerName(
      `${partner.firstName} ${partner.lastName}`
    )
  })

  test('Partner is not shown for non elementary family', async () => {
    const partner = familyWithTwoGuardians.otherGuardian
    await insertGuardianFixtures([
      {
        guardianId: familyWithTwoGuardians.guardian.id,
        childId: familyWithTwoGuardians.children[0].id
      }
    ])
    await insertFeeDecisionFixtureAndNavigateToIt(
      familyWithTwoGuardians.guardian,
      familyWithTwoGuardians.children[0],
      partner
    )
    await feeDecisionsPage.openFirstFeeDecision()
    await new FeeDecisionDetailsPage(page).assertPartnerNameNotShown()
  })
})
