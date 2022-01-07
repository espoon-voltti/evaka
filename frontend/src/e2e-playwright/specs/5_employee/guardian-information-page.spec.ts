// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  insertApplications,
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDecisionFixtures,
  insertInvoiceFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  applicationFixture,
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  decisionFixture,
  enduserChildFixtureJari,
  enduserGuardianFixture,
  Fixture,
  invoiceFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let page: Page

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const admin = await Fixture.employeeAdmin().save()

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    fixtures.enduserChildFixtureJari.id,
    daycareFixture.id
  )
  const application = applicationFixture(
    enduserChildFixtureJari,
    enduserGuardianFixture
  )

  const startDate = '2021-08-16'
  await insertDaycarePlacementFixtures([daycarePlacementFixture])
  await insertApplications([application])
  await insertDecisionFixtures([
    {
      ...decisionFixture(application.id, startDate, startDate),
      employeeId: admin.data.id
    }
  ])

  page = await Page.open()
  await employeeLogin(page, admin.data)
})

describe('Employee - Guardian Information', () => {
  test('guardian information is shown', async () => {
    const guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(fixtures.enduserGuardianFixture.id)

    const personInfoSection = guardianPage.getCollapsible('personInfo')
    await personInfoSection.assertPersonInfo(
      enduserGuardianFixture.lastName,
      enduserGuardianFixture.firstName,
      enduserGuardianFixture.ssn ?? ''
    )

    const expectedChildName = `${enduserChildFixtureJari.lastName} ${enduserChildFixtureJari.firstName}`
    const dependantsSection = await guardianPage.openCollapsible('dependants')
    await dependantsSection.assertContainsDependantChild(expectedChildName)

    const applicationsSection = await guardianPage.openCollapsible(
      'applications'
    )
    await applicationsSection.assertApplicationCount(1)
    await applicationsSection.assertApplicationSummary(
      0,
      expectedChildName,
      daycareFixture.name
    )

    const decisionsSection = await guardianPage.openCollapsible('decisions')
    await decisionsSection.assertDecisionCount(1)
    await decisionsSection.assertDecision(
      0,
      expectedChildName,
      daycareFixture.name,
      'Odottaa vastausta'
    )
  })

  test('Invoices are listed on the admin UI guardian page', async () => {
    await insertInvoiceFixtures([
      invoiceFixture(
        fixtures.enduserGuardianFixture.id,
        fixtures.enduserChildFixtureJari.id,
        fixtures.daycareFixture.id,
        'DRAFT',
        '2020-01-01',
        '2020-02-01'
      )
    ])

    const guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(fixtures.enduserGuardianFixture.id)

    const invoiceSection = await guardianPage.openCollapsible('invoices')
    await invoiceSection.assertInvoiceCount(1)
    await invoiceSection.assertInvoice(0, '01.01.2020', '01.02.2020', 'Luonnos')
  })
})
