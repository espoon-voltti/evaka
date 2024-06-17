// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput } from '../../../utils/page'

export class PairingFlow {
  constructor(private readonly page: Page) {}

  #mobileStartPairingBtn = this.page.findByDataQa('start-pairing-btn')
  #mobilePairingTitle1 = this.page.findByDataQa('mobile-pairing-wizard-title-1')
  #mobilePairingTitle3 = this.page.findByDataQa('mobile-pairing-wizard-title-3')
  #challengeKeyInput = new TextInput(
    this.page.findByDataQa('challenge-key-input')
  )
  #submitChallengeKeyBtn = this.page.findByDataQa('submit-challenge-key-btn')
  #responseKey = this.page.findByDataQa('response-key')
  #startCtaLink = this.page.findByDataQa('start-cta-link')
  #topBarTitle = this.page.findByDataQa('top-bar-title')

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
    return this.#responseKey.text
  }

  isPairingWizardFinished() {
    return this.#mobilePairingTitle3.visible
  }

  async clickStartCta() {
    await this.#startCtaLink.click()
    await this.#topBarTitle.waitUntilVisible()
  }
}
