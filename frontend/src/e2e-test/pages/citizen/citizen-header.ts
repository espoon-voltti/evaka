// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Lang } from 'lib-customizations/citizen'

import { waitUntilFalse } from '../../utils'
import { Page, Element, EnvType } from '../../utils/page'

export default class CitizenHeader {
  #languageMenuToggle: Element
  #languageOptionList: Element
  #childrenNav: Element
  #messagesNav: Element
  #unreadChildrenCount: Element
  #unreadMessagesCount: Element
  #subNavMenu: Element
  constructor(
    private readonly page: Page,
    private readonly type: EnvType = 'desktop'
  ) {
    this.#languageMenuToggle = page
      .findAllByDataQa('button-select-language')
      .last()
    this.#languageOptionList = page.findByDataQa('select-lang')
    this.#childrenNav = page.findByDataQa(`nav-children-${this.type}`)
    this.#messagesNav = page.findByDataQa(`nav-messages-${this.type}`)
    this.#unreadChildrenCount = page.findByDataQa(
      `nav-children-${this.type}-notification-count`
    )
    this.#unreadMessagesCount = page.findByDataQa(
      `nav-messages-${this.type}-notification-count`
    )
    this.#subNavMenu = page.findByDataQa(`sub-nav-menu-${this.type}`)
  }

  #languageOption(lang: Lang) {
    return this.#languageOptionList.find(`[data-qa="lang-${lang}"]`)
  }

  async #toggleChildrenMenu() {
    return this.#childrenNav.click()
  }

  async selectTab(
    tab:
      | 'applications'
      | 'decisions'
      | 'income'
      | 'calendar'
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
      await this.#subNavMenu.click()
      await this.page.findByDataQa(`sub-nav-menu-${tab}`).click()
    } else {
      await this.page.findByDataQa(`nav-${tab}-${this.type}`).click()
    }
  }

  async assertNoTab(
    tab:
      | 'applications'
      | 'decisions'
      | 'income'
      | 'calendar'
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
      await this.#subNavMenu.click()
      await this.page.findByDataQa(`sub-nav-menu-${tab}`).waitUntilHidden()
    } else {
      await this.page.findByDataQa(`nav-${tab}-${this.type}`).waitUntilHidden()
    }
  }

  async openChildPage(childId: string) {
    await this.#childrenNav.waitUntilVisible()
    if (
      (await this.#childrenNav.findByDataQa('drop-down-icon').visible) ||
      this.type === 'mobile'
    ) {
      await this.#toggleChildrenMenu()
      const selector = this.page.findByDataQa(`children-menu-${childId}`)
      if (await selector.visible) {
        await selector.click()
      }
    } else {
      await this.#childrenNav.click()
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

  async assertSubNavMenuHasText(text: string) {
    await this.page
      .findByDataQa(`sub-nav-menu-${this.type}`)
      .findText(text)
      .waitUntilVisible()
  }

  async checkPersonalDetailsAttentionIndicatorsAreShown() {
    await this.page
      .findByDataQa(`attention-indicator-sub-menu-${this.type}`)
      .waitUntilVisible()
    await this.#subNavMenu.click()
    await this.page
      .findByDataQa('personal-details-notification')
      .waitUntilVisible()
    await this.#subNavMenu.click()
  }

  async assertUnreadMessagesCount(expectedCount: number) {
    await this.#messagesNav.waitUntilVisible()
    if (expectedCount !== 0) {
      await this.#unreadMessagesCount.assertTextEquals(expectedCount.toString())
    } else {
      await waitUntilFalse(() => this.#unreadMessagesCount.visible)
    }
  }

  async assertUnreadChildrenCount(expectedCount: number) {
    await this.#childrenNav.waitUntilVisible()
    if (expectedCount !== 0) {
      await this.#unreadChildrenCount.assertTextEquals(expectedCount.toString())
    } else {
      await waitUntilFalse(() => this.#unreadChildrenCount.visible)
    }
  }

  async assertNoChildrenTab() {
    await this.#childrenNav.waitUntilHidden()
  }

  async logout() {
    await this.#subNavMenu.click()
    await this.page.findByDataQa(`sub-nav-menu-logout`).click()
  }
}
