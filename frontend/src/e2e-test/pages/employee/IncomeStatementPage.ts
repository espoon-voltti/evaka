// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Page, Element, ElementCollection } from '../../utils/page'
import { TextInput } from '../../utils/page'

export class IncomeStatementPage {
  #form: Element
  #childOtherInfo: Element
  #noAttachments: Element
  #status: Element
  #moveToHandlingBtn: Element
  #returnToSentBtn: Element
  #markHandledBtn: Element
  #returnToHandlingBtn: Element
  #noteInput: TextInput
  #submitBtn: Element
  #attachments: ElementCollection
  constructor(page: Page) {
    this.#form = page.findByDataQa(`handler-notes-form`)
    this.#childOtherInfo = page.findByDataQa('other-info')
    this.#noAttachments = page.findByDataQa('no-attachments')
    this.#status = page.findByDataQa('income-statement-status')
    this.#moveToHandlingBtn = page.findByDataQa('move-to-handling-btn')
    this.#returnToSentBtn = page.findByDataQa('return-to-sent-btn')
    this.#markHandledBtn = page.findByDataQa('mark-handled-btn')
    this.#returnToHandlingBtn = page.findByDataQa('return-to-handling-btn')
    this.#noteInput = new TextInput(this.#form.find('input[type="text"]'))
    this.#submitBtn = this.#form.findByDataQa('submit-btn')
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

  async assertStatus(expectedStatus: string) {
    await this.#status.assertTextEquals(expectedStatus)
  }

  async moveToHandling() {
    await this.#moveToHandlingBtn.click()
  }

  async returnToSent() {
    await this.#returnToSentBtn.click()
  }

  async markHandled() {
    await this.#markHandledBtn.click()
  }

  async returnToHandling() {
    await this.#returnToHandlingBtn.click()
  }

  async submit() {
    await this.#submitBtn.click()
  }
}
