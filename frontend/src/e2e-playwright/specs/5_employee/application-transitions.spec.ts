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
import {
  cleanUpMessages,
  createDecisionPdf,
  execSimpleApplicationActions,
  insertApplications,
  insertDecisionFixtures,
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
// import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import { UnitPage } from '../../pages/employee/units/unit'
import { addWeeks, format } from 'date-fns'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'
import ApplicationListView from '../../pages/employee/applications/application-list-view'

let page: Page
let applicationWorkbench: ApplicationWorkbenchPage
let applicationReadView: ApplicationReadView

const serviceWorker = {
  id: config.serviceWorkerAad,
  externalId: `espoo-ad:${config.serviceWorkerAad}`,
  firstName: 'Paula',
  lastName: 'Palveluohjaaja',
  email: 'paula.palveluohjaaja@evaka.test',
  roles: ['SERVICE_WORKER' as const]
}

const unitSupervisor = {
  id: config.unitSupervisorAad,
  externalId: `espoo-ad:${config.unitSupervisorAad}`,
  firstName: 'Esa',
  lastName: 'Esimies',
  email: 'esa.esimies@evaka.test',
  roles: []
}

let fixtures: AreaAndPersonFixtures
let applicationId: string

beforeEach(async () => {
  await resetDatabase()
  await cleanUpMessages()
  fixtures = await initializeAreaAndPersonData()

  await insertEmployeeFixture(serviceWorker)

  page = await Page.open()
  applicationWorkbench = new ApplicationWorkbenchPage(page)
  applicationReadView = new ApplicationReadView(page)
})

describe('Application transitions', () => {
  test('Service worker accepts decision on behalf of the enduser', async () => {
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

    await employeeLogin(page, 'SERVICE_WORKER')
    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.acceptDecision('DAYCARE')

    await applicationReadView.assertApplicationStatus('Paikka vastaanotettu')
  })

  test('Service worker accepts decision on behalf of the enduser and forwards start date 2 weeks', async () => {
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

    await employeeLogin(page, 'SERVICE_WORKER')
    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.setDecisionStartDate(
      'DAYCARE',
      format(
        addWeeks(new Date(fixture.form.preferredStartDate), 2),
        'dd.MM.yyyy'
      )
    )

    await applicationReadView.acceptDecision('DAYCARE')
    await applicationReadView.assertApplicationStatus('Paikka vastaanotettu')
  })

  test('Sending decision sets application to waiting confirmation state', async () => {
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

    await employeeLogin(page, 'SERVICE_WORKER')
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openDecisionQueue()
    await applicationWorkbench.sendDecisionsWithoutProposal(applicationId)

    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.assertApplicationStatus(
      'Vahvistettavana huoltajalla'
    )
  })

  test('Accepting decision for non vtj guardian sets application to waiting for mailing state', async () => {
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

    await employeeLogin(page, 'SERVICE_WORKER')
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openDecisionQueue()
    await applicationWorkbench.sendDecisionsWithoutProposal(applicationId)

    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.assertApplicationStatus('Odottaa postitusta')
  })

  test('Placement dialog shows warning if guardian has restricted details', async () => {
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

    await execSimpleApplicationActions(applicationId, [
      'move-to-waiting-placement'
    ])

    await employeeLogin(page, 'SERVICE_WORKER')
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementQueue()

    await applicationWorkbench.openDaycarePlacementDialogById(applicationId)

    await page.find('[data-qa="restricted-details-warning"]').waitUntilVisible()
  })

  test('Placement proposal flow', async () => {
    const fixture1 = {
      ...applicationFixture(
        fixtures.enduserChildFixtureJari,
        fixtures.familyWithTwoGuardians.guardian
      ),
      status: 'SENT' as const
    }
    applicationId = fixture1.id

    const applicationId2 = 'dd54782e-231c-4014-abaf-a63eed4e2627'
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

    const page2 = await Page.open()
    const unitPage = new UnitPage(page2)

    await insertEmployeeFixture(unitSupervisor)
    await setAclForDaycares(
      unitSupervisor.externalId,
      fixtures.daycareFixture.id
    )
    await employeeLogin(page2, 'UNIT_SUPERVISOR')

    // unit supervisor
    await unitPage.navigateToUnit(fixtures.daycareFixture.id)
    let placementProposals = (await unitPage.openApplicationProcessTab())
      .placementProposals

    await placementProposals.assertAcceptButtonDisabled()
    await placementProposals.clickProposalAccept(applicationId)
    await placementProposals.assertAcceptButtonEnabled()
    await placementProposals.clickProposalAccept(applicationId2)

    await placementProposals.clickProposalReject(applicationId2)
    await placementProposals.selectProposalRejectionReason(0)
    await placementProposals.submitProposalRejectionReason()

    // service worker
    await employeeLogin(page, 'SERVICE_WORKER')
    await page.goto(ApplicationListView.url)
    await applicationWorkbench.waitUntilLoaded()

    await applicationWorkbench.openPlacementProposalQueue()
    await applicationWorkbench.withdrawPlacementProposal(applicationId2)
    await applicationWorkbench.assertWithdrawPlacementProposalsButtonDisabled()

    // unit supervisor
    await unitPage.navigateToUnit(fixtures.daycareFixture.id)
    const applicationProcessPage = await unitPage.openApplicationProcessTab()
    placementProposals = applicationProcessPage.placementProposals
    await placementProposals.assertAcceptButtonEnabled()
    await placementProposals.clickAcceptButton()
    await applicationProcessPage.assertIsLoading()
    await applicationProcessPage.waitUntilLoaded()

    await execSimpleApplicationActions(applicationId, [
      'confirm-decision-mailed'
    ])

    await unitPage.navigateToUnit(fixtures.daycareFixture.id)
    const placementPlans = (await unitPage.openApplicationProcessTab())
      .placementPlans
    await placementPlans.assertWaitingGuardianConfirmationRowCount(1)
  })

  test('Supervisor can download decision PDF only after it has been generated', async () => {
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
        employeeId: serviceWorker.id
      }
    ])
    await employeeLogin(page, 'SERVICE_WORKER')

    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.assertDecisionDownloadPending(decision.type)

    // NOTE: No need to wait for pending async jobs as this is synchronous (unlike the normal flow of users creating
    // decisions that would trigger PDF generation as an async job).
    await createDecisionPdf(decisionId)

    await applicationReadView.navigateToApplication(applicationId)
    await applicationReadView.assertDecisionAvailableForDownload(decision.type)
  })
})
