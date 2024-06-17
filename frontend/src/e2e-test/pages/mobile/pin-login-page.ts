// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Select, TextInput } from '../../utils/page'

export default class PinLoginPage {
  constructor(private readonly page: Page) {}

  #staffSelect = new Select(this.page.findByDataQa('select-staff'))
  #pinInput = new TextInput(this.page.findByDataQa('pin-input'))
  #pinSubmit = new TextInput(this.page.findByDataQa('pin-submit'))
  #pinInfo = this.page.findByDataQa('pin-input-info')

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
