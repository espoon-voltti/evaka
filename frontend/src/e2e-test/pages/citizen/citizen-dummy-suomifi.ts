// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element } from '../../utils/page'

export class DummySuomiFiLoginPage {
  loginButton: Element

  constructor(private page: Page) {
    this.loginButton = page.findTextExact('Kirjaudu')
  }

  userRadio(name: string): Element {
    return this.page.findTextExact(name)
  }
}
export class DummySuomiFiConfirmPage {
  proceedButton: Element

  constructor(page: Page) {
    this.proceedButton = page.findTextExact('Jatka')
  }
}
