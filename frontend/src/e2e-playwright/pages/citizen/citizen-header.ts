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
    const isContainedInApplyingSubheader = [
      'applications',
      'decisions',
      'income'
    ].includes(tab)
    if (this.type === 'mobile') {
      await this.#menuButton.click()
    }
    if (tab !== 'income') {
      if (isContainedInApplyingSubheader) {
        await this.page
          .locator(`[data-qa="applying-nav"]`)
          .locator(`[data-qa="nav-${tab}"]`)
          .click()
        await this.page.waitForSelector('[data-qa="applying-subnavigation"]', {
          state: 'visible'
        })
        await this.page
          .locator(`[data-qa="applying-subnavigation"]`)
          .locator(`[data-qa="nav-${tab}"]`)
          .click()
      }
      await this.page
        .locator(`[data-qa="${this.type}-nav"]`)
        .locator(`[data-qa="nav-${tab}"]`)
        .click()
    } else {
      await this.page
        .locator(`[data-qa="${this.type}-nav"]`)
        .locator(`[data-qa="user-menu-title"]`)
        .click()
      await this.page.waitForSelector('[data-qa="user-menu-income"]', {
        state: 'visible'
      })
      await this.page.locator(`[data-qa="user-menu-income"]`).click()
    }
  }

  async selectLanguage(lang: 'fi' | 'sv' | 'en') {
    await this.#languageMenuToggle.click()
    await this.#languageOptionList.find(`[data-qa="lang-${lang}"]`).click()
  }
}
