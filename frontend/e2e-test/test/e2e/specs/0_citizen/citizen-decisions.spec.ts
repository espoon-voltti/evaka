// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  deleteApplication,
  execSimpleApplicationActions,
  getDecisionsByApplication,
  insertApplications,
  runPendingAsyncJobs
} from '../../dev-api'
import {
  applicationFixture,
  enduserChildFixtureJari,
  Fixture,
  daycareFixture
} from '../../dev-api/fixtures'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenDecisionResponsePage from '../../pages/citizen/citizen-decision-response'
import { format } from 'date-fns'

const citizenHomePage = new CitizenHomePage()
const citizenDecisionsPage = new CitizenDecisionsPage()
const citizenDecisionResponsePage = new CitizenDecisionResponsePage()

let applicationId: string
let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Citizen decisions')
  .meta({ type: 'regression', subType: 'citizen-decisions' })
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await deleteApplication(applicationId)
  })
  .after(async () => {
    await Fixture.cleanup()
    await cleanUp()
  })

test('Citizen sees her decisions, accepts preschool and rejects preschool daycare', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'preschool',
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
    format(new Date(application.form.preferredStartDate), 'dd.MM.yyyy'),
    'Vahvistettavana huoltajalla'
  )

  await citizenDecisionsPage.assertApplicationDecision(
    applicationId,
    preschoolDaycareDecisionId,
    `${enduserChildFixtureJari.firstName} ${enduserChildFixtureJari.lastName}`,
    'Päätös liittyvästä varhaiskasvatuksesta',
    format(new Date(application.form.preferredStartDate), 'dd.MM.yyyy'),
    'Vahvistettavana huoltajalla'
  )

  await t.click(citizenDecisionsPage.goRespondToDecisionBtn(applicationId))

  // === Response page for decisions of a single application ===

  await t
    .expect(citizenDecisionResponsePage.pageTitle.textContent)
    .contains('Päätökset')

  await t
    .expect(citizenDecisionResponsePage.unresolvedDecisionsInfoBox.textContent)
    .contains('2 päätöstä odottaa vahvistusta')

  // preschool_daycare decision cannot be accepted before accepting preschool
  await t
    .expect(
      citizenDecisionResponsePage
        .submitResponseBtn(preschoolDaycareDecisionId)
        .hasAttribute('disabled')
    )
    .ok()

  // accept preschool decision
  await t
    .expect(
      citizenDecisionResponsePage.decisionTitle(preschoolDecisionId).textContent
    )
    .eql('Päätös esiopetuksesta')
  await t
    .expect(
      citizenDecisionResponsePage.decisionUnit(preschoolDecisionId).textContent
    )
    .eql(daycareFixture.decisionPreschoolName)
  await t
    .expect(
      citizenDecisionResponsePage.decisionStatus(preschoolDecisionId)
        .textContent
    )
    .eql('Vahvistettavana huoltajalla')
  await t.click(citizenDecisionResponsePage.acceptRadioBtn(preschoolDecisionId))
  await t.click(
    citizenDecisionResponsePage.submitResponseBtn(preschoolDecisionId)
  )
  await t
    .expect(
      citizenDecisionResponsePage.decisionStatus(preschoolDecisionId)
        .textContent
    )
    .eql('Hyväksytty')

  await t
    .expect(citizenDecisionResponsePage.unresolvedDecisionsInfoBox.textContent)
    .contains('1 päätös odottaa vahvistusta')

  // reject preschool_daycare decision
  await t
    .expect(
      citizenDecisionResponsePage.decisionTitle(preschoolDaycareDecisionId)
        .textContent
    )
    .eql('Päätös liittyvästä varhaiskasvatuksesta')
  await t
    .expect(
      citizenDecisionResponsePage.decisionUnit(preschoolDaycareDecisionId)
        .textContent
    )
    .eql(daycareFixture.decisionDaycareName)
  await t
    .expect(
      citizenDecisionResponsePage.decisionStatus(preschoolDaycareDecisionId)
        .textContent
    )
    .eql('Vahvistettavana huoltajalla')
  await t.click(
    citizenDecisionResponsePage.rejectRadioBtn(preschoolDaycareDecisionId)
  )
  await t.click(
    citizenDecisionResponsePage.submitResponseBtn(preschoolDaycareDecisionId)
  )
  await t
    .expect(
      citizenDecisionResponsePage.decisionStatus(preschoolDaycareDecisionId)
        .textContent
    )
    .eql('Hylätty')

  await t
    .expect(citizenDecisionResponsePage.unresolvedDecisionsInfoBox.exists)
    .notOk()
})

test('Rejecting preschool decision also rejects connected daycare after confirmation', async (t) => {
  const application = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture,
    undefined,
    'preschool',
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

  // === Response page for decisions of a single application ===

  // reject preschool decision
  await t.click(citizenDecisionResponsePage.rejectRadioBtn(preschoolDecisionId))
  await t.click(
    citizenDecisionResponsePage.submitResponseBtn(preschoolDecisionId)
  )
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
  await t
    .expect(citizenDecisionResponsePage.unresolvedDecisionsInfoBox.exists)
    .notOk()
})
