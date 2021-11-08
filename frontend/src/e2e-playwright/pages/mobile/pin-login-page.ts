// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { waitUntilEqual } from '../../utils'

export default class PinLoginPage {
  constructor(private readonly page: Page) {}

  #staffSelect = this.page.locator('[data-qa="select-staff"] select')
  #pinInput = this.page.locator('[data-qa="pin-input"]')
  #pinInfo = this.page.locator('[data-qa="pin-input-info"]')

  async submitPin(pin: string) {
    await this.#pinInput.selectText()
    await this.#pinInput.type(pin)
    await this.page.keyboard.press('Enter')
  }

  async login(name: string, pin: string) {
    await this.#staffSelect.selectOption({ label: name })
    await this.submitPin(pin)
  }

  async assertWrongPinError() {
    await waitUntilEqual(() => this.#pinInfo.innerText(), 'Väärä PIN-koodi')
  }
}
