// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  deleteEmployeeById,
  deleteMobileDevice,
  deletePairing,
  insertEmployeeFixture,
  postMobileDevice
} from '../../dev-api'
import { mobileAutoSignInRole } from '../../config/users'
import { t } from 'testcafe'
import { uuidv4 } from '../../dev-api/fixtures'
import MobileGroupsPage from '../../pages/employee/mobile/mobile-groups'

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

const employeeId = uuidv4()
const mobileDeviceId = employeeId
const mobileLongTermToken = uuidv4()
const pairingId = uuidv4()

fixture('Mobile daily notes')
  .meta({ type: 'regression', subType: 'mobile' })
  .page(config.adminUrl)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()

    await Promise.all([
      insertEmployeeFixture({
        id: employeeId,
        externalId: `espooad: ${employeeId}`,
        firstName: 'Yrjö',
        lastName: 'Yksikkö',
        email: 'yy@example.com',
        roles: ['MOBILE']
      })
    ])

    await postMobileDevice({
      id: mobileDeviceId,
      unitId: fixtures.daycareFixture.id,
      name: 'testMobileDevice',
      deleted: false,
      longTermToken: mobileLongTermToken
    })
  })
  .beforeEach(async () => {
    await t.useRole(mobileAutoSignInRole(mobileLongTermToken))
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await deletePairing(pairingId)
    await deleteMobileDevice(mobileDeviceId)
    await cleanUp()
    await deleteEmployeeById(employeeId)
  })

const mobileGroupsPage = new MobileGroupsPage()

test('Do mobile pairing via dev api', async (t) => {
  await t.expect(mobileGroupsPage.allGroups.visible).ok()
})
