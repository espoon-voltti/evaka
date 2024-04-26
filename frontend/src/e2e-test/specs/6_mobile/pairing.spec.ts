// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  postPairing,
  postPairingResponse,
  resetServiceState
} from '../../generated/api-clients'
import { PairingFlow } from '../../pages/employee/mobile/pairing-flow'
import { waitUntilTrue } from '../../utils'
import { Page } from '../../utils/page'

let page: Page
let pairingFlow: PairingFlow
let fixtures: AreaAndPersonFixtures

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()

  page = await Page.open({ acceptDownloads: true })
  pairingFlow = new PairingFlow(page)
})

describe('Mobile pairing', () => {
  test('User can add a mobile device mobile side', async () => {
    await page.goto(config.mobileUrl)

    await pairingFlow.startPairing()
    const res = await postPairing({
      body: { unitId: fixtures.daycareFixture.id }
    })

    await pairingFlow.submitChallengeKey(res.challengeKey)
    const responseKey = await pairingFlow.getResponseKey()
    expect(responseKey).toBeTruthy()

    await postPairingResponse({
      id: res.id,
      body: { challengeKey: res.challengeKey, responseKey }
    })

    await waitUntilTrue(() => pairingFlow.isPairingWizardFinished())
    await pairingFlow.clickStartCta()
  })
})
