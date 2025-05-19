// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { fromUuid } from 'lib-common/id-type'

import config from '../../config'
import { execSimpleApplicationAction, runPendingAsyncJobs } from '../../dev-api'
import {
  applicationFixture,
  familyWithRestrictedDetailsGuardian,
  familyWithSeparatedGuardians,
  familyWithTwoGuardians,
  Fixture,
  testAdult,
  testCareArea,
  testChild2,
  testDaycare,
  testPreschool
} from '../../dev-api/fixtures'
import {
  cleanUpMessages,
  createApplicationPlacementPlan,
  createApplications,
  getMessages,
  resetServiceState
} from '../../generated/api-clients'
import type {
  DevApplicationWithForm,
  DevEmployee
} from '../../generated/api-types'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let applicationListView: ApplicationListView
let applicationReadView: ApplicationReadView

let admin: DevEmployee

let singleParentApplication: DevApplicationWithForm
let familyWithTwoGuardiansApplication: DevApplicationWithForm
let separatedFamilyApplication: DevApplicationWithForm
let restrictedDetailsGuardianApplication: DevApplicationWithForm

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await testPreschool.save()
  await Fixture.family({ guardian: testAdult, children: [testChild2] }).save()
  await familyWithTwoGuardians.save()
  await familyWithSeparatedGuardians.save()
  await familyWithRestrictedDetailsGuardian.save()
  singleParentApplication = applicationFixture(testChild2, testAdult)
  familyWithTwoGuardiansApplication = {
    ...applicationFixture(
      familyWithTwoGuardians.children[0],
      familyWithTwoGuardians.guardian,
      familyWithTwoGuardians.otherGuardian
    ),
    id: fromUuid<ApplicationId>('8634e2b9-200b-4a68-b956-66c5126f86a0')
  }
  separatedFamilyApplication = {
    ...applicationFixture(
      familyWithSeparatedGuardians.children[0],
      familyWithSeparatedGuardians.guardian,
      familyWithSeparatedGuardians.otherGuardian,
      'DAYCARE',
      'NOT_AGREED'
    ),
    id: fromUuid<ApplicationId>('0c8b9ad3-d283-460d-a5d4-77bdcbc69374')
  }
  restrictedDetailsGuardianApplication = {
    ...applicationFixture(
      familyWithRestrictedDetailsGuardian.children[0],
      familyWithRestrictedDetailsGuardian.guardian,
      familyWithRestrictedDetailsGuardian.otherGuardian,
      'DAYCARE',
      'AGREED'
    ),
    id: fromUuid<ApplicationId>('6a9b1b1e-3fdf-11eb-b378-0242ac130002')
  }
  await cleanUpMessages()

  await createApplications({
    body: [
      singleParentApplication,
      familyWithTwoGuardiansApplication,
      separatedFamilyApplication,
      restrictedDetailsGuardianApplication
    ]
  })

  admin = await Fixture.employee().admin().save()

  page = await Page.open()
  applicationListView = new ApplicationListView(page)
  applicationReadView = new ApplicationReadView(page)
})

describe('Application details', () => {
  test('Admin can view application details', async () => {
    await employeeLogin(page, admin)
    await page.goto(config.adminUrl)

    await applicationListView.searchButton.click()
    const application = await applicationListView
      .applicationRow(singleParentApplication.id)
      .openApplication()
    await application.assertGuardianName(
      `${testAdult.lastName} ${testAdult.firstName}`
    )
  })

  test('Other VTJ guardian is shown as empty if there is no other guardian', async () => {
    await employeeLogin(page, admin)
    await page.goto(config.adminUrl)

    await applicationListView.searchButton.click()
    const application = await applicationListView
      .applicationRow(singleParentApplication.id)
      .openApplication()
    await application.assertOtherVtjGuardianMissing()
  })

  test('Other VTJ guardian in same address is shown', async () => {
    await employeeLogin(page, admin)
    await page.goto(config.adminUrl)

    await applicationListView.searchButton.click()
    const application = await applicationListView
      .applicationRow(familyWithTwoGuardiansApplication.id)
      .openApplication()
    await application.assertOtherVtjGuardianName(
      `${familyWithTwoGuardians.otherGuardian!.lastName} ${familyWithTwoGuardians.otherGuardian!.firstName}`
    )
    await application.assertOtherGuardianSameAddress(true)
  })

  test('Other VTJ guardian in different address is shown', async () => {
    await employeeLogin(page, admin)
    await page.goto(config.adminUrl)

    await applicationListView.searchButton.click()
    const application = await applicationListView
      .applicationRow(separatedFamilyApplication.id)
      .openApplication()
    await application.assertOtherVtjGuardianName(
      `${familyWithSeparatedGuardians.otherGuardian!.lastName} ${familyWithSeparatedGuardians.otherGuardian!.firstName}`
    )
    await application.assertOtherGuardianSameAddress(false)
    await application.assertOtherGuardianAgreementStatus(false)
  })

  test('Decision is not sent automatically to the other guardian if the first guardian has restricted details enabled', async () => {
    const serviceWorker = await Fixture.employee().serviceWorker().save()

    await execSimpleApplicationAction(
      restrictedDetailsGuardianApplication.id,
      'MOVE_TO_WAITING_PLACEMENT',
      HelsinkiDateTime.now() // TODO: use mock clock
    )
    const preferredStartDate =
      // eslint-disable-next-line @typescript-eslint/no-extra-non-null-assertion,@typescript-eslint/no-unnecessary-type-assertion
      restrictedDetailsGuardianApplication.form.preferences.preferredStartDate!!
    await createApplicationPlacementPlan({
      applicationId: restrictedDetailsGuardianApplication.id,
      body: {
        unitId: testPreschool.id,
        period: new FiniteDateRange(preferredStartDate, preferredStartDate),
        preschoolDaycarePeriod: null
      }
    })
    await execSimpleApplicationAction(
      restrictedDetailsGuardianApplication.id,
      'SEND_DECISIONS_WITHOUT_PROPOSAL',
      HelsinkiDateTime.now() // TODO: use mock clock
    )

    await employeeLogin(page, serviceWorker)
    await applicationReadView.navigateToApplication(
      restrictedDetailsGuardianApplication.id
    )
    await applicationReadView.assertApplicationStatus(
      'Vahvistettavana huoltajalla'
    )

    await runPendingAsyncJobs(
      HelsinkiDateTime.now() // TODO: use mock clock
    )
    const messages = await getMessages()
    expect(messages.length).toEqual(1)
    expect(messages[0].ssn).toEqual(
      familyWithRestrictedDetailsGuardian.guardian.ssn
    )
  })

  test('Supervisor can read an accepted application although the supervisors unit is not a preferred unit before and after accepting the decision', async () => {
    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(testPreschool.id)
      .save()

    await execSimpleApplicationAction(
      singleParentApplication.id,
      'MOVE_TO_WAITING_PLACEMENT',
      HelsinkiDateTime.now() // TODO: use mock clock
    )
    const preferredStartDate =
      // eslint-disable-next-line @typescript-eslint/no-extra-non-null-assertion,@typescript-eslint/no-unnecessary-type-assertion
      singleParentApplication.form.preferences.preferredStartDate!!
    await createApplicationPlacementPlan({
      applicationId: singleParentApplication.id,
      body: {
        unitId: testPreschool.id,
        period: new FiniteDateRange(preferredStartDate, preferredStartDate),
        preschoolDaycarePeriod: null
      }
    })
    await execSimpleApplicationAction(
      singleParentApplication.id,
      'SEND_DECISIONS_WITHOUT_PROPOSAL',
      HelsinkiDateTime.now() // TODO: use mock clock
    )

    await employeeLogin(page, unitSupervisor)
    await applicationReadView.navigateToApplication(singleParentApplication.id)
    await applicationReadView.assertApplicationStatus(
      'Vahvistettavana huoltajalla'
    )
    await applicationReadView.acceptDecision('DAYCARE')
    await applicationReadView.assertApplicationStatus('Paikka vastaanotettu')
  })

  test('Service worker can create, edit and delete application notes', async () => {
    const serviceWorker = await Fixture.employee().serviceWorker().save()
    await employeeLogin(page, serviceWorker)
    await page.goto(config.employeeUrl)

    await applicationListView.searchButton.click()
    const application = await applicationListView
      .applicationRow(singleParentApplication.id)
      .openApplication()
    const newNote = 'New note.'
    await application.addNote(newNote)
    await application.assertNote(0, newNote)
    const editedNote = 'Edited note.'
    await application.editNote(0, editedNote)
    await application.assertNote(0, editedNote)
    await application.deleteNote(0)
    await application.assertNoNotes()
  })
})
