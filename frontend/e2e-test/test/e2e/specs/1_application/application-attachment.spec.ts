// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Home from '../../pages/home'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import {
  applicationFixture,
  applicationFixtureId,
  uuidv4
} from '../../dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import { deleteApplication, insertApplications } from '../../dev-api'
import { enduserRole, seppoAdminRole } from '../../config/users'
import EnduserPage from '../../pages/enduser/enduser-navigation'
import ApplicationReadView from '../../pages/employee/applications/application-read-view'

const home = new Home()
const enduserPage = new EnduserPage()
const applicationReadView = new ApplicationReadView()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Enduser attachments')
  .meta({ type: 'regression', subType: 'application-attachments' })
  .page(home.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
  })
  .afterEach(logConsoleMessages)
  .afterEach(async () => {
    await deleteApplication(applicationFixtureId)
  })
  .after(async () => {
    await cleanUp()
  })

test('Urgent application attachments can be uploaded by end user', async (t) => {
  const fixture = applicationFixture(
    fixtures.enduserChildFixtureJari,
    fixtures.enduserGuardianFixture
  )

  const urgentApplication = {
    ...fixture,
    form: {
      ...fixture.form,
      urgent: true
    },
    id: uuidv4()
  }

  const attachmentFileName = 'test_file.jpg'
  await insertApplications([urgentApplication])
  await t.useRole(enduserRole)
  await enduserPage.navigateToApplicationsTab()
  await enduserPage.editApplication(urgentApplication.id)
  await enduserPage.uploadUrgentFile(`./${attachmentFileName}`)
  await enduserPage.assertUrgentFileHasBeenUploaded(attachmentFileName)

  await t.useRole(seppoAdminRole)
  await applicationReadView.openApplicationByLink(urgentApplication.id)
  await applicationReadView.assertPageTitle('Varhaiskasvatushakemus')
  await applicationReadView.assertUrgentAttachmentExists(attachmentFileName)
})
