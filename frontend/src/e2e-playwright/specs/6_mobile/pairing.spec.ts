// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import {
  deleteEmployeeFixture,
  insertEmployeeFixture,
  postPairing,
  postPairingResponse,
  resetDatabase,
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import { Page } from 'playwright'
import { newBrowserContext } from '../../browser'
import { PairingFlow } from '../../pages/employee/mobile/pairing-flow'
import { waitUntilVisible } from '../../utils'

const employeeExternalIds = [
  'espoo-ad:df979243-f081-4241-bc4f-e93a019bddfa',
  'espoo-ad:7e7daa1e-2e92-4c36-9e90-63cea3cd8f3f'
] as const

let page: Page
let pairingFlow: PairingFlow
let fixtures: AreaAndPersonFixtures

beforeEach(async () => {
  page = await (await newBrowserContext({ acceptDownloads: true })).newPage()
  pairingFlow = new PairingFlow(page)
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

describe('Mobile pairing', () => {
  test('User can add a mobile device mobile side', async () => {
    await page.goto(config.mobileUrl)

    await pairingFlow.mobileStartPairingBtn.click()
    await waitUntilVisible(pairingFlow.mobilePairingTitle1)
    const res = await postPairing(fixtures.daycareFixture.id)

    await pairingFlow.challengeKeyInput.type(res.challengeKey)
    await pairingFlow.submitChallengeKeyBtn.click()

    await waitUntilVisible(pairingFlow.responseKey)
    const responseKey = await pairingFlow.responseKey.innerText()
    expect(responseKey).toBeTruthy()
    await postPairingResponse(res.id, res.challengeKey, responseKey)

    await waitUntilVisible(pairingFlow.mobilePairingTitle3)
    await pairingFlow.unitPageLink.click()
    await waitUntilVisible(pairingFlow.unitName)
  })
})
