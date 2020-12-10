// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  deleteEmployeeFixture,
  deleteMobileDevice,
  deletePairing,
  insertEmployeeFixture,
  postPairing,
  postPairingResponse,
  setAclForDaycares
} from '../../dev-api'
import { mobileRole } from '../../config/users'
import PairingFlow from '../../pages/employee/mobile/pairing-flow'
import { UUID } from '../../dev-api/types'
import { t } from 'testcafe'

const pairingFlow = new PairingFlow()

const employeeExternalIds = [
  'espoo-ad:df979243-f081-4241-bc4f-e93a019bddfa',
  'espoo-ad:7e7daa1e-2e92-4c36-9e90-63cea3cd8f3f'
] as const

let pairingId: UUID | undefined = undefined
let deviceId: UUID | null = null

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>

fixture('Mobile pairing')
  .meta({ type: 'regression', subType: 'mobile' })
  .page(config.adminUrl)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    await deleteEmployeeFixture(config.supervisorExternalId)
    await insertEmployeeFixture({
      externalId: config.supervisorExternalId,
      firstName: 'Seppo',
      lastName: 'Sorsa',
      email: 'seppo.sorsa@espoo.fi',
      roles: []
    })
    await setAclForDaycares(
      config.supervisorExternalId,
      fixtures.daycareFixture.id
    )
    await Promise.all([
      insertEmployeeFixture({
        externalId: employeeExternalIds[0],
        firstName: 'Pete',
        lastName: 'Päiväkoti',
        email: 'pete@example.com',
        roles: []
      }),
      insertEmployeeFixture({
        externalId: employeeExternalIds[1],
        firstName: 'Yrjö',
        lastName: 'Yksikkö',
        email: 'yy@example.com',
        roles: []
      })
    ])
  })
  .beforeEach(async () => {
    await t.useRole(mobileRole)
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    if (pairingId) {
      await deletePairing(pairingId)
      pairingId = undefined
    }
    if (deviceId) {
      await deleteMobileDevice(deviceId)
      deviceId = null
    }
    await cleanUp()
    await Promise.all(employeeExternalIds.map(deleteEmployeeFixture))
    await deleteEmployeeFixture(config.supervisorExternalId)
  })

test('User can add a mobile device mobile side', async (t) => {
  await t.navigateTo(config.mobileUrl)

  await t.click(pairingFlow.mobileStartPairingBtn)
  await t.expect(pairingFlow.mobilePairingTitle1.exists).ok()
  const res = await postPairing(fixtures.daycareFixture.id)

  await t.typeText(pairingFlow.challengeKeyInput, res.challengeKey)
  await t.click(pairingFlow.submitChallengeKeyBtn)

  await t.expect(pairingFlow.responseKey.exists).ok()
  const responseKey = await pairingFlow.responseKey.textContent
  const pairingResponse = await postPairingResponse(
    res.id,
    res.challengeKey,
    responseKey
  )
  pairingId = pairingResponse.id
  deviceId = pairingResponse.mobileDeviceId

  await t.expect(pairingFlow.mobilePairingTitle3.exists).ok()
  await t.click(pairingFlow.unitPageLink)
  await t.expect(pairingFlow.unitName.exists).ok()
})
