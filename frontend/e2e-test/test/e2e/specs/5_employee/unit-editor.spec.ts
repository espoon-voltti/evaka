// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UnitDetailsPage } from '../../pages/admin/unit-details-page'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import { seppoAdminRole } from '../../config/users'
import AdminHome from '../../pages/home'
import { Daycare } from '../../dev-api/types'

const adminHome = new AdminHome()
const unitDetailsPage = new UnitDetailsPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
let daycare1: Daycare

fixture('Unit - unit details')
  .meta({ type: 'regression', subType: 'units' })
  .page(adminHome.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    daycare1 = fixtures.daycareFixture
  })
  .beforeEach(async (t) => {
    await t.useRole(seppoAdminRole)
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUp()
  })

test('Admin can edit unit details', async () => {
  await unitDetailsPage.openUnitDetailsPageById(daycare1.id)
  await unitDetailsPage.enableUnitEditor()
  await unitDetailsPage.fillManagerDataAndSubmitForm()
})
