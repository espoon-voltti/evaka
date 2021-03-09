// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'

export default class CitizenHomepage {
  readonly nav = {
    applications: Selector('[data-qa="nav-applications"]'),
    decisions: Selector('[data-qa="nav-decisions"]'),
    messages: Selector('[data-qa="nav-messages"]')
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

  readonly unresolvedDecisionsInfoBox = Selector(
    '[data-qa="alert-box-unconfirmed-decisions-count"]'
  )
}
