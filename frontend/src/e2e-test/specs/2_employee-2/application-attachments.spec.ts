// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import path from 'path'
import {
  insertApplications,
  resetDatabase
} from '../../../e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../../e2e-test-common/dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId
} from '../../../e2e-test-common/dev-api/fixtures'
import { employeeLogin, seppoAdmin, seppoAdminRole } from '../../config/users'
import ApplicationEditView from '../../pages/employee/applications/application-edit-view'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'
import Home from '../../pages/home'
import { logConsoleMessages } from '../../utils/fixture'

const testFilePath = '../../assets/test_file.png'
const testFileName = path.basename(testFilePath)

const home = new Home()
const applicationReadView = new ApplicationReadView()
const applicationEditView = new ApplicationEditView()

let fixtures: AreaAndPersonFixtures

fixture('Employee attachments')
  .meta({ type: 'regression', subType: 'attachments' })
  .beforeEach(async (t) => {
    await resetDatabase()
    ;[fixtures] = await initializeAreaAndPersonData()

    const fixture = applicationFixture(
      fixtures.enduserChildFixtureJari,
      fixtures.enduserGuardianFixture
    )
    await insertApplications([fixture])

    await employeeLogin(t, seppoAdmin, home.homePage('admin'))
  })
  .afterEach(logConsoleMessages)

test('Employee can add and remove attachments', async () => {
  await applicationReadView.openApplicationByLink(applicationFixtureId)
  await applicationReadView.startEditing()

  await applicationEditView.setUrgent()
  await applicationEditView.uploadUrgentFile(testFilePath)
  await applicationEditView.assertUploadedUrgentFile(testFileName)

  await applicationEditView.setShiftCareNeeded()
  await applicationEditView.uploadShiftCareFile(testFilePath)
  await applicationEditView.assertUploadedShiftCareFile(testFileName)

  await applicationEditView.deleteShiftCareFile(testFileName)
  await applicationEditView.assertUploadedShiftCareFile(testFileName, false)
  await applicationEditView.saveApplication()

  await applicationReadView.openApplicationByLink(applicationFixtureId)
  await applicationReadView.assertUrgencyAttachmentReceivedAtVisible(
    testFileName
  )
})
