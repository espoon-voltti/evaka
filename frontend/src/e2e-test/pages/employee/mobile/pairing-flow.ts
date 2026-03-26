// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { expect } from '../../../playwright'
import type { Page, Element } from '../../../utils/page'
import { TextInput } from '../../../utils/page'

export class PairingFlow {
  #mobileStartPairingBtn: Element
  #mobilePairingTitle1: Element
  #mobilePairingTitle3: Element
  #challengeKeyInput: TextInput
  #submitChallengeKeyBtn: Element
  #responseKey: Element
  #startCtaLink: Element
  #topBarTitle: Element
  constructor(readonly page: Page) {
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
    await expect(this.#mobilePairingTitle1).toBeVisible()
  }

  async submitChallengeKey(challengeKey: string) {
    await this.#challengeKeyInput.type(challengeKey)
    await this.#submitChallengeKeyBtn.click()
  }

  async getResponseKey() {
    await expect(this.#responseKey).toBeVisible()
    return this.#responseKey.text
  }

  async waitUntilPairingWizardFinished() {
    await expect(this.#mobilePairingTitle3).toBeVisible()
  }

  async clickStartCta() {
    await this.#startCtaLink.click()
    await expect(this.#topBarTitle).toBeVisible()
  }
}
