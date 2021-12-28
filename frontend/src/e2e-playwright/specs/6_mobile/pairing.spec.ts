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
  postPairing,
  postPairingResponse,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { PairingFlow } from '../../pages/employee/mobile/pairing-flow'
import { waitUntilTrue } from '../../utils'
import { Page } from 'e2e-playwright/utils/page'
import { Fixture } from 'e2e-test-common/dev-api/fixtures'

let page: Page
let pairingFlow: PairingFlow
let fixtures: AreaAndPersonFixtures

beforeEach(async () => {
  page = await Page.open({ acceptDownloads: true })
  pairingFlow = new PairingFlow(page)
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  await deleteEmployeeFixture(config.supervisorExternalId)
  await Fixture.employee()
    .with({
      externalId: config.supervisorExternalId,
      roles: []
    })
    .withDaycareAcl(fixtures.daycareFixture.id, 'UNIT_SUPERVISOR')
    .save()
})

describe('Mobile pairing', () => {
  test('User can add a mobile device mobile side', async () => {
    await page.goto(config.mobileUrl)

    await pairingFlow.startPairing()
    const res = await postPairing(fixtures.daycareFixture.id)

    await pairingFlow.submitChallengeKey(res.challengeKey)
    const responseKey = await pairingFlow.getResponseKey()
    expect(responseKey).toBeTruthy()

    await postPairingResponse(res.id, res.challengeKey, responseKey)

    await waitUntilTrue(() => pairingFlow.isPairingWizardFinished())
    await pairingFlow.clickStartCta()
  })
})
