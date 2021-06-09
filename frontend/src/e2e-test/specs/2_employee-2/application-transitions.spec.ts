// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import {
  applicationFixture,
  decisionFixture
} from 'e2e-test-common/dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { EmployeeDetail } from 'e2e-test-common/dev-api/types'
import {
  cleanUpMessages,
  createDecisionPdf,
  execSimpleApplicationAction,
  execSimpleApplicationActions,
  insertApplications,
  insertDecisionFixtures,
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares as setAclForDaycare
} from 'e2e-test-common/dev-api'
import { employeeLogin, seppoAdmin, seppoManager } from '../../config/users'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import UnitPage from '../../pages/employee/units/unit-page'
import { addWeeks, format } from 'date-fns'
import { Selector } from 'testcafe'

const applicationWorkbench = new ApplicationWorkbenchPage()
const applicationReadView = new ApplicationReadView()
const unitPage = new UnitPage()

const supervisor: EmployeeDetail = {
  id: '552e5bde-92fb-4807-a388-40016f85f593',
  externalId: config.supervisorExternalId,
  firstName: 'Eeva',
  lastName: 'Esimies',
  email: 'eeva.esimies@espoo.fi',
  roles: ['SERVICE_WORKER', 'ADMIN']
}

let fixtures: AreaAndPersonFixtures
let applicationId: string
let applicationId2: string
let supervisorId: string

fixture('Application-transitions')
  .meta({ type: 'regression', subType: 'daycare-application' })
  .beforeEach(async () => {
    await resetDatabase()
    await cleanUpMessages()
    fixtures = await initializeAreaAndPersonData()

    const uniqueSupervisor = {
      ...supervisor,
      email: `${Math.random().toString(36).substring(7)}@espoo.fi`
    }
    supervisorId = await insertEmployeeFixture(uniqueSupervisor)
    await setAclForDaycare(
      config.supervisorExternalId,
      fixtures.daycareFixture.id
    )
  })
  .afterEach(logConsoleMessages)

test('Supervisor accepts decision on behalf of the enduser', async (t) => {
  const fixture = {
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture
    ),
    status: 'SENT' as const
  }
  applicationId = fixture.id

  await insertApplications([fixture])
  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-decisions-without-proposal'
  ])

  await employeeLogin(t, seppoManager)
  await applicationReadView.openApplicationByLink(applicationId)
  await applicationReadView.acceptDecision('DAYCARE')

  await t
    .expect(applicationReadView.applicationStatus.innerText)
    .contains('Paikka vastaanotettu')
})

test('Supervisor accepts decision on behalf of the enduser and forwards start date 2 weeks', async (t) => {
  const fixture = {
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture
    ),
    status: 'SENT' as const
  }
  applicationId = fixture.id

  await insertApplications([fixture])
  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-decisions-without-proposal'
  ])

  await employeeLogin(t, seppoManager)
  await applicationReadView.openApplicationByLink(applicationId)
  await applicationReadView.setDecisionStartDate(
    'DAYCARE',
    format(addWeeks(new Date(), 2), 'dd.MM.yyyy')
  )

  await applicationReadView.acceptDecision('DAYCARE')
  await t
    .expect(applicationReadView.applicationStatus.innerText)
    .contains('Paikka vastaanotettu')
})

test('Sending decision sets application to waiting confirmation -state', async (t) => {
  const fixture = {
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture
    ),
    status: 'SENT' as const
  }
  applicationId = fixture.id

  await insertApplications([fixture])
  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan'
  ])

  await employeeLogin(t, seppoAdmin, ApplicationListView.url)

  await applicationWorkbench.openDecisionQueue()
  await applicationWorkbench.sendDecisionsWithoutProposal(applicationId)

  await applicationReadView.openApplicationByLink(applicationId)
  await t
    .expect(applicationReadView.applicationStatus.innerText)
    .contains('Vahvistettavana huoltajalla')
})

test('Accepting decision for non vtj guardiam sets application to waiting mailing -state', async (t) => {
  const fixture = {
    ...applicationFixture(
      fixtures.enduserChildFixtureKaarina,
      fixtures.familyWithTwoGuardians.guardian
    ),
    status: 'SENT' as const
  }
  applicationId = fixture.id

  await insertApplications([fixture])
  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan'
  ])

  await employeeLogin(t, seppoAdmin, ApplicationListView.url)

  await applicationWorkbench.openDecisionQueue()
  await applicationWorkbench.sendDecisionsWithoutProposal(applicationId)

  await applicationReadView.openApplicationByLink(applicationId)
  await t
    .expect(applicationReadView.applicationStatus.innerText)
    .contains('Odottaa postitusta')
})

test('Placement dialog shows warning if guardian has restricted details', async (t) => {
  const restrictedDetailsGuardianApplication = {
    ...applicationFixture(
      fixtures.familyWithRestrictedDetailsGuardian.children[0],
      fixtures.familyWithRestrictedDetailsGuardian.guardian,
      fixtures.familyWithRestrictedDetailsGuardian.otherGuardian,
      'DAYCARE',
      'NOT_AGREED'
    ),
    id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
  }
  const applicationId = restrictedDetailsGuardianApplication.id

  await insertApplications([restrictedDetailsGuardianApplication])

  await execSimpleApplicationAction(applicationId, 'move-to-waiting-placement')

  await employeeLogin(t, seppoAdmin, ApplicationListView.url)

  await applicationWorkbench.openPlacementQueue()

  await applicationWorkbench.openDaycarePlacementDialogById(applicationId)

  await t
    .expect(Selector('[data-qa="restricted-details-warning"]').visible)
    .ok()
})

test('Placement proposal flow', async (t) => {
  const fixture1 = {
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.familyWithTwoGuardians.guardian
    ),
    status: 'SENT' as const
  }
  applicationId = fixture1.id

  applicationId2 = 'dd54782e-231c-4014-abaf-a63eed4e2627'
  const fixture2 = {
    ...applicationFixture(
      fixtures.enduserChildFixtureKaarina,
      fixtures.familyWithSeparatedGuardians.guardian
    ),
    status: 'SENT' as const,
    id: applicationId2
  }

  await insertApplications([fixture1, fixture2])
  await execSimpleApplicationActions(applicationId, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-placement-proposal'
  ])
  await execSimpleApplicationActions(applicationId2, [
    'move-to-waiting-placement',
    'create-default-placement-plan',
    'send-placement-proposal'
  ])

  await employeeLogin(t, seppoManager)
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabPlacementProposals()

  await t
    .expect(unitPage.placementProposalsAcceptButton.hasAttribute('disabled'))
    .ok()

  await unitPage.clickProposalAccept(applicationId)
  await t
    .expect(unitPage.placementProposalsAcceptButton.hasAttribute('disabled'))
    .ok()

  await unitPage.clickProposalAccept(applicationId2)
  await t
    .expect(unitPage.placementProposalsAcceptButton.hasAttribute('disabled'))
    .notOk()

  await unitPage.clickProposalReject(applicationId2)
  await unitPage.selectProposalRejectionReason(0)
  await unitPage.submitProposalRejectionReason()
  await t
    .expect(unitPage.placementProposalsAcceptButton.hasAttribute('disabled'))
    .ok()

  await employeeLogin(t, seppoAdmin, ApplicationListView.url)
  await applicationWorkbench.openPlacementProposalQueue()
  await applicationWorkbench.withdrawPlacementProposal(applicationId2)

  await employeeLogin(t, seppoManager)
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabPlacementProposals()
  await t
    .expect(unitPage.placementProposalsAcceptButton.hasAttribute('disabled'))
    .notOk()
  await t.click(unitPage.placementProposalsAcceptButton)
  await t.expect(unitPage.placementProposalsAcceptButton.exists).notOk()

  await execSimpleApplicationAction(applicationId, 'confirm-decision-mailed')

  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabWaitingConfirmation()
  await t.expect(unitPage.waitingGuardianConfirmationRow.count).eql(1)
})

test('Supervisor can download decision PDF only after it has been generated', async (t) => {
  const application = {
    ...applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture
    ),
    status: 'SENT' as const
  }
  applicationId = application.id

  await insertApplications([application])

  const decision = decisionFixture(
    applicationId,
    application.form.preferredStartDate,
    application.form.preferredStartDate
  )
  const decisionId = decision.id

  // NOTE: This will NOT generate a PDF, just create the decision
  await insertDecisionFixtures([
    {
      ...decision,
      employeeId: supervisorId
    }
  ])
  await employeeLogin(t, seppoManager)

  await applicationReadView.openApplicationByLink(applicationId)
  await applicationReadView.assertDecisionDownloadPending(decision.type)

  // NOTE: No need to wait for pending async jobs as this is synchronous (unlike the normal flow of users creating
  // decisions that would trigger PDF generation as an async job).
  await createDecisionPdf(decisionId)

  await applicationReadView.openApplicationByLink(applicationId)
  await applicationReadView.assertDecisionAvailableForDownload(decision.type)
})
