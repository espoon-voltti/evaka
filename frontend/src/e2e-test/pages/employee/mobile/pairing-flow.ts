// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'

export default class PairingFlow {
  readonly mobileStartPairingBtn = Selector('[data-qa="start-pairing-btn"]')
  readonly mobilePairingTitle1 = Selector(
    '[data-qa="mobile-pairing-wizard-title-1"]'
  )
  readonly mobilePairingTitle3 = Selector(
    '[data-qa="mobile-pairing-wizard-title-3"]'
  )
  readonly challengeKeyInput = Selector('[data-qa="challenge-key-input"]')
  readonly submitChallengeKeyBtn = Selector(
    '[data-qa="submit-challenge-key-btn"]'
  )
  readonly responseKey = Selector('[data-qa="response-key"]')
  readonly unitPageLink = Selector('[data-qa="unit-page-link"]')
  readonly unitName = Selector('[data-qa="unit-name"]')
}
