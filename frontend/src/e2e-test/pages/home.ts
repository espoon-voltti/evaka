// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// Home model to handle page url, login and logout actions
import { Selector, t } from 'testcafe'
import config from 'e2e-test-common/config'
import { Idp } from '../config/idp'
import DevLoginForm from './dev-login-form'

type EvakaRole = 'manager' | 'admin' | 'enduser'

export default class Home {
  private readonly devLoginForm = new DevLoginForm()
  constructor(
    readonly loginBtn = Selector('[data-qa="login-btn"]'),
    readonly userNameBtn = Selector('[data-qa="username"]'),
    readonly logoutBtn = Selector('[data-qa="logout-btn"]'),
    readonly logoWrapper = Selector('.logo-wrapper')
  ) {}

  public homePage(role: EvakaRole) {
    switch (role) {
      case 'manager':
      case 'admin':
        return config.adminUrl
      case 'enduser':
        return config.enduserUrl
    }
  }

  async idpLogin(user: string, pass: string) {
    await t.click(this.loginBtn)
    await new Idp().login(user, pass)
  }

  isLoggedIn() {
    return this.userNameBtn.exists
  }

  async login(role: EvakaRole) {
    await t.click(this.loginBtn)
    switch (role) {
      case 'manager':
        await this.devLoginForm.login({
          aad: config.supervisorAad,
          roles: []
        })
        break
      case 'admin':
        await this.devLoginForm.login({
          aad: config.adminAad,
          roles: ['SERVICE_WORKER', 'FINANCE_ADMIN', 'ADMIN']
        })
        break
      case 'enduser':
        break
    }
    if (role === 'enduser') {
      await t.expect(this.logoutBtn.visible).ok()
    } else {
      await t.expect(this.userNameBtn.visible).ok()
    }
  }

  async logout() {
    await t.click(this.userNameBtn)
    await t.click(this.logoutBtn)
  }

  async navigateToUserHomePage(role: EvakaRole) {
    switch (role) {
      case 'manager':
        await t.navigateTo(this.homePage('manager'))
        break
      case 'admin':
        await t.navigateTo(this.homePage('admin'))
        break
      case 'enduser':
        await t.navigateTo(this.homePage('enduser'))
        break
    }
    await t.expect(this.loginBtn.visible).ok()
  }
}
