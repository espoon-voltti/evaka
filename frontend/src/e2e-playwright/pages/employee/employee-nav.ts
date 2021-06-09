// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

import config from 'e2e-test-common/config'
import { UserRole } from 'e2e-test-common/dev-api/types'

import { RawElement } from 'e2e-playwright/utils/element'
import DevLoginForm from 'e2e-playwright/pages/dev-login-form'

export default class EmployeeNav {
  constructor(private readonly page: Page) {}

  readonly #loginBtn = new RawElement(this.page, '[data-qa="login-btn"]')
  readonly #userNameBtn = new RawElement(this.page, '[data-qa="username"]')

  readonly applicationsTab = new RawElement(
    this.page,
    '[data-qa="applications-nav"]'
  )
  readonly unitsTab = new RawElement(this.page, '[data-qa="units-nav"]')
  readonly searchTab = new RawElement(this.page, '[data-qa="search-nav"]')
  readonly financeTab = new RawElement(this.page, '[data-qa="finance-nav"]')
  readonly reportsTab = new RawElement(this.page, '[data-qa="reports-nav"]')
  readonly messagesTab = new RawElement(this.page, '[data-qa="messages-nav"]')

  readonly #employeesLink = new RawElement(
    this.page,
    '[data-qa="user-popup-employees"]'
  )
  readonly #pinCodeLink = new RawElement(
    this.page,
    '[data-qa="user-popup-pin-code"]'
  )

  async login(role: UserRole) {
    await this.#loginBtn.click()

    const form = new DevLoginForm(this.page)
    switch (role) {
      case 'MANAGER':
        await form.login({
          aad: config.supervisorAad,
          roles: []
        })
        break
      case 'ADMIN':
        await form.login({
          aad: config.adminAad,
          roles: ['SERVICE_WORKER', 'FINANCE_ADMIN', 'ADMIN']
        })
        break
      case 'SERVICE_WORKER':
        await form.loginAsUser(config.serviceWorkerAad)
        break
      case 'FINANCE_ADMIN':
        await form.loginAsUser(config.financeAdminAad)
        break
      case 'DIRECTOR':
        await form.loginAsUser(config.directorAad)
        break
      case 'STAFF':
        await form.loginAsUser(config.staffAad)
        break
      case 'UNIT_SUPERVISOR':
        await form.loginAsUser(config.unitSupervisorAad)
        break
      case 'SPECIAL_EDUCATION_TEACHER':
        await form.loginAsUser(config.specialEducationTeacher)
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

  async openTab(tab: 'reports' | 'units' | 'finance') {
    switch (tab) {
      case 'units':
        await this.unitsTab.click()
        break
      case 'reports':
        await this.reportsTab.click()
        break
      case 'finance':
        await this.financeTab.click()
        break
    }
  }

  async tabsVisible(params: {
    applications: boolean
    units: boolean
    search: boolean
    finance: boolean
    reports: boolean
    messages: boolean
  }) {
    expect(await this.applicationsTab.visible).toBe(params.applications)
    expect(await this.unitsTab.visible).toBe(params.units)
    expect(await this.searchTab.visible).toBe(params.search)
    expect(await this.financeTab.visible).toBe(params.finance)
    expect(await this.reportsTab.visible).toBe(params.reports)
    expect(await this.messagesTab.visible).toBe(params.messages)
  }
}
