// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, TextInput, Element } from '../../../utils/page'

export class PairingFlow {
  #mobileStartPairingBtn: Element
  #mobilePairingTitle1: Element
  #mobilePairingTitle3: Element
  #challengeKeyInput: TextInput
  #submitChallengeKeyBtn: Element
  #responseKey: Element
  #startCtaLink: Element
  #topBarTitle: Element
  constructor(private readonly page: Page) {
    this.#mobileStartPairingBtn = page.findByDataQa('start-pairing-btn')
    this.#mobilePairingTitle1 = page.findByDataQa(
      'mobile-pairing-wizard-title-1'
    )
    this.#mobilePairingTitle3 = page.findByDataQa(
      'mobile-pairing-wizard-title-3'
    )
    this.#challengeKeyInput = new TextInput(
      page.findByDataQa('challenge-key-input')
    )
    this.#submitChallengeKeyBtn = page.findByDataQa('submit-challenge-key-btn')
    this.#responseKey = page.findByDataQa('response-key')
    this.#startCtaLink = page.findByDataQa('start-cta-link')
    this.#topBarTitle = page.findByDataQa('top-bar-title')
  }

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
