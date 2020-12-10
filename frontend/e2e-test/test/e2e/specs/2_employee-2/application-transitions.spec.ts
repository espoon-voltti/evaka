// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { applicationFixture } from '../../dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { EmployeeDetail } from '../../dev-api/types'
import {
  cleanUpMessages,
  deleteAclForDaycare,
  deleteApplication,
  deleteEmployeeFixture,
  execSimpleApplicationAction,
  execSimpleApplicationActions,
  insertApplications,
  insertEmployeeFixture,
  setAclForDaycares as setAclForDaycare
} from '../../dev-api'
import { seppoAdminRole, seppoManagerRole } from '../../config/users'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import UnitPage from '../../pages/employee/units/unit-page'

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
let cleanUp: () => Promise<void>
let applicationId: string
let applicationId2: string

fixture('Application-transitions')
  .meta({ type: 'regression', subType: 'daycare-application' })
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    const uniqueSupervisor = {
      ...supervisor,
      email: `${Math.random().toString(36).substring(7)}@espoo.fi`
    }
    await insertEmployeeFixture(uniqueSupervisor)
    await setAclForDaycare(
      config.supervisorExternalId,
      fixtures.daycareFixture.id
    )
    await cleanUpMessages()
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    if (applicationId) {
      await deleteApplication(applicationId)
      applicationId = ''
    }
    if (applicationId2) {
      await deleteApplication(applicationId2)
      applicationId = ''
    }
    await cleanUpMessages()
  })
  .after(async () => {
    await deleteAclForDaycare(
      config.supervisorExternalId,
      fixtures.daycareFixture.id
    )
    await cleanUp()
    await deleteEmployeeFixture(config.supervisorExternalId)
  })

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

  await t.useRole(seppoManagerRole)
  await applicationReadView.openApplicationByLink(applicationId)
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

  await t.useRole(seppoAdminRole)

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

  await t.useRole(seppoAdminRole)

  await applicationWorkbench.openDecisionQueue()
  await applicationWorkbench.sendDecisionsWithoutProposal(applicationId)

  await applicationReadView.openApplicationByLink(applicationId)
  await t
    .expect(applicationReadView.applicationStatus.innerText)
    .contains('Odottaa postitusta')
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

  await t.useRole(seppoManagerRole)
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

  await t.useRole(seppoAdminRole)
  await applicationWorkbench.openPlacementProposalQueue()
  await applicationWorkbench.withdrawPlacementProposal(applicationId2)

  await t.useRole(seppoManagerRole)
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabPlacementProposals()
  await t
    .expect(unitPage.placementProposalsAcceptButton.hasAttribute('disabled'))
    .notOk()
  await t.click(unitPage.placementProposalsAcceptButton)

  await execSimpleApplicationAction(applicationId, 'confirm-decision-mailed')

  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openTabWaitingConfirmation()
  await t.expect(unitPage.waitingGuardianConfirmationRow.count).eql(1)
})
