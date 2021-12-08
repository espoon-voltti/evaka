// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Checkbox, TextInput } from 'e2e-playwright/utils/page'

export class IncomeStatementPage {
  constructor(private readonly page: Page) {}

  #form = this.page.find(`[data-qa="handler-notes-form"]`)

  #handledCheckbox = new Checkbox(this.#form.find('[data-qa="set-handled"]'))
  #noteInput = new TextInput(this.#form.find('input[type="text"]'))
  #submitBtn = this.#form.find('button')

  async typeHandlerNote(text: string) {
    await this.#noteInput.fill(text)
  }

  async setHandled(handled = true) {
    handled
      ? await this.#handledCheckbox.check()
      : await this.#handledCheckbox.uncheck()
  }

  async submit() {
    await this.#submitBtn.click()
  }
}
