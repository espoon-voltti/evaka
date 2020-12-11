// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { enduserRole } from '../../config/users'
import {
  deleteApplication,
  deleteEmployeeFixture,
  insertApplications,
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDecisionFixtures,
  insertEmployeeFixture
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  applicationFixture,
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  decisionFixture,
  enduserChildFixtureJari,
  enduserGuardianFixture,
  supervisor
} from '../../dev-api/fixtures'
import { DaycarePlacement } from '../../dev-api/types'
import EnduserPage from '../../pages/enduser/enduser-navigation'
import Home from '../../pages/home'
import { logConsoleMessages } from '../../utils/fixture'

const home = new Home()
const enduserPage = new EnduserPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

let daycarePlacementFixture: DaycarePlacement
let supervisorId: string
let decisionId: string
let applicationId: string

fixture('Enduser - Daycare decision')
  .meta({ type: 'regression', subType: 'daycare-decision' })
  .page(home.homePage('enduser'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    await insertDaycareGroupFixtures([daycareGroupFixture])
    supervisorId = await insertEmployeeFixture(supervisor)

    fixtures.enduserChildFixtureJari.firstName
    daycarePlacementFixture = createDaycarePlacementFixture(
      fixtures.enduserChildFixtureJari.id,
      daycareFixture.id
    )
    const application = applicationFixture(
      enduserChildFixtureJari,
      enduserGuardianFixture
    )
    applicationId = application.id
    await insertDaycarePlacementFixtures([daycarePlacementFixture])
    await insertApplications([application])
    const decision = decisionFixture(
      applicationId,
      application.form.preferredStartDate,
      application.form.preferredStartDate
    )
    decisionId = decision.id
    await insertDecisionFixtures([
      {
        ...decision,
        employeeId: supervisorId
      }
    ])
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    applicationId && (await deleteApplication(applicationId))
    await cleanUp()
    await deleteEmployeeFixture(config.supervisorExternalId)
  })

test('Enduser sees decisions listed by application when navigating from home to decisions directly', async (t) => {
  await t.useRole(enduserRole)
  await enduserPage.navigateDecisions()
  const decisions = enduserPage.getDecisionByApplicationId(applicationId)
  await t.expect(decisions.count).eql(1)
  await t
    .expect(decisions.nth(0).getAttribute('data-decision-id'))
    .eql(decisionId)
})

test('Enduser sees decision details when navigating directly to decision URL', async (t) => {
  await t.useRole(enduserRole)
  await enduserPage.navigateToDecision(decisionId)
  await enduserPage.assertDecisionDetails(
    fixtures.enduserChildFixtureJari.firstName,
    fixtures.enduserChildFixtureJari.lastName
  )
})
