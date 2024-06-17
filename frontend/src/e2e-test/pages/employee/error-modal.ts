// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../utils/page'

export default class ErrorModal {
  constructor(private page: Page) {}

  #modal = this.page.findByDataQa('app-error-modal')
  #title = this.#modal.find('[data-qa="title"]')
  #text = this.#modal.find('[data-qa="text"]')

  async ensureTitle(title: string) {
    await this.#title.findText(title).waitUntilVisible()
  }

  async ensureText(title: string) {
    await this.#text.findText(title).waitUntilVisible()
  }
}
