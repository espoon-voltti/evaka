import { RawElement } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class CitizenHeader {
  constructor(private readonly page: Page) {}

  #languageMenuToggle = new RawElement(
    this.page,
    '[data-qa="button-select-language"]'
  )
  #languageOptionList = new RawElement(this.page, '[data-qa="select-lang"]')
  applicationsTab = new RawElement(this.page, '[data-qa="nav-applications"]')
  decisionsTab = new RawElement(this.page, '[data-qa="nav-decisions"]')

  async selectLanguage(lang: 'fi' | 'sv' | 'en') {
    await this.#languageMenuToggle.click()
    await this.#languageOptionList.find(`[data-qa="lang-${lang}"]`).click()
  }
}
