// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'

export class Idp {
  private readonly user = Selector('#username')
  private readonly pass = Selector('#password')
  private readonly proceed = Selector('[name="_eventId_proceed"]')
  private readonly globalConsent = Selector('#_shib_idp_globalConsent')

  async login(user: string, pass: string) {
    await t.typeText(this.user, user, { paste: true })
    await t.typeText(this.pass, pass, { paste: true })
    await t.click(this.proceed)
    await t.click(this.globalConsent)
    await t.click(this.proceed)
  }
}
