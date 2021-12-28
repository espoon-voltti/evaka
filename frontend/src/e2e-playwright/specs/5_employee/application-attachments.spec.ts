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
  await Fixture.employee()
    .with({
      id: config.serviceWorkerAad,
      externalId: `espoo-ad:${config.serviceWorkerAad}`,
      roles: ['SERVICE_WORKER']
    })
    .save()

  page = await Page.open()
  applicationsPage = new ApplicationsPage(page)

  await employeeLogin(page, 'SERVICE_WORKER')
  await page.goto(config.employeeUrl)
  await new EmployeeNav(page).applicationsTab.click()
})

describe('Employee application attachments', () => {
  test('Employee can add and remove attachments', async () => {
    let applicationView = await applicationsPage
      .applicationRow(applicationFixtureId)
      .openApplication()
    await applicationView.startEditing()

    await applicationView.setUrgent()
    await applicationView.uploadUrgentAttachment(testFilePath)
    await applicationView.assertUrgentAttachmentUploaded(testFileName)

    await applicationView.setShiftCareNeeded()
    await applicationView.uploadShiftCareAttachment(testFilePath)
    await applicationView.assertShiftCareAttachmentUploaded(testFileName)

    await applicationView.deleteShiftCareAttachment(testFileName)
    await applicationView.assertShiftCareAttachmentsDeleted()
    await applicationView.saveApplication()

    applicationView = await applicationsPage
      .applicationRow(applicationFixtureId)
      .openApplication()
    await applicationView.assertUrgencyAttachmentReceivedAtVisible(testFileName)
  })
})
