// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import { ApplicationWorkbenchPage } from '../../pages/admin/application-workbench-page'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { applicationFixture } from 'e2e-test-common/dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import {
  cleanUpMessages,
  createPlacementPlan,
  execSimpleApplicationAction,
  insertApplications,
  insertEmployeeFixture,
  setAclForDaycares,
  runPendingAsyncJobs,
  getMessages,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { employeeLogin, seppoAdmin } from '../../config/users'
import AdminHome from '../../pages/home'
import { ApplicationDetailsPage } from '../../pages/admin/application-details-page'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import { Application } from 'e2e-test-common/dev-api/types'
import assert from 'assert'

const applicationWorkbench = new ApplicationWorkbenchPage()
const applicationDetailsPage = new ApplicationDetailsPage()
const applicationReadView = new ApplicationReadView()
const adminHome = new AdminHome()

let fixtures: AreaAndPersonFixtures

let singleParentApplication: Application
let familyWithTwoGuardiansApplication: Application
let separatedFamilyApplication: Application
let restrictedDetailsGuardianApplication: Application

fixture('Application - employee application details')
  .meta({ type: 'regression', subType: 'applications' })
  .beforeEach(async () => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
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
        'DAYCARE',
        'NOT_AGREED'
      ),
      id: '0c8b9ad3-d283-460d-a5d4-77bdcbc69374'
    }
    restrictedDetailsGuardianApplication = {
      ...applicationFixture(
        fixtures.familyWithRestrictedDetailsGuardian.children[0],
        fixtures.familyWithRestrictedDetailsGuardian.guardian,
        fixtures.familyWithRestrictedDetailsGuardian.otherGuardian,
        'DAYCARE',
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

    await insertApplications([
      singleParentApplication,
      familyWithTwoGuardiansApplication,
      separatedFamilyApplication,
      restrictedDetailsGuardianApplication
    ])
  })
  .afterEach(logConsoleMessages)

test('Admin can view application details', async (t) => {
  await employeeLogin(t, seppoAdmin, adminHome.homePage('admin'))
  await applicationWorkbench.openApplicationById(singleParentApplication.id)
  await t
    .expect(applicationDetailsPage.guardianName.innerText)
    .eql(
      `${fixtures.enduserGuardianFixture.lastName} ${fixtures.enduserGuardianFixture.firstName}`
    )
})

test('Other VTJ guardian is shown as empty if there is no other guardian', async (t) => {
  await employeeLogin(t, seppoAdmin, adminHome.homePage('admin'))
  await applicationWorkbench.openApplicationById(singleParentApplication.id)
  await t.expect(applicationDetailsPage.noOtherVtjGuardianText.visible).ok()
})

test('Other VTJ guardian in same address is shown', async (t) => {
  await employeeLogin(t, seppoAdmin, adminHome.homePage('admin'))
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
  await employeeLogin(t, seppoAdmin, adminHome.homePage('admin'))
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

  await employeeLogin(t, config.supervisorAad)
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

  await employeeLogin(t, config.supervisorAad)
  await applicationReadView.openApplicationByLink(singleParentApplication.id)
  await t
    .expect(applicationDetailsPage.applicationStatus.innerText)
    .eql('Vahvistettavana huoltajalla')
  await applicationReadView.acceptDecision('DAYCARE')
  await t
    .expect(applicationDetailsPage.applicationStatus.innerText)
    .eql('Paikka vastaanotettu')
})
