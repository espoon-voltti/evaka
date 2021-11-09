// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { waitUntilVisible } from '../../../utils'

export class PairingFlow {
  constructor(private readonly page: Page) {}
  #mobileStartPairingBtn = this.page.locator('[data-qa="start-pairing-btn"]')
  #mobilePairingTitle1 = this.page.locator(
    '[data-qa="mobile-pairing-wizard-title-1"]'
  )
  #mobilePairingTitle3 = this.page.locator(
    '[data-qa="mobile-pairing-wizard-title-3"]'
  )
  #challengeKeyInput = this.page.locator('[data-qa="challenge-key-input"]')
  #submitChallengeKeyBtn = this.page.locator(
    '[data-qa="submit-challenge-key-btn"]'
  )
  #responseKey = this.page.locator('[data-qa="response-key"]')
  #startCtaLink = this.page.locator('[data-qa="start-cta-link"]')
  #topBarTitle = this.page.locator('[data-qa="top-bar-title"]')

  async startPairing() {
    await this.#mobileStartPairingBtn.click()
    await waitUntilVisible(this.#mobilePairingTitle1)
  }

  async submitChallengeKey(challengeKey: string) {
    await this.#challengeKeyInput.type(challengeKey)
    await this.#submitChallengeKeyBtn.click()
  }

  async getResponseKey() {
    await waitUntilVisible(this.#responseKey)
    return this.#responseKey.innerText()
  }

  isPairingWizardFinished() {
    return this.#mobilePairingTitle3.isVisible()
  }

  async clickStartCta() {
    await this.#startCtaLink.click()
    await waitUntilVisible(this.#topBarTitle)
  }
}
