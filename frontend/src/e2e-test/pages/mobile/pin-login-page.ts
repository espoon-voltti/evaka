// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Select, TextInput, Element } from '../../utils/page'

export default class PinLoginPage {
  #staffSelect: Select
  #pinInput: TextInput
  #pinSubmit: TextInput
  #pinInfo: Element
  constructor(readonly page: Page) {
    this.#staffSelect = new Select(page.findByDataQa('select-staff'))
    this.#pinInput = new TextInput(page.findByDataQa('pin-input'))
    this.#pinSubmit = new TextInput(page.findByDataQa('pin-submit'))
    this.#pinInfo = page.findByDataQa('pin-input-info')
  }

  async submitPin(pin: string) {
    await this.#pinInput.fill(pin)
    await this.#pinSubmit.click()
  }

  async login(name: string, pin: string) {
    await this.#staffSelect.selectOption({ label: name })
    await this.submitPin(pin)
  }

  async personalDeviceLogin(pin: string) {
    // The staff member is not asked on personal mobile device
    await this.submitPin(pin)
  }

  async assertWrongPinError() {
    await this.#pinInfo.assertTextEquals('Väärä PIN-koodi')
  }
}
