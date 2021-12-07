// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'e2e-playwright/utils/page'

export default class CitizenHeader {
  constructor(
    private readonly page: Page,
    private readonly type: 'desktop' | 'mobile' = 'desktop'
  ) {}

  #menuButton = this.page.find('[data-qa="menu-button"]')
  #loginButton = this.page.find(
    `[data-qa="${this.type}-nav"] [data-qa="login-btn"]`
  )
  #languageMenuToggle = this.page.find('[data-qa="button-select-language"]')
  #languageOptionList = this.page.find('[data-qa="select-lang"]')
  #userMenu = this.page.find(`[data-qa="user-menu-title-${this.type}"]`)
  applyingTab = this.page.find('[data-qa="nav-applying"]')

  async logIn() {
    if (this.type === 'mobile') {
      await this.#menuButton.click()
    }
    await this.#loginButton.click()
  }

  async waitUntilLoggedIn() {
    await this.#userMenu.waitUntilVisible()
  }

  async selectTab(
    tab:
      | 'applications'
      | 'decisions'
      | 'income'
      | 'calendar'
      | 'pedagogical-documents'
  ) {
    const isContainedInApplyingSubheader = [
      'applications',
      'decisions'
    ].includes(tab)
    if (this.type === 'mobile') {
      await this.#menuButton.click()
    }
    if (tab !== 'income') {
      if (isContainedInApplyingSubheader) {
        await this.page
          .find(`[data-qa="${this.type}-nav"] [data-qa="nav-applying"]`)
          .click()
        await this.page
          .find('[data-qa="applying-subnavigation"]')
          .waitUntilVisible()
        await this.page
          .find(`[data-qa="applying-subnavigation"] [data-qa="${tab}-tab"]`)
          .click()
      } else {
        await this.page
          .find(`[data-qa="${this.type}-nav"] [data-qa="nav-${tab}"]`)
          .click()
      }
    } else {
      await this.#userMenu.click()
      await this.page.find('[data-qa="user-menu-income"]').waitUntilVisible()
      await this.page.find(`[data-qa="user-menu-income"]`).click()
    }
  }

  async selectLanguage(lang: 'fi' | 'sv' | 'en') {
    await this.#languageMenuToggle.click()
    await this.#languageOptionList.find(`[data-qa="lang-${lang}"]`).click()
  }

  async checkPersonalDetailsAttentionIndicatorsAreShown() {
    const attentionIndicator = this.page.find(
      `[data-qa="attention-indicator-${this.type}"]`
    )
    const personalDetailsAttentionIndicator = this.page.find(
      `[data-qa="personal-details-attention-indicator-${this.type}"]`
    )

    await attentionIndicator.waitUntilVisible()
    if (this.type === 'mobile') {
      await this.#menuButton.click()
      await attentionIndicator.waitUntilVisible()
    }

    await this.#userMenu.click()
    await personalDetailsAttentionIndicator.waitUntilVisible()
  }

  async navigateToPersonalDetailsPage() {
    const personalDetailsLink = this.page.find(
      '[data-qa="user-menu-personal-details"]'
    )

    if (this.type === 'mobile' && !(await this.#userMenu.visible)) {
      await this.#menuButton.click()
    }

    if (!(await personalDetailsLink.visible)) {
      await this.#userMenu.click()
    }

    await personalDetailsLink.click()
  }
}
