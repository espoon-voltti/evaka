// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

export class IncomeStatementPage {
  constructor(private readonly page: Page) {}

  #form = this.page.locator(`[data-qa="handler-notes-form"]`)

  #handledCheckbox = this.#form.locator('input[type="checkbox"]')
  #noteInput = this.#form.locator('input[type="text"]')
  #submitBtn = this.#form.locator('button')

  async typeHandlerNote(text: string) {
    await this.#noteInput.type(text)
  }

  async setHandled(handled = true) {
    handled
      ? await this.#handledCheckbox.check({ force: true })
      : await this.#handledCheckbox.uncheck({ force: true })
  }

  async submit() {
    await this.#submitBtn.click()
  }
}
