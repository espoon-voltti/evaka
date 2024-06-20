// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../utils/page'

export default class ErrorModal {
  #modal: Element
  #title: Element
  #text: Element
  constructor(page: Page) {
    this.#modal = page.findByDataQa('app-error-modal')
    this.#title = this.#modal.find('[data-qa="title"]')
    this.#text = this.#modal.find('[data-qa="text"]')
  }

  async ensureTitle(title: string) {
    await this.#title.findText(title).waitUntilVisible()
  }

  async ensureText(title: string) {
    await this.#text.findText(title).waitUntilVisible()
  }
}
