import { Selector, t } from 'testcafe'

export default class CitizenHomepage {
  readonly nav = {
    applications: Selector('[data-qa="nav-old-applications"]'),
    decisions: Selector('[data-qa="nav-decisions"]')
  }

  readonly language = {
    button: Selector('[data-qa="button-select-language"]'),
    select: (lang: 'fi' | 'sv' | 'en') =>
      Selector('[data-qa="select-lang"]').find(`[data-qa="lang-${lang}"]`)
  }

  async selectLanguage(lang: 'fi' | 'sv' | 'en') {
    await t.click(this.language.button)
    await t.click(this.language.select(lang))
  }
}
