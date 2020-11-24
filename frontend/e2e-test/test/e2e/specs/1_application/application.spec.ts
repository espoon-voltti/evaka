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
import { enduserRole } from '../../config/users'
import EnduserPage from '../../pages/enduser/enduser-navigation'

const home = new Home()
const enduserPage = new EnduserPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Employee reads applications')
  .meta({ type: 'regression', subType: 'applications2' })
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

  await insertApplications([urgentApplication])
  await t.useRole(enduserRole)
  await enduserPage.navigateToApplicationsTab()
  await enduserPage.editApplication(urgentApplication.id)
  await enduserPage.uploadUrgentFile('./test_file.jpg')
  await enduserPage.assertUrgentFileHasBeenUploaded('test_file.jpg')
})
