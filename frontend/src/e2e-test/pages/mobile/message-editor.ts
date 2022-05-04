// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../utils'
import { Checkbox, Page, TextInput } from '../../utils/page'

export default class MobileMessageEditorPage {
  constructor(private readonly page: Page) {}

  #inputTitle = new TextInput(this.page.find('[data-qa="input-title"]'))
  #inputContent = new TextInput(this.page.find('[data-qa="input-content"]'))
  #sendMessageButton = this.page.find('[data-qa="send-message-btn"]')
  #urgent = new Checkbox(this.page.findByDataQa('checkbox-urgent'))
  noReceiversInfo = this.page.find('[data-qa="info-no-receivers"]')

  async getEditorState() {
    return this.page
      .find('[data-qa="message-editor"]')
      .getAttribute('data-status')
  }

  async isEditorVisible() {
    return this.#inputContent.visible
  }

  async draftNewMessage(message: {
    title: string
    content: string
    urgent?: boolean
  }) {
    await waitUntilEqual(() => this.isEditorVisible(), true)
    await this.#inputTitle.fill(message.title)
    await this.#inputContent.fill(message.content)
    if (message.urgent ?? false) {
      await this.#urgent.check()
    } else {
      await this.#urgent.uncheck()
    }
    await this.page.keyboard.press('Enter')
    await waitUntilEqual(() => this.getEditorState(), 'clean')
  }

  async sendEditedMessage() {
    await this.#sendMessageButton.click()
    await waitUntilEqual(() => this.isEditorVisible(), false)
  }
}
