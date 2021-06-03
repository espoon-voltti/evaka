// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UnitDetailsPage } from '../../pages/admin/unit-details-page'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import { employeeLogin, seppoAdmin } from '../../config/users'
import AdminHome from '../../pages/home'
import { Daycare } from 'e2e-test-common/dev-api/types'
import { insertCareAreaFixtures, resetDatabase } from 'e2e-test-common/dev-api'

const adminHome = new AdminHome()
const unitDetailsPage = new UnitDetailsPage()

let fixtures: AreaAndPersonFixtures
let daycare1: Daycare

fixture('Unit - unit details')
  .meta({ type: 'regression', subType: 'units' })
  .beforeEach(async (t) => {
    await resetDatabase()
    ;[fixtures] = await initializeAreaAndPersonData()
    await insertCareAreaFixtures([
      {
        id: '7f08ec20-3843-466e-807e-a8cddf5d5605',
        name: 'Matinkylä-Olari',
        areaCode: '251',
        subCostCenter: '03',
        shortName: 'matinkyla-olari'
      }
    ])
    daycare1 = fixtures.daycareFixture
    await employeeLogin(t, seppoAdmin, adminHome.homePage('admin'))
  })
  .afterEach(logConsoleMessages)

test('Admin creates a new unit', async () => {
  await unitDetailsPage.openNewUnitEditor()
  await unitDetailsPage.fillUnitName('Uusi Kerho')
  await unitDetailsPage.chooseArea('Superkeskus')
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
