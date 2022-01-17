// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'

export default class EmployeeNav {
  constructor(private readonly page: Page) {}

  readonly #userNameBtn = this.page.find('[data-qa="username"]')

  readonly applicationsTab = this.page.find('[data-qa="applications-nav"]')
  readonly unitsTab = this.page.find('[data-qa="units-nav"]')
  readonly searchTab = this.page.find('[data-qa="search-nav"]')
  readonly financeTab = this.page.find('[data-qa="finance-nav"]')
  readonly reportsTab = this.page.find('[data-qa="reports-nav"]')
  readonly messagesTab = this.page.find('[data-qa="messages-nav"]')

  readonly #employeesLink = this.page.find('[data-qa="user-popup-employees"]')
  readonly #pinCodeLink = this.page.find('[data-qa="user-popup-pin-code"]')
  readonly #financeBasicsLink = this.page.find(
    '[data-qa="user-popup-finance-basics"]'
  )
  readonly #vasuTemplatesLink = this.page.find(
    '[data-qa="user-popup-vasu-templates"]'
  )

  async openDropdownMenu() {
    await this.#userNameBtn.click()
  }

  async openAndClickDropdownMenuItem(
    item: 'employees' | 'pinCode' | 'financeBasics' | 'vasu-templates'
  ) {
    await this.openDropdownMenu()
    switch (item) {
      case 'employees':
        return await this.#employeesLink.click()
      case 'pinCode':
        return await this.#pinCodeLink.click()
      case 'financeBasics':
        return await this.#financeBasicsLink.click()
      case 'vasu-templates':
        return await this.#vasuTemplatesLink.click()
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
