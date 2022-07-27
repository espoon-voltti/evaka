// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../utils'
import type { Page } from '../../utils/page'
import { Checkbox, TextInput } from '../../utils/page'

export class IncomeStatementPage {
  constructor(private readonly page: Page) {}

  #form = this.page.find(`[data-qa="handler-notes-form"]`)

  #handledCheckbox = new Checkbox(this.#form.find('[data-qa="set-handled"]'))
  #noteInput = new TextInput(this.#form.find('input[type="text"]'))
  #submitBtn = this.#form.find('button')

  #childOtherInfo = this.page.find('[data-qa="other-info"]')
  #attachments = this.page.findAll('[data-qa="attachments"]')
  #noAttachments = this.page.find('[data-qa="no-attachments"]')

  async assertChildIncomeStatement(
    expectedOtherInfo: string,
    expectedAttachmentsCount: number
  ) {
    await waitUntilEqual(
      () => this.#childOtherInfo.textContent,
      expectedOtherInfo
    )
    expectedAttachmentsCount > 0
      ? await this.#attachments.assertCount(expectedAttachmentsCount)
      : await this.#noAttachments.waitUntilVisible()
  }

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
