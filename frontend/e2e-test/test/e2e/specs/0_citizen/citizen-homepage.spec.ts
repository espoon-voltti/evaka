// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { logConsoleMessages } from '../../utils/fixture'
import { enduserRole } from '../../config/users'
import CitizenHomePage from '../../pages/citizen/citizen-homepage'

const citizenHomePage = new CitizenHomePage()

fixture('Citizen page')
  .meta({ type: 'regression', subType: 'citizen-homepage' })
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
