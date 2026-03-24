// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from '../../config'
import { testCareArea, testDaycare } from '../../dev-api/fixtures'
import {
  postPairing,
  postPairingResponse,
  resetServiceState
} from '../../generated/api-clients'
import { PairingFlow } from '../../pages/employee/mobile/pairing-flow'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'

test.describe('Mobile pairing', () => {
  let page: Page
  let pairingFlow: PairingFlow

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await testCareArea.save()
    await testDaycare.save()

    page = evaka
    pairingFlow = new PairingFlow(page)
  })

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

    await expect.poll(() => pairingFlow.isPairingWizardFinished()).toBe(true)
    await pairingFlow.clickStartCta()
  })
})
