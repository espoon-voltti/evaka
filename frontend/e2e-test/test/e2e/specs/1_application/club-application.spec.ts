// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EnduserPage from '../../pages/enduser/enduser-navigation'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import ClubApplication from '../../pages/enduser/club-application-form'
import config from '../../config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import { EmployeeDetail, SuomiFiMessage } from '../../dev-api/types'
import {
  cleanUpMessages,
  deleteAclForDaycare,
  deleteApplication,
  deleteEmployeeFixture,
  getMessages,
  insertEmployeeFixture,
  runPendingAsyncJobs,
  setAclForDaycares as setAclForDaycare
} from '../../dev-api'
import { t } from 'testcafe'
import {
  enduserRole,
  seppoAdminRole,
  seppoManagerRole
} from '../../config/users'
import assert from 'assert'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'

const enduserPage = new EnduserPage()
const applicationWorkbench = new ApplicationWorkbenchPage()
const applicationReadView = new ApplicationReadView()
const supervisor: EmployeeDetail = {
  id: '552e5bde-92fb-4807-a388-40016f85f593',
  externalId: config.supervisorExternalId,
  firstName: 'Eeva',
  lastName: 'Esimies',
  email: 'eeva.esimies@espoo.fi',
  roles: []
}

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('New club application')
  .meta({ type: 'regression', subType: 'club-application' })
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    const uniqueSupervisor = {
      ...supervisor,
      email: `${Math.random().toString(36).substring(7)}@espoo.fi`
    }
    await insertEmployeeFixture(uniqueSupervisor)
    await setAclForDaycare(config.supervisorExternalId, fixtures.clubFixture.id)
    await cleanUpMessages()
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    applicationId ? await deleteApplication(applicationId) : false
    await deleteAclForDaycare(
      config.supervisorExternalId,
      fixtures.clubFixture.id
    )
    await deleteEmployeeFixture(config.supervisorExternalId)
    await cleanUpMessages()
    await cleanUp()
  })

let applicationId: string

test('Enduser sends club application', async () => {
  await t.useRole(enduserRole)

  await enduserPage.navigateToApplicationsTab()
  await enduserPage.createApplication('CLUB')
  const clubApplication = new ClubApplication()

  applicationId = await clubApplication.getApplicationId()
  await clubApplication.fillClubApplication()

  await clubApplication.checkAndSend()

  await clubApplication.assertApplicationStatus(applicationId, 'LÄHETETTY')

  await enduserPage.navigateToApplicationsTab()
  await enduserPage.editApplication(applicationId)
  await t.expect(clubApplication.saveBtn.exists).notOk()

  await t.useRole(seppoAdminRole)
  await applicationWorkbench.openApplicationById(applicationId)
})

test('Service worker adds a note to application and unit supervisor reads it', async (t) => {
  await t.useRole(seppoAdminRole)
  await applicationWorkbench.openApplicationById(applicationId)
  await applicationWorkbench.addNote('Make evaka great again')
  await applicationWorkbench.closeApplication()
  await t.useRole(seppoManagerRole)
  await applicationReadView.openApplicationByLink(applicationId)
  await applicationWorkbench.verifyNoteExists('Make evaka great again')
  await applicationWorkbench.closeApplication()
  await t.useRole(seppoAdminRole)
  await applicationWorkbench.openApplicationById(applicationId)
  await applicationWorkbench.deleteNote()
})

test('Admin sends the received applications to placement queue', async (t) => {
  await t.useRole(seppoAdminRole)
  // Send the application sent by the enduser to placement queue
  await applicationWorkbench.moveToWaitingPlacement(applicationId)
  await applicationWorkbench.openPlacementQueue()
  await t
    .expect(applicationWorkbench.getApplicationById(applicationId).exists)
    .ok()
})

test('Admin places application into a unit', async (t) => {
  await t.useRole(seppoAdminRole)

  // Place the submitted application
  await applicationWorkbench.openPlacementQueue()
  await applicationWorkbench.openDaycarePlacementDialogById(applicationId)
  await applicationWorkbench.placementPage.placeIn(0)
  await applicationWorkbench.placementPage.sendPlacement()

  // Ensure that the submitted application is in correct state
  await applicationWorkbench.openDecisionQueue()
  await t
    .expect(applicationWorkbench.getApplicationById(applicationId).exists)
    .ok()
})

test('Admin creates a decision', async (t) => {
  await t.useRole(seppoAdminRole)
  // Place the submitted application
  await applicationWorkbench.openDecisionQueue()
  await applicationWorkbench.openDecisionEditorById(applicationId)
  await applicationWorkbench.assertDecisionGuardians(
    'Johannes Olavi Antero Tapio Karhula'
  )
  await applicationWorkbench.decisionEditorPage.cancel()
  await applicationWorkbench.sendDecisionsWithoutProposal(applicationId)

  // Ensure that the submitted application is in correct state
  await applicationWorkbench.searchFilter.filterByApplicationStatus(
    'WAITING_CONFIRMATION'
  )
  await applicationWorkbench.openApplicationById(applicationId)
})

test('Enduser downloads decision PDF', async () => {
  await t.useRole(enduserRole)
  await enduserPage.navigateToApplicationsTab()
  await enduserPage.assertApplicationStatus(
    applicationId,
    'Vahvistettavana huoltajalla'
  )
  await runPendingAsyncJobs()
  await enduserPage.assertDownloadPDF(applicationId, 'Kerhopäätös')

  const messages = await getMessages()

  assert(messages.length === 1)
  assertSfiMessage(
    messages.find((m) => m.ssn === '070644-937X'),
    'Kerhopäätös_Jari-Petteri_Mukkelis-Makkelis_Vetelä-Viljami_Eelis-Juhani_Karhula.pdf',
    '070644-937X',
    'Kamreerintie 1',
    'Espoon varhaiskasvatukseen liittyvät päätökset',
    'fi'
  )
})

const assertSfiMessage = (
  message: SuomiFiMessage | undefined,
  expectedDocumentName: string,
  expectedSsn: string,
  expectedStreetAddress: string,
  expectedMessageHeader: string,
  expectedLanguage: string
) => {
  assert(
    message !== undefined,
    `Did not find expected sfi message for ${expectedSsn}`
  )
  assert(message.documentDisplayName === expectedDocumentName)
  assert(message.ssn === expectedSsn)
  assert(message.streetAddress === expectedStreetAddress)
  assert(message.messageHeader === expectedMessageHeader)
  assert(message.language === expectedLanguage)
}

test('Enduser accepts the placement', async () => {
  await t.useRole(enduserRole)
  await enduserPage.navigateToApplicationsTab()
  await enduserPage.assertApplicationStatus(
    applicationId,
    'Vahvistettavana huoltajalla'
  )
  await enduserPage.sendDecision(applicationId)
  await enduserPage.assertDecisionStatus(applicationId, 'HYVÄKSYTTY')
})
