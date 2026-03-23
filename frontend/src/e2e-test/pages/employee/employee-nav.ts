// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { expect } from '@playwright/test'

import type { Page, Element } from '../../utils/page'

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

  async assertTabNotificationsCount(
    tab: 'reports' | 'messages',
    count: number
  ) {
    let tabElem: Element
    switch (tab) {
      case 'reports':
        tabElem = this.reportsTab
        break
      case 'messages':
        tabElem = this.messagesTab
        break
    }
    if (count > 0) {
      await tabElem
        .findByDataQa('notifications')
        .assertTextEquals(count.toString())
    } else {
      await tabElem.findByDataQa('no-notifications').waitUntilAttached()
    }
  }

  private async assertVisibility(element: Element, visible: boolean) {
    if (visible) {
      await expect(element.locator).toBeVisible()
    } else {
      await expect(element.locator).toBeHidden()
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
    await this.assertVisibility(this.applicationsTab, params.applications)
    await this.assertVisibility(this.unitsTab, params.units)
    await this.assertVisibility(this.searchTab, params.search)
    await this.assertVisibility(this.financeTab, params.finance)
    await this.assertVisibility(this.reportsTab, params.reports)
    await this.assertVisibility(this.messagesTab, params.messages)
  }
}
