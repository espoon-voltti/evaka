// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import { insertApplications, resetDatabase } from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  Fixture
} from 'e2e-test-common/dev-api/fixtures'
import { employeeLogin } from 'e2e-playwright/utils/user'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import ApplicationsPage from 'e2e-playwright/pages/employee/applications'
import { Page } from '../../utils/page'

let page: Page
let applicationsPage: ApplicationsPage

const testFileName = 'test_file.png'
const testFilePath = `src/e2e-playwright/assets/${testFileName}`

beforeEach(async () => {
  await resetDatabase()
  const fixtures = await initializeAreaAndPersonData()

  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )
  await insertApplications([fixture])
  const serviceWorker = await Fixture.employeeServiceWorker().save()

  page = await Page.open()
  applicationsPage = new ApplicationsPage(page)

  await employeeLogin(page, serviceWorker.data)
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).applicationsTab.click()
})

describe('Employee application attachments', () => {
  test('Employee can add and remove attachments', async () => {
    let applicationView = await applicationsPage
      .applicationRow(applicationFixtureId)
      .openApplication()
    const applicationEditView = await applicationView.startEditing()

    await applicationEditView.setUrgent()
    await applicationEditView.uploadUrgentAttachment(testFilePath)
    await applicationEditView.assertUrgentAttachmentUploaded(testFileName)

    await applicationEditView.setShiftCareNeeded()
    await applicationEditView.uploadShiftCareAttachment(testFilePath)
    await applicationEditView.assertShiftCareAttachmentUploaded(testFileName)

    await applicationEditView.deleteShiftCareAttachment(testFileName)
    await applicationEditView.assertShiftCareAttachmentsDeleted()
    await applicationEditView.saveApplication()

    applicationView = await applicationsPage
      .applicationRow(applicationFixtureId)
      .openApplication()
    await applicationView.assertUrgencyAttachmentReceivedAtVisible(testFileName)
  })
})
