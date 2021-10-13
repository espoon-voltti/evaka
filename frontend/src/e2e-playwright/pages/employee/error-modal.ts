// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import { waitUntilVisible } from '../../utils'

export default class ErrorModal {
  constructor(private page: Page) {}

  #modal = this.page.locator('[data-qa="app-error-modal"]')
  #title = this.#modal.locator('[data-qa="title"]')
  #text = this.#modal.locator('[data-qa="text"]')

  async ensureTitle(title: string) {
    await waitUntilVisible(this.#title.locator(`text=${title}`))
  }

  async ensureText(title: string) {
    await waitUntilVisible(this.#text.locator(`text=${title}`))
  }
}
