// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from 'e2e-playwright/utils'
import { Page, TextInput } from 'e2e-playwright/utils/page'

export default class MobileMessageEditorPage {
  constructor(private readonly page: Page) {}

  #inputTitle = new TextInput(this.page.find('[data-qa="input-title"]'))
  #inputContent = new TextInput(this.page.find('[data-qa="input-content"]'))
  #sendMessageButton = this.page.find('[data-qa="send-message-btn"]')

  async getEditorState() {
    return this.page
      .find('[data-qa="message-editor"]')
      .getAttribute('data-status')
  }

  async isEditorVisible() {
    return this.#inputContent.visible
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
