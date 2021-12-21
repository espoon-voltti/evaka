// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Combobox, Page } from '../../../utils/page'
import config from 'e2e-test-common/config'

export default class ApplicationListView {
  constructor(private page: Page) {}

  url = `${config.employeeUrl}/applications`
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

  #areaFilter = new Combobox(this.page.find('[data-qa="area-filter"]'))

  async toggleArea(areaName: string) {
    await this.#areaFilter.fillAndSelectFirst(areaName)
  }

  specialFilterItems = {
    duplicate: this.page.find(
      '[data-for="application-basis-DUPLICATE_APPLICATION"]'
    )
  }

  #unitFilter = new Combobox(this.page.find('[data-qa="unit-selector"]'))

  toggleUnit = async (unitName: string) => {
    await this.#unitFilter.fillAndSelectFirst(unitName)
  }

  application = (id: string) => this.page.find(`[data-application-id="${id}"]`)

  applications = this.page
    .find('[data-qa="table-of-applications"]')
    .find('[data-qa="table-application-row"]')

  voucherUnitFilter = {
    firstChoice: this.page.find('[data-qa="filter-voucher-first-choice"]'),
    voucherOnly: this.page.find('[data-qa="filter-voucher-all"]'),
    voucherHide: this.page.find('[data-qa="filter-voucher-hide"]'),
    noFilter: this.page.find('[data-qa="filter-voucher-no-filter"]')
  }
}
