// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from 'e2e-playwright/utils'
import {
  RawElementDEPRECATED,
  RawTextInput
} from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class MobileMessageEditorPage {
  constructor(private readonly page: Page) {}

  #inputTitle = new RawTextInput(this.page, '[data-qa="input-title"]')
  #inputContent = new RawTextInput(this.page, '[data-qa="input-content"]')
  #sendMessageButton = new RawElementDEPRECATED(
    this.page,
    '[data-qa="send-message-btn"]'
  )

  async getEditorState() {
    return new RawElementDEPRECATED(
      this.page,
      '[data-qa="message-editor"]'
    ).getAttribute('data-status')
  }

  async isEditorVisible() {
    return this.page.$$eval(
      '[data-qa="input-content"]',
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
