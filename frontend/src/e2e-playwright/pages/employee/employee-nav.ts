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
  readonly #logoutBtn = new RawElement(this.page, '[data-qa="logout-btn"]')

  readonly #reportsTab = new RawElement(this.page, '[data-qa="reports-nav"]')

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
    await this.#logoutBtn.waitUntilVisible()
  }

  async openTab(tab: 'reports') {
    switch (tab) {
      case 'reports':
        await this.#reportsTab.click()
        break
    }
  }
}
