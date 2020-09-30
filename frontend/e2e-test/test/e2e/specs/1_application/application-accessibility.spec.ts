// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import EnduserPage from '../../pages/enduser/enduser-navigation'
import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'

const enduserPage = new EnduserPage()

let cleanUp: () => Promise<void>

fixture('Application accessibility')
  .meta({ type: 'regression', subType: 'accessibility' })
  .before(async () => {
    ;[, cleanUp] = await initializeAreaAndPersonData()
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUp()
  })

test('Pressing Esc-key in application type selection modal closes modal', async (t) => {
  await t.useRole(enduserRole)
  await enduserPage.navigateToApplicationsTab()

  await enduserPage.applications.create(0)
  await t.expect(enduserPage.applicationTypeSelectModal.exists).ok()

  await t.pressKey('esc')
  await t.expect(enduserPage.applicationTypeSelectModal.exists).notOk()
})
