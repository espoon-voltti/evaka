// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RawElement } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class CitizenHeader {
  constructor(
    private readonly page: Page,
    private readonly type: 'desktop' | 'mobile' = 'desktop'
  ) {}

  #menuButton = this.page.locator('[data-qa="menu-button"]')
  #loginButton = this.page
    .locator(`[data-qa="${this.type}-nav"]`)
    .locator('[data-qa="login-btn"]')
  #languageMenuToggle = new RawElement(
    this.page,
    '[data-qa="button-select-language"]'
  )
  #languageOptionList = new RawElement(this.page, '[data-qa="select-lang"]')
  applicationsTab = new RawElement(this.page, '[data-qa="nav-applications"]')

  async logIn() {
    if (this.type === 'mobile') {
      await this.#menuButton.click()
    }
    await this.#loginButton.click()
  }

  async selectTab(tab: 'applications' | 'decisions' | 'income' | 'calendar') {
    if (this.type === 'mobile') {
      await this.#menuButton.click()
    }
    await this.page
      .locator(`[data-qa="${this.type}-nav"]`)
      .locator(`[data-qa="nav-${tab}"]`)
      .click()
  }

  async selectLanguage(lang: 'fi' | 'sv' | 'en') {
    await this.#languageMenuToggle.click()
    await this.#languageOptionList.find(`[data-qa="lang-${lang}"]`).click()
  }
}
