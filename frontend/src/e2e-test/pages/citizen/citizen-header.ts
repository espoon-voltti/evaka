// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Lang } from 'lib-customizations/citizen'

import { waitUntilEqual, waitUntilFalse } from '../../utils'
import { Page } from '../../utils/page'

export default class CitizenHeader {
  constructor(
    private readonly page: Page,
    private readonly type: 'desktop' | 'mobile' = 'desktop'
  ) {}

  #languageMenuToggle = this.page.find('[data-qa="button-select-language"]')
  #languageOptionList = this.page.find('[data-qa="select-lang"]')
  #unreadChildrenCount = this.page.findAllByDataQa(
    `nav-children-${this.type}-notification-count`
  )

  #languageOption(lang: Lang) {
    return this.#languageOptionList.find(`[data-qa="lang-${lang}"]`)
  }

  async waitUntilLoggedIn() {
    await this.page.findByDataQa(`nav-children-${this.type}`).waitUntilVisible()
  }

  async selectTab(
    tab:
      | 'applications'
      | 'decisions'
      | 'income'
      | 'calendar'
      | 'children'
      | 'messages'
      | 'personal-details'
  ) {
    const isContainedInSubnav = [
      'applications',
      'decisions',
      'income',
      'personal-details'
    ].includes(tab)
    if (isContainedInSubnav) {
      await this.page.findByDataQa(`sub-nav-menu-${this.type}`).click()
      await this.page.findByDataQa(`sub-nav-menu-${tab}`).click()
    } else {
      await this.page.findByDataQa(`nav-${tab}-${this.type}`).click()
    }
  }

  async selectLanguage(lang: 'fi' | 'sv' | 'en') {
    await this.#languageMenuToggle.click()
    await this.#languageOption(lang).click()
  }

  async listLanguages(): Promise<Record<Lang, boolean>> {
    const isVisible = (lang: Lang) => this.#languageOption(lang).visible
    await this.#languageMenuToggle.click()
    const languages = {
      fi: await isVisible('fi'),
      sv: await isVisible('sv'),
      en: await isVisible('en')
    }
    await this.#languageMenuToggle.click()
    return languages
  }

  async assertDOMLangAttrib(lang: 'fi' | 'sv' | 'en') {
    await this.page.find(`html[lang=${lang}]`).waitUntilVisible()
  }

  async assertChildrenTabHasText(text: string) {
    await this.page
      .findByDataQa('nav-children-desktop')
      .findByDataQa('nav-text')
      .findText(text)
      .waitUntilVisible()
  }

  async checkPersonalDetailsAttentionIndicatorsAreShown() {
    await this.page
      .findByDataQa(`attention-indicator-sub-menu-${this.type}`)
      .waitUntilVisible()
    await this.page.findByDataQa(`sub-nav-menu-${this.type}`).click()
    await this.page
      .findByDataQa('personal-details-notification')
      .waitUntilVisible()
    await this.page.findByDataQa(`sub-nav-menu-${this.type}`).click()
  }

  async assertUnreadChildrenCount(expectedCount: number) {
    expectedCount != 0
      ? await waitUntilEqual(
          () => this.#unreadChildrenCount.first().textContent,
          expectedCount.toString()
        )
      : await waitUntilFalse(() => this.#unreadChildrenCount.first().visible)
  }
}
