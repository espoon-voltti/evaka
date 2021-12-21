// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { MultiSelect, Page } from '../../../utils/page'
import config from 'e2e-test-common/config'
import { waitUntilEqual } from '../../../utils'

export default class ApplicationListView {
  constructor(private page: Page) {}

  static url = `${config.employeeUrl}/applications`
  applicationStatus = this.page.find('[data-qa="application-status"]')

  actionsMenu = (applicationId: string) =>
    this.page
      .find(`[data-application-id="${applicationId}"]`)
      .find('[data-qa="application-actions-menu"]')

  allApplications = this.page.find('[data-qa="application-status-filter-ALL"]')

  #actionsMenuItemSelector = (id: string) =>
    this.page.find(`[data-qa="action-item-${id}"]`)

  actionsMenuItems = {
    verify: this.#actionsMenuItemSelector('verify'),
    setVerified: this.#actionsMenuItemSelector('set-verified'),
    createPlacement: this.#actionsMenuItemSelector('placement-draft'),
    createDecision: this.#actionsMenuItemSelector('decision'),
    acceptPlacementWihtoutDecision: this.#actionsMenuItemSelector(
      'placement-without-decision'
    )
  }

  #areaFilter = new MultiSelect(this.page.find('[data-qa="area-filter"]'))

  async toggleArea(areaName: string) {
    await this.#areaFilter.fillAndSelectFirst(areaName)
  }

  specialFilterItems = {
    duplicate: this.page.find(
      '[data-for="application-basis-DUPLICATE_APPLICATION"]'
    )
  }

  #unitFilter = new MultiSelect(this.page.find('[data-qa="unit-selector"]'))

  toggleUnit = async (unitName: string) => {
    await this.#unitFilter.fillAndSelectFirst(unitName)
  }

  #application = (id: string) => this.page.find(`[data-application-id="${id}"]`)

  async assertApplicationIsVisible(applicationId: string) {
    await this.#application(applicationId).waitUntilVisible()
  }

  #applications = this.page
    .find('[data-qa="table-of-applications"]')
    .findAll('[data-qa="table-application-row"]')

  async assertApplicationCount(n: number) {
    await waitUntilEqual(() => this.#applications.count(), n)
  }

  voucherUnitFilter = {
    firstChoice: this.page.find('[data-qa="filter-voucher-first-choice"]'),
    voucherOnly: this.page.find('[data-qa="filter-voucher-all"]'),
    voucherHide: this.page.find('[data-qa="filter-voucher-hide"]'),
    noFilter: this.page.find('[data-qa="filter-voucher-no-filter"]')
  }
}
