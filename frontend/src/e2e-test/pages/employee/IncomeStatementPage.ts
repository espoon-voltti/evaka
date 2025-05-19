// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page, Element, ElementCollection } from '../../utils/page'
import { Checkbox, TextInput } from '../../utils/page'

export class IncomeStatementPage {
  #form: Element
  #childOtherInfo: Element
  #noAttachments: Element
  #handledCheckbox: Checkbox
  #noteInput: TextInput
  #submitBtn: Element
  #attachments: ElementCollection
  constructor(page: Page) {
    this.#form = page.findByDataQa(`handler-notes-form`)
    this.#childOtherInfo = page.findByDataQa('other-info')
    this.#noAttachments = page.findByDataQa('no-attachments')
    this.#handledCheckbox = new Checkbox(this.#form.findByDataQa('set-handled'))
    this.#noteInput = new TextInput(this.#form.find('input[type="text"]'))
    this.#submitBtn = this.#form.find('button')
    this.#attachments = page.findAll('[data-qa="attachments"]')
  }

  async assertChildIncomeStatement(
    expectedOtherInfo: string,
    expectedAttachmentsCount: number
  ) {
    await this.#childOtherInfo.assertTextEquals(expectedOtherInfo)
    if (expectedAttachmentsCount > 0) {
      await this.#attachments.assertCount(expectedAttachmentsCount)
    } else {
      await this.#noAttachments.waitUntilVisible()
    }
  }

  async typeHandlerNote(text: string) {
    await this.#noteInput.fill(text)
  }

  async setHandled(handled = true) {
    if (handled) {
      await this.#handledCheckbox.check()
    } else {
      await this.#handledCheckbox.uncheck()
    }
  }

  async submit() {
    await this.#submitBtn.click()
  }
}
