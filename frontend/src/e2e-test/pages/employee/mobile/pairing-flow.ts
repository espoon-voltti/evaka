// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput } from '../../../utils/page'

export class PairingFlow {
  constructor(private readonly page: Page) {}

  #mobileStartPairingBtn = this.page.find('[data-qa="start-pairing-btn"]')
  #mobilePairingTitle1 = this.page.find(
    '[data-qa="mobile-pairing-wizard-title-1"]'
  )
  #mobilePairingTitle3 = this.page.find(
    '[data-qa="mobile-pairing-wizard-title-3"]'
  )
  #challengeKeyInput = new TextInput(
    this.page.find('[data-qa="challenge-key-input"]')
  )
  #submitChallengeKeyBtn = this.page.find(
    '[data-qa="submit-challenge-key-btn"]'
  )
  #responseKey = this.page.find('[data-qa="response-key"]')
  #startCtaLink = this.page.find('[data-qa="start-cta-link"]')
  #topBarTitle = this.page.find('[data-qa="top-bar-title"]')

  async startPairing() {
    await this.#mobileStartPairingBtn.click()
    await this.#mobilePairingTitle1.waitUntilVisible()
  }

  async submitChallengeKey(challengeKey: string) {
    await this.#challengeKeyInput.type(challengeKey)
    await this.#submitChallengeKeyBtn.click()
  }

  async getResponseKey() {
    await this.#responseKey.waitUntilVisible()
    return this.#responseKey.innerText
  }

  isPairingWizardFinished() {
    return this.#mobilePairingTitle3.visible
  }

  async clickStartCta() {
    await this.#startCtaLink.click()
    await this.#topBarTitle.waitUntilVisible()
  }
}
