// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { insertApplications, resetDatabase } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  Fixture
} from '../../dev-api/fixtures'
import ApplicationsPage from '../../pages/employee/applications'
import EmployeeNav from '../../pages/employee/employee-nav'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let applicationsPage: ApplicationsPage

const testFileName = 'test_file.png'
const testFilePath = `src/e2e-test/assets/${testFileName}`

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
