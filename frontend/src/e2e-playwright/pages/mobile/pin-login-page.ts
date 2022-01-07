// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../utils'
import { Page, Select, TextInput } from '../../utils/page'

export default class PinLoginPage {
  constructor(private readonly page: Page) {}

  #staffSelect = new Select(this.page.find('[data-qa="select-staff"]'))
  #pinInput = new TextInput(this.page.find('[data-qa="pin-input"]'))
  #pinSubmit = new TextInput(this.page.find('[data-qa="pin-submit"]'))
  #pinInfo = this.page.find('[data-qa="pin-input-info"]')

  async submitPin(pin: string) {
    await this.#pinInput.fill(pin)
    await this.#pinSubmit.click()
  }

  async login(name: string, pin: string) {
    await this.#staffSelect.selectOption({ label: name })
    await this.submitPin(pin)
  }

  async assertWrongPinError() {
    await waitUntilEqual(() => this.#pinInfo.innerText, 'Väärä PIN-koodi')
  }
}
