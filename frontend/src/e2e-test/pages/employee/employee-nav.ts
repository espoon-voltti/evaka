// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from '../../utils'
import { Page, Element } from '../../utils/page'

export default class EmployeeNav {
  #userNameBtn: Element
  applicationsTab: Element
  unitsTab: Element
  searchTab: Element
  financeTab: Element
  reportsTab: Element
  messagesTab: Element
  constructor(private readonly page: Page) {
    this.#userNameBtn = page.findByDataQa('username')
    this.applicationsTab = page.findByDataQa('applications-nav')
    this.unitsTab = page.findByDataQa('units-nav')
    this.searchTab = page.findByDataQa('search-nav')
    this.financeTab = page.findByDataQa('finance-nav')
    this.reportsTab = page.findByDataQa('reports-nav')
    this.messagesTab = page.findByDataQa('messages-nav')
  }

  async openAndClickDropdownMenuItem(
    item:
      | 'employees'
      | 'pin-code'
      | 'finance-basics'
      | 'document-templates'
      | 'holiday-periods'
      | 'preferred-first-name'
      | 'system-notifications'
  ) {
    await this.#userNameBtn.click()
    return this.page.findByDataQa(`user-popup-${item}`).click()
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
