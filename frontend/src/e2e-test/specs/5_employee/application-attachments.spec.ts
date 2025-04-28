// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationId, DaycareId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'

import config from '../../config'
import { execSimpleApplicationActions } from '../../dev-api'
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
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import EmployeeNav from '../../pages/employee/employee-nav'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page, testFileName } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let applicationListView: ApplicationListView

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()

  const fixture = applicationFixture(testChild, testAdult)
  await createApplications({ body: [fixture] })
  const serviceWorker = await Fixture.employee().serviceWorker().save()

  page = await Page.open()
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

describe('Employee application attachments', () => {
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

  test('Extended care attachment is visible to appropriate unit supervisor', async () => {
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

    const page2 = await Page.open()
    const unitPage = new UnitPage(page2)
    await employeeLogin(page2, unitSupervisor)
    await unitPage.navigateToUnit(testDaycare.id)

    const view = new ApplicationReadView(page2)
    await view.navigateToApplication(applicationFixtureId)
    await view.waitUntilLoaded()
    await view.assertExtendedCareAttachmentExists(testFileName)
  })

  test('Extended care attachment is not visible to non-around-the-clock unit supervisor', async () => {
    const daycareId = randomId<DaycareId>()
    await Fixture.daycare({
      ...testDaycare,
      shiftCareOperationTimes: null,
      shiftCareOpenOnHolidays: false,
      id: daycareId
    }).save()

    const applicationId = randomId<ApplicationId>()
    await createApplications({
      body: [
        {
          ...applicationFixture(
            testChild2,
            testAdult,
            undefined,
            'DAYCARE',
            null,
            [daycareId]
          ),
          id: applicationId
        }
      ]
    })

    await page.reload()

    await addAttachmentToApplication(applicationId)

    await execSimpleApplicationActions(
      applicationId,
      [
        'MOVE_TO_WAITING_PLACEMENT',
        'CREATE_DEFAULT_PLACEMENT_PLAN',
        'SEND_PLACEMENT_PROPOSAL'
      ],
      HelsinkiDateTime.now() // TODO: use mock clock
    )

    const page2 = await Page.open()
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
