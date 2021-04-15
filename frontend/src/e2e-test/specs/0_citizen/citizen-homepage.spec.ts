// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import CitizenHomePage from '../../pages/citizen/citizen-homepage'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import { resetDatabase } from 'e2e-test-common/dev-api'

const citizenHomePage = new CitizenHomePage()

fixture('Citizen page')
  .meta({ type: 'regression', subType: 'citizen-homepage' })
  .before(async () => {
    await resetDatabase()
    await initializeAreaAndPersonData()
  })
  .afterEach(logConsoleMessages)

test('Citizen can change the UI language', async (t) => {
  await t.useRole(enduserRole)
  await t.click(citizenHomePage.nav.decisions)
  await citizenHomePage.selectLanguage('fi')
  await t
    .expect(citizenHomePage.nav.applications.textContent)
    .contains('Hakemukset')
  await citizenHomePage.selectLanguage('en')
  await t
    .expect(citizenHomePage.nav.applications.textContent)
    .contains('Applications')
})
