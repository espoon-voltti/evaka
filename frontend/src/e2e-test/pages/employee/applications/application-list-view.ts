// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationTypeToggle } from 'lib-common/generated/api-types/application'

import config from '../../../config'
import { waitUntilEqual } from '../../../utils'
import { MultiSelect, Page } from '../../../utils/page'

export default class ApplicationListView {
  constructor(private page: Page) {}

  static url = `${config.employeeUrl}/applications`
  applicationStatus = this.page.findByDataQa('application-status')

  actionsMenu = (applicationId: string) =>
    this.page
      .find(`[data-application-id="${applicationId}"]`)
      .find('[data-qa="application-actions-menu"]')

  allApplications = this.page.findByDataQa('application-status-filter-ALL')

  #actionsMenuItemSelector = (id: string) =>
    this.page.findByDataQa(`action-item-${id}`)

  actionsMenuItems = {
    verify: this.#actionsMenuItemSelector('verify'),
    setVerified: this.#actionsMenuItemSelector('set-verified'),
    createPlacement: this.#actionsMenuItemSelector('placement-draft'),
    createDecision: this.#actionsMenuItemSelector('decision'),
    acceptPlacementWihtoutDecision: this.#actionsMenuItemSelector(
      'placement-without-decision'
    )
  }

  #areaFilter = new MultiSelect(this.page.findByDataQa('area-filter'))

  async toggleArea(areaName: string) {
    await this.#areaFilter.fillAndSelectFirst(areaName)
  }

  specialFilterItems = {
    duplicate: this.page.findByDataQa('application-basis-DUPLICATE_APPLICATION')
  }

  #unitFilter = new MultiSelect(this.page.findByDataQa('unit-selector'))

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
    firstChoice: this.page.findByDataQa('filter-voucher-first-choice'),
    voucherOnly: this.page.findByDataQa('filter-voucher-all'),
    voucherHide: this.page.findByDataQa('filter-voucher-hide'),
    noFilter: this.page.findByDataQa('filter-voucher-no-filter')
  }

  async filterByApplicationType(type: ApplicationTypeToggle) {
    await this.page.findByDataQa(`application-type-filter-${type}`).click()
  }
}
