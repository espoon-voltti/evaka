// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import {
  execSimpleApplicationActions,
  getDecisionsByApplication,
  insertApplications,
  resetDatabase,
  runPendingAsyncJobs
} from 'e2e-test-common/dev-api'
import {
  applicationFixture,
  enduserChildFixtureJari,
  daycareFixture
} from 'e2e-test-common/dev-api/fixtures'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenDecisionResponsePage from '../../pages/citizen/citizen-decision-response'
import { format } from 'date-fns'

const citizenHomePage = new CitizenHomePage()
const citizenDecisionsPage = new CitizenDecisionsPage()
const citizenDecisionResponsePage = new CitizenDecisionResponsePage()

let applicationId: string
let fixtures: AreaAndPersonFixtures

fixture('Citizen decisions')
  .meta({ type: 'regression', subType: 'citizen-decisions' })
  .beforeEach(async () => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
  })
  .afterEach(logConsoleMessages)

test('Citizen sees her decisions, accepts preschool and rejects preschool daycare', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'PRESCHOOL',
    null,
    [daycareFixture.id],
    true
  )
  applicationId = application.id
  await insertApplications([application])

  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-decisions-without-proposal'
  ])
  await runPendingAsyncJobs()

  const decisions = await getDecisionsByApplication(applicationId)
  if (decisions.length !== 2) throw Error('Expected 2 decisions')
  const preschoolDecisionId = decisions.find((d) => d.type === 'PRESCHOOL')?.id
  if (!preschoolDecisionId)
    throw Error('Expected a decision with type PRESCHOOL')
  const preschoolDaycareDecisionId = decisions.find(
    (d) => d.type === 'PRESCHOOL_DAYCARE'
  )?.id
  if (!preschoolDaycareDecisionId)
    throw Error('Expected a decision with type PRESCHOOL_DAYCARE')

  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.decisions)

  await t
    .expect(citizenDecisionsPage.unresolvedDecisionsInfoBox.textContent)
    .contains('2 päätöstä odottaa vahvistusta')

  await citizenDecisionsPage.assertApplicationDecision(
    applicationId,
    preschoolDecisionId,
    `${enduserChildFixtureJari.firstName} ${enduserChildFixtureJari.lastName}`,
    'Päätös esiopetuksesta',
    format(new Date(), 'dd.MM.yyyy'),
    'Vahvistettavana huoltajalla'
  )

  await citizenDecisionsPage.assertApplicationDecision(
    applicationId,
    preschoolDaycareDecisionId,
    `${enduserChildFixtureJari.firstName} ${enduserChildFixtureJari.lastName}`,
    'Päätös liittyvästä varhaiskasvatuksesta',
    format(new Date(), 'dd.MM.yyyy'),
    'Vahvistettavana huoltajalla'
  )

  await t.click(citizenDecisionsPage.goRespondToDecisionBtn(applicationId))

  // === Response page for decisions of a single application ===

  await t
    .expect(citizenDecisionResponsePage.pageTitle.textContent)
    .contains('Päätökset')

  await citizenDecisionResponsePage.assertUnresolvedDecisionsNotification(2)

  // preschool_daycare decision cannot be accepted before accepting preschool
  await t
    .expect(
      citizenDecisionResponsePage
        .submitResponseBtn(preschoolDaycareDecisionId)
        .hasAttribute('disabled')
    )
    .ok()

  await citizenDecisionResponsePage.assertDecisionData(
    preschoolDecisionId,
    'Päätös esiopetuksesta',
    daycareFixture.decisionPreschoolName,
    'Vahvistettavana huoltajalla'
  )

  await citizenDecisionResponsePage.acceptDecision(preschoolDecisionId)

  await t
    .expect(
      citizenDecisionResponsePage.decisionStatus(preschoolDecisionId)
        .textContent
    )
    .eql('Hyväksytty')

  await citizenDecisionResponsePage.assertUnresolvedDecisionsNotification(1)

  await citizenDecisionResponsePage.assertDecisionData(
    preschoolDaycareDecisionId,
    'Päätös liittyvästä varhaiskasvatuksesta',
    daycareFixture.decisionDaycareName,
    'Vahvistettavana huoltajalla'
  )

  await citizenDecisionResponsePage.rejectDecision(preschoolDaycareDecisionId)
  await t
    .expect(
      citizenDecisionResponsePage.decisionStatus(preschoolDaycareDecisionId)
        .textContent
    )
    .eql('Hylätty')

  await citizenDecisionResponsePage.assertUnresolvedDecisionsNotification(0)
})

test('Rejecting preschool decision also rejects connected daycare after confirmation', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'PRESCHOOL',
    null,
    [daycareFixture.id],
    true
  )
  applicationId = application.id
  await insertApplications([application])

  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-decisions-without-proposal'
  ])
  await runPendingAsyncJobs()

  const decisions = await getDecisionsByApplication(applicationId)
  if (decisions.length !== 2) throw Error('Expected 2 decisions')
  const preschoolDecisionId = decisions.find((d) => d.type === 'PRESCHOOL')?.id
  if (!preschoolDecisionId)
    throw Error('Expected a decision with type PRESCHOOL')
  const preschoolDaycareDecisionId = decisions.find(
    (d) => d.type === 'PRESCHOOL_DAYCARE'
  )?.id
  if (!preschoolDaycareDecisionId)
    throw Error('Expected a decision with type PRESCHOOL_DAYCARE')

  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.decisions)
  await t.click(citizenDecisionsPage.goRespondToDecisionBtn(applicationId))
  await citizenDecisionResponsePage.rejectDecision(preschoolDecisionId)
  await citizenDecisionResponsePage.confirmRejectCascade()

  await t
    .expect(
      citizenDecisionResponsePage.decisionStatus(preschoolDecisionId)
        .textContent
    )
    .eql('Hylätty')
  await t
    .expect(
      citizenDecisionResponsePage.decisionStatus(preschoolDaycareDecisionId)
        .textContent
    )
    .eql('Hylätty')

  await citizenDecisionResponsePage.assertUnresolvedDecisionsNotification(0)
})
