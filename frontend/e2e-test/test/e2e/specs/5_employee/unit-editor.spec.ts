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

test('Admin creates a new unit', async (t) => {
  await unitDetailsPage.openNewUnitEditor()
  await unitDetailsPage.fillUnitName('Uusi Kerho')
  await unitDetailsPage.chooseArea('Matinkylä-Olari')
  await unitDetailsPage.toggleCareType('CLUB')
  await unitDetailsPage.toggleApplicationType('CLUB')
  await unitDetailsPage.fillVisitingAddress('Kamreerintie 1', '02100', 'Espoo')
  await unitDetailsPage.fillManagerData(
    'Kerhon Johtaja',
    '01234567',
    'manager@example.com'
  )
})

test('Admin can edit unit details', async () => {
  await unitDetailsPage.openUnitDetailsPageById(daycare1.id)
  await unitDetailsPage.enableUnitEditor()
  await unitDetailsPage.fillManagerData(
    'Päiväkodin Johtaja',
    '01234567',
    'manager@example.com'
  )
  await unitDetailsPage.submitForm()
  // todo: verify that the updated manager data is visible somewhere
})
