// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page, Element, TextInput, ElementCollection } from '../../utils/page'

export class KeycloakLoginPage {
  email: TextInput
  password: TextInput
  loginButton: Element
  forgotPasswordLink: Element
  registerLink: Element

  constructor(page: Page) {
    this.email = new TextInput(page.findTextExact('Sähköpostiosoite'))
    this.password = new TextInput(page.findTextExact('Salasana'))
    this.loginButton = page.findTextExact('Kirjaudu sisään')
    this.forgotPasswordLink = page.findTextExact('Unohditko salasanasi?')
    this.registerLink = page.findTextExact('Luo tunnus')
  }
}

export class ForgotPasswordPage {
  email: TextInput
  changePasswordButton: Element
  backToLoginLink: Element

  constructor(page: Page) {
    this.email = new TextInput(page.findTextExact('Sähköpostiosoite'))
    this.changePasswordButton = page.findTextExact('Vahvista')
    this.backToLoginLink = page.findTextExact('Palaa sisäänkirjautumiseen')
  }
}

export class UpdatePasswordPage {
  newPassword: TextInput
  confirmPassword: TextInput
  changePasswordButton: Element

  constructor(page: Page) {
    this.newPassword = new TextInput(page.findTextExact('Salasana'))
    this.confirmPassword = new TextInput(
      page.findTextExact('Vahvista salasana')
    )
    this.changePasswordButton = page.findTextExact('Vahvista')
  }
}

export class ConfirmPage {
  email: TextInput
  sendButton: Element
  allLabels: ElementCollection

  constructor(page: Page) {
    this.email = new TextInput(page.findTextExact('Sähköpostiosoite'))
    this.sendButton = page.findTextExact('Lähetä')
    this.allLabels = page.findAll('label')
  }
}
