// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'

import config from '../../config'
import {
  execSimpleApplicationActions,
  insertApplicationAttachment
} from '../../dev-api'
import {
  applicationFixture,
  applicationFixtureId,
  testDaycare,
  testChild2,
  testAdult,
  Fixture,
  testChild,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import EmployeeNav from '../../pages/employee/employee-nav'
import { UnitPage } from '../../pages/employee/units/unit'
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import { testFileName, testFilePath } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

test.describe('Employee application attachments', () => {
  let page: Page
  let applicationListView: ApplicationListView
  let serviceWorker: DevEmployee

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await Fixture.decisionReasoningGenericDefaults().save()
    await testCareArea.save()
    await testDaycare.save()
    await Fixture.family({
      guardian: testAdult,
      children: [testChild, testChild2]
    }).save()

    const fixture = applicationFixture(testChild, testAdult)
    await createApplications({ body: [fixture] })
    serviceWorker = await Fixture.employee().serviceWorker().save()

    page = evaka
    applicationListView = new ApplicationListView(page)

    await employeeLogin(page, serviceWorker)
    await page.goto(config.employeeUrl)
    await new EmployeeNav(page).applicationsTab.click()
  })

  async function addAttachmentToApplication(applicationId: string) {
    await applicationListView.searchButton.click()
    const applicationView = await applicationListView
      .applicationRow(applicationId)
      .openApplication()
    const applicationEditView = await applicationView.startEditing()
    await applicationEditView.setShiftCareNeeded()
    await applicationEditView.shiftCareAttachmentFileUpload.uploadTestFile()
    await applicationEditView.saveApplication()
  }

  test('Employee can add and remove attachments', async () => {
    await applicationListView.searchButton.click()
    const applicationView = await applicationListView
      .applicationRow(applicationFixtureId)
      .openApplication()
    const applicationEditView = await applicationView.startEditing()

    await applicationEditView.setUrgent()
    await applicationEditView.urgentAttachmentFileUpload.uploadTestFile()

    await applicationEditView.setShiftCareNeeded()
    await applicationEditView.shiftCareAttachmentFileUpload.uploadTestFile()

    await applicationEditView.shiftCareAttachmentFileUpload.deleteUploadedFile()
    await applicationEditView.saveApplication()

    await applicationListView.searchButton.click()
    const applicationView2 = await applicationListView
      .applicationRow(applicationFixtureId)
      .openApplication()
    await applicationView2.assertUrgencyAttachmentReceivedAtVisible(
      testFileName
    )
  })

  test('Extended care attachment is visible to appropriate unit supervisor', async ({
    newEvakaPage
  }) => {
    await addAttachmentToApplication(applicationFixtureId)

    await execSimpleApplicationActions(
      applicationFixtureId,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_PLACEMENT_PROPOSAL'
      ],
      HelsinkiDateTime.now() // TODO: use mock clock
    )

    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()

    const page2 = await newEvakaPage()
    const unitPage = new UnitPage(page2)
    await employeeLogin(page2, unitSupervisor)
    await unitPage.navigateToUnit(testDaycare.id)

    const view = new ApplicationReadView(page2)
    await view.navigateToApplication(applicationFixtureId)
    await view.waitUntilLoaded()
    await view.assertExtendedCareAttachmentExists(testFileName)
  })

  test('Extended care attachment is not visible to non-around-the-clock unit supervisor', async ({
    newEvakaPage
  }) => {
    const daycareId = randomId<DaycareId>()
    await Fixture.daycare({
      ...testDaycare,
      shiftCareOperationTimes: null,
      shiftCareOpenOnHolidays: false,
      id: daycareId
    }).save()

    // Shift care requested at a unit that does not provide it. The editor no
    // longer offers such units once shift care is ticked, so this application
    // is built through the dev API rather than the UI.
    const application = applicationFixture(
      testChild2,
      testAdult,
      undefined,
      'DAYCARE',
      null,
      [daycareId]
    )
    const applicationId = randomId<ApplicationId>()
    await createApplications({
      body: [
        {
          ...application,
          id: applicationId,
          form: {
            ...application.form,
            preferences: {
              ...application.form.preferences,
              serviceNeed: {
                ...application.form.preferences.serviceNeed!,
                shiftCare: true
              }
            }
          }
        }
      ]
    })
    await insertApplicationAttachment(
      applicationId,
      serviceWorker.id,
      'EXTENDED_CARE',
      testFileName,
      testFilePath
    )

    await execSimpleApplicationActions(
      applicationId,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_PLACEMENT_PROPOSAL'
      ],
      HelsinkiDateTime.now() // TODO: use mock clock
    )

    const page2 = await newEvakaPage()
    await employeeLogin(
      page2,
      await Fixture.employee().unitSupervisor(daycareId).save()
    )
    const view = new ApplicationReadView(page2)
    await view.navigateToApplication(applicationId)
    await view.waitUntilLoaded()
    await view.assertExtendedCareAttachmentDoesNotExist(testFileName)
  })
})
