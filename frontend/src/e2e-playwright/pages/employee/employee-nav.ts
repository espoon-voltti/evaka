// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RawElement } from 'e2e-playwright/utils/element'
import config from 'e2e-test-common/config'
import DevLoginForm from 'e2e-playwright/pages/dev-login-form'
import { Page } from 'playwright'

export default class EmployeeNav {
  constructor(private readonly page: Page) {}

  readonly #loginBtn = new RawElement(this.page, '[data-qa="login-btn"]')
  readonly #userNameBtn = new RawElement(this.page, '[data-qa="username"]')

  readonly #reportsTab = new RawElement(this.page, '[data-qa="reports-nav"]')

  readonly #employeesLink = new RawElement(
    this.page,
    '[data-qa="user-popup-employees"]'
  )
  readonly #pinCodeLink = new RawElement(
    this.page,
    '[data-qa="user-popup-pin-code"]'
  )

  async login(role: 'manager' | 'admin') {
    await this.#loginBtn.click()

    const form = new DevLoginForm(this.page)

    switch (role) {
      case 'manager':
        await form.login({
          aad: config.supervisorAad,
          roles: []
        })
        break
      case 'admin':
        await form.login({
          aad: config.adminAad,
          roles: ['SERVICE_WORKER', 'FINANCE_ADMIN', 'ADMIN']
        })
        break
    }
    await this.#userNameBtn.waitUntilVisible()
  }

  async openDropdownMenu() {
    await this.#userNameBtn.click()
  }

  async openAndClickDropdownMenuItem(item: 'employees' | 'pinCode') {
    await this.openDropdownMenu()
    switch (item) {
      case 'employees':
        return await this.#employeesLink.click()
      case 'pinCode':
        return await this.#pinCodeLink.click()
    }
  }

  async openTab(tab: 'reports') {
    switch (tab) {
      case 'reports':
        await this.#reportsTab.click()
        break
    }
  }
}
