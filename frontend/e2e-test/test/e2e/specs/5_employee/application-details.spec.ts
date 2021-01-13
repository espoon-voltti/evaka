// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { applicationFixture } from '../../dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import {
  cleanUpMessages,
  cleanUpInvoicingDatabase,
  createPlacementPlan,
  deleteAclForDaycare,
  deleteApplication,
  deleteEmployeeByExternalId,
  execSimpleApplicationAction,
  insertApplications,
  insertEmployeeFixture,
  setAclForDaycares,
  runPendingAsyncJobs,
  getMessages
} from '../../dev-api'
import { seppoAdminRole } from '../../config/users'
import AdminHome from '../../pages/home'
import EmployeeHome from '../../pages/employee/home'
import { ApplicationDetailsPage } from '../../pages/admin/application-details-page'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import { Application } from '../../dev-api/types'
import { DevLoginUser } from '../../pages/dev-login-form'
import assert from 'assert'

const applicationWorkbench = new ApplicationWorkbenchPage()
const applicationDetailsPage = new ApplicationDetailsPage()
const applicationReadView = new ApplicationReadView()
const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

let singleParentApplication: Application
let familyWithTwoGuardiansApplication: Application
let separatedFamilyApplication: Application
let restrictedDetailsGuardianApplication: Application

const preschoolSupervisor: DevLoginUser = {
  aad: config.supervisorAad,
  roles: []
}

fixture('Application - employee application details')
  .meta({ type: 'regression', subType: 'applications' })
  .page(adminHome.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    singleParentApplication = applicationFixture(
      fixtures.enduserChildFixtureKaarina,
      fixtures.enduserGuardianFixture
    )
    familyWithTwoGuardiansApplication = {
      ...applicationFixture(
        fixtures.familyWithTwoGuardians.children[0],
        fixtures.familyWithTwoGuardians.guardian,
        fixtures.familyWithTwoGuardians.otherGuardian
      ),
      id: '8634e2b9-200b-4a68-b956-66c5126f86a0'
    }
    separatedFamilyApplication = {
      ...applicationFixture(
        fixtures.familyWithSeparatedGuardians.children[0],
        fixtures.familyWithSeparatedGuardians.guardian,
        fixtures.familyWithSeparatedGuardians.otherGuardian,
        'daycare',
        'NOT_AGREED'
      ),
      id: '0c8b9ad3-d283-460d-a5d4-77bdcbc69374'
    }
    restrictedDetailsGuardianApplication = {
      ...applicationFixture(
        fixtures.familyWithRestrictedDetailsGuardian.children[0],
        fixtures.familyWithRestrictedDetailsGuardian.guardian,
        fixtures.familyWithRestrictedDetailsGuardian.otherGuardian,
        'daycare',
        'NOT_AGREED'
      ),
      id: '6a9b1b1e-3fdf-11eb-b378-0242ac130002'
    }
    await insertEmployeeFixture({
      externalId: config.supervisorExternalId,
      firstName: 'Esa',
      lastName: 'Esimies',
      roles: []
    })
    await setAclForDaycares(
      config.supervisorExternalId,
      fixtures.preschoolFixture.id
    )
    await cleanUpMessages()
  })
  .beforeEach(async () => {
    await insertApplications([
      singleParentApplication,
      familyWithTwoGuardiansApplication,
      separatedFamilyApplication,
      restrictedDetailsGuardianApplication
    ])
  })
  .afterEach(async (t) => {
    await logConsoleMessages(t)
    await cleanUpMessages()
    await deleteApplication(singleParentApplication.id)
    await deleteApplication(familyWithTwoGuardiansApplication.id)
    await deleteApplication(separatedFamilyApplication.id)
    await deleteApplication(restrictedDetailsGuardianApplication.id)
  })

  .after(async () => {
    await deleteEmployeeByExternalId(config.supervisorExternalId)
    await deleteAclForDaycare(
      config.supervisorExternalId,
      fixtures.preschoolFixture.id
    )
    await cleanUpInvoicingDatabase()
    await cleanUp()
  })

test('Admin can view application details', async (t) => {
  await t.useRole(seppoAdminRole)
  await applicationWorkbench.openApplicationById(singleParentApplication.id)
  await t
    .expect(applicationDetailsPage.guardianName.innerText)
    .eql(
      `${fixtures.enduserGuardianFixture.lastName} ${fixtures.enduserGuardianFixture.firstName}`
    )
})

test('Other VTJ guardian is shown as empty if there is no other guardian', async (t) => {
  await t.useRole(seppoAdminRole)
  await applicationWorkbench.openApplicationById(singleParentApplication.id)
  await t.expect(applicationDetailsPage.noOtherVtjGuardianText.visible).ok()
})

test('Other VTJ guardian in same address is shown', async (t) => {
  await t.useRole(seppoAdminRole)
  await applicationWorkbench.openApplicationById(
    familyWithTwoGuardiansApplication.id
  )
  await t
    .expect(applicationDetailsPage.vtjGuardianName.innerText)
    .eql(
      `${fixtures.familyWithTwoGuardians.otherGuardian.lastName} ${fixtures.familyWithTwoGuardians.otherGuardian.firstName}`
    )
  await t
    .expect(applicationDetailsPage.otherGuardianSameAddress.innerText)
    .contains('Kyllä')
})

test('Other VTJ guardian in different address is shown', async (t) => {
  await t.useRole(seppoAdminRole)
  await applicationWorkbench.openApplicationById(separatedFamilyApplication.id)
  await t
    .expect(applicationDetailsPage.vtjGuardianName.innerText)
    .eql(
      `${fixtures.familyWithSeparatedGuardians.otherGuardian.lastName} ${fixtures.familyWithSeparatedGuardians.otherGuardian.firstName}`
    )
  await t
    .expect(applicationDetailsPage.otherGuardianSameAddress.innerText)
    .contains('Ei')

  await t
    .expect(applicationDetailsPage.otherGuardianAgreementStatus.innerText)
    .contains('Ei ole sovittu yhdessä')
})

test('Decision is not sent automatically to the other guardian if the first guardian has restricted details enabled', async (t) => {
  await execSimpleApplicationAction(
    restrictedDetailsGuardianApplication.id,
    'move-to-waiting-placement'
  )
  await createPlacementPlan(restrictedDetailsGuardianApplication.id, {
    unitId: fixtures.preschoolFixture.id,
    period: {
      start: restrictedDetailsGuardianApplication.form.preferredStartDate,
      end: restrictedDetailsGuardianApplication.form.preferredStartDate
    }
  })
  await execSimpleApplicationAction(
    restrictedDetailsGuardianApplication.id,
    'send-decisions-without-proposal'
  )
  await employeeHome.login(preschoolSupervisor)
  await applicationReadView.openApplicationByLink(
    restrictedDetailsGuardianApplication.id
  )
  await t
    .expect(applicationDetailsPage.applicationStatus.innerText)
    .eql('Vahvistettavana huoltajalla')
  await runPendingAsyncJobs()

  const messages = await getMessages()

  assert(messages.length === 1)
  assert(
    messages[0].ssn ===
      fixtures.familyWithRestrictedDetailsGuardian.guardian.ssn
  )
})

test('Supervisor can read an accepted application although the supervisors unit is not a preferred unit before and after accepting the decision', async (t) => {
  await execSimpleApplicationAction(
    singleParentApplication.id,
    'move-to-waiting-placement'
  )
  await createPlacementPlan(singleParentApplication.id, {
    unitId: fixtures.preschoolFixture.id,
    period: {
      start: singleParentApplication.form.preferredStartDate,
      end: singleParentApplication.form.preferredStartDate
    }
  })
  await execSimpleApplicationAction(
    singleParentApplication.id,
    'send-decisions-without-proposal'
  )

  await employeeHome.login(preschoolSupervisor)
  await applicationReadView.openApplicationByLink(singleParentApplication.id)
  await t
    .expect(applicationDetailsPage.applicationStatus.innerText)
    .eql('Vahvistettavana huoltajalla')
  await applicationReadView.acceptDecision('DAYCARE')
  await t
    .expect(applicationDetailsPage.applicationStatus.innerText)
    .eql('Paikka vastaanotettu')
})
