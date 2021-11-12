// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilTrue } from 'e2e-playwright/utils'
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
  #userMenu = new RawElement(
    this.page,
    `[data-qa="user-menu-title-${this.type}"]`
  )
  applyingTab = new RawElement(this.page, '[data-qa="nav-applying"]')

  async logIn() {
    if (this.type === 'mobile') {
      await this.#menuButton.click()
    }
    await this.#loginButton.click()
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
          .locator(`[data-qa="${this.type}-nav"]`)
          .locator(`[data-qa="nav-applying"]`)
          .click()
        await this.page.waitForSelector('[data-qa="applying-subnavigation"]', {
          state: 'visible'
        })
        await this.page
          .locator(`[data-qa="applying-subnavigation"]`)
          .locator(`[data-qa="${tab}-tab"]`)
          .click()
      } else {
        await this.page
          .locator(`[data-qa="${this.type}-nav"]`)
          .locator(`[data-qa="nav-${tab}"]`)
          .click()
      }
    } else {
      await this.#userMenu.click()
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

  async checkPersonalDetailsAttentionIndicatorsAreShown() {
    const attentionIndicatorSelector = `[data-qa="attention-indicator-${this.type}"]`
    const personalDetailsAttentionIndicatorSelector = `[data-qa="personal-details-attention-indicator-${this.type}"]`

    await waitUntilTrue(() => this.page.isVisible(attentionIndicatorSelector))
    if (this.type === 'mobile') {
      await this.#menuButton.click()
      await waitUntilTrue(() => this.page.isVisible(attentionIndicatorSelector))
    }

    await this.#userMenu.click()
    await waitUntilTrue(() =>
      this.page.isVisible(personalDetailsAttentionIndicatorSelector)
    )
  }

  async navigateToPersonalDetailsPage() {
    const personalDetailsLinkSelector = '[data-qa="user-menu-personal-details"]'

    if (this.type === 'mobile' && !(await this.#userMenu.visible)) {
      await this.#menuButton.click()
    }

    if (!(await this.page.locator(personalDetailsLinkSelector).isVisible())) {
      await this.#userMenu.click()
    }

    await this.page.locator(personalDetailsLinkSelector).click()
  }
}
