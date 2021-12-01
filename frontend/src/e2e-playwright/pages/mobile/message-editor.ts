// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from 'e2e-playwright/utils'
import { Page } from 'playwright'

export default class MobileMessageEditorPage {
  constructor(private readonly page: Page) {}

  #inputTitle = this.page.locator('[data-qa="input-title"]')
  #inputContent = this.page.locator('[data-qa="input-content"]')
  #sendMessageButton = this.page.locator('[data-qa="send-message-btn"]')

  async getEditorState() {
    return this.page
      .locator('[data-qa="message-editor"]')
      .getAttribute('data-status')
  }

  async isEditorVisible() {
    return this.#inputContent.evaluateAll(
      (contentInput) => contentInput.length > 0
    )
  }

  async draftNewMessage(title: string, content: string) {
    await waitUntilEqual(() => this.isEditorVisible(), true)
    await this.#inputTitle.fill(title)
    await this.#inputContent.fill(content)
    await this.page.keyboard.press('Enter')
    await waitUntilEqual(() => this.getEditorState(), 'clean')
  }

  async sendEditedMessage() {
    await this.#sendMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), false)
  }
}
