// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { Fixture, testCareArea, testDaycare } from '../../dev-api/fixtures'
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

beforeEach(async () => {
  await resetServiceState()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare(testDaycare).save()

  page = await Page.open({ acceptDownloads: true })
  pairingFlow = new PairingFlow(page)
})

describe('Mobile pairing', () => {
  test('User can add a mobile device mobile side', async () => {
    await page.goto(config.mobileUrl)

    await pairingFlow.startPairing()
    const res = await postPairing({
      body: { unitId: testDaycare.id }
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
