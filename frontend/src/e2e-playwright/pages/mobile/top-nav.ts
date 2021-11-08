// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

export default class TopNav {
  constructor(private readonly page: Page) {}

  #logoutBtn = this.page.locator('[data-qa="logout-btn"]')

  async logout() {
    await this.#logoutBtn.click()
  }

  getUserInitials(): Promise<string> {
    return this.#logoutBtn.innerText()
  }
}
