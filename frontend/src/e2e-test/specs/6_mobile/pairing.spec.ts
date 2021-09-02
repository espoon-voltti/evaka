// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import { logConsoleMessages } from '../../utils/fixture'
import {
  deleteEmployeeFixture,
  insertEmployeeFixture,
  postPairing,
  postPairingResponse,
  resetDatabase,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import PairingFlow from '../../pages/employee/mobile/pairing-flow'

const pairingFlow = new PairingFlow()

const employeeExternalIds = [
  'espoo-ad:df979243-f081-4241-bc4f-e93a019bddfa',
  'espoo-ad:7e7daa1e-2e92-4c36-9e90-63cea3cd8f3f'
] as const

let fixtures: AreaAndPersonFixtures

fixture('Mobile pairing')
  .meta({ type: 'regression', subType: 'mobile' })
  .beforeEach(async () => {
    await resetDatabase()
    fixtures = await initializeAreaAndPersonData()
    await deleteEmployeeFixture(config.supervisorExternalId)
    await insertEmployeeFixture({
      externalId: config.supervisorExternalId,
      firstName: 'Seppo',
      lastName: 'Sorsa',
      email: 'seppo.sorsa@evaka.test',
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
  .afterEach(logConsoleMessages)

test('User can add a mobile device mobile side', async (t) => {
  await t.navigateTo(config.mobileUrl)

  await t.click(pairingFlow.mobileStartPairingBtn)
  await t.expect(pairingFlow.mobilePairingTitle1.exists).ok()
  const res = await postPairing(fixtures.daycareFixture.id)

  await t.typeText(pairingFlow.challengeKeyInput, res.challengeKey)
  await t.click(pairingFlow.submitChallengeKeyBtn)

  await t.expect(pairingFlow.responseKey.exists).ok()
  const responseKey = await pairingFlow.responseKey.textContent
  await postPairingResponse(res.id, res.challengeKey, responseKey)

  await t.expect(pairingFlow.mobilePairingTitle3.exists).ok()
  await t.click(pairingFlow.unitPageLink)
  await t.expect(pairingFlow.unitName.exists).ok()
})
