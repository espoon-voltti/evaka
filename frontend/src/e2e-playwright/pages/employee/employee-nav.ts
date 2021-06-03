// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'

import { RawElement } from 'e2e-playwright/utils/element'
import { waitUntilEqual } from 'e2e-playwright/utils'

export default class EmployeeNav {
  constructor(private readonly page: Page) {}

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

  async openTab(tab: 'reports' | 'units') {
    switch (tab) {
      case 'units':
        await this.unitsTab.click()
        break
      case 'reports':
        await this.reportsTab.click()
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
    await waitUntilEqual(
      () => this.applicationsTab.visible,
      params.applications
    )
    await waitUntilEqual(
      () => this.applicationsTab.visible,
      params.applications
    )
    await waitUntilEqual(() => this.unitsTab.visible, params.units)
    await waitUntilEqual(() => this.searchTab.visible, params.search)
    await waitUntilEqual(() => this.financeTab.visible, params.finance)
    await waitUntilEqual(() => this.reportsTab.visible, params.reports)
    await waitUntilEqual(() => this.messagesTab.visible, params.messages)
  }
}
