// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationTypeToggle } from 'lib-common/generated/api-types/application'

import config from '../../../config'
import { waitUntilEqual } from '../../../utils'
import {
  MultiSelect,
  Page,
  Element,
  ElementCollection,
  Radio
} from '../../../utils/page'

export default class ApplicationListView {
  applicationStatus: Element
  allApplications: Element
  #areaFilter: MultiSelect
  #unitFilter: MultiSelect
  #applications: ElementCollection
  actionsMenuItems: {
    createPlacement: Element
    createDecision: Element
    acceptPlacementWithoutDecision: Element
    cancelApplication: Element
  }
  cancelConfirmation: {
    confidentialRadioYes: Radio
    confidentialRadioNo: Radio
    submitButton: Element
  }
  specialFilterItems: {
    duplicate: Element
  }
  voucherUnitFilter: {
    firstChoice: Element
    voucherOnly: Element
    voucherHide: Element
    noFilter: Element
  }
  constructor(private page: Page) {
    this.applicationStatus = page.findByDataQa('application-status')
    this.allApplications = page.findByDataQa('application-status-filter-ALL')
    this.#areaFilter = new MultiSelect(page.findByDataQa('area-filter'))
    this.#unitFilter = new MultiSelect(page.findByDataQa('unit-selector'))
    this.#applications = page
      .find('[data-qa="table-of-applications"]')
      .findAll('[data-qa="table-application-row"]')
    this.actionsMenuItems = {
      createPlacement: this.#actionsMenuItemSelector('placement-draft'),
      createDecision: this.#actionsMenuItemSelector('decision'),
      acceptPlacementWithoutDecision: this.#actionsMenuItemSelector(
        'placement-without-decision'
      ),
      cancelApplication: this.#actionsMenuItemSelector('cancel-application')
    }
    this.cancelConfirmation = {
      confidentialRadioYes: new Radio(page.findByDataQa('confidential-yes')),
      confidentialRadioNo: new Radio(page.findByDataQa('confidential-no')),
      submitButton: page.findByDataQa('modal-okBtn')
    }
    this.specialFilterItems = {
      duplicate: page.findByDataQa('application-basis-DUPLICATE_APPLICATION')
    }
    this.voucherUnitFilter = {
      firstChoice: page.findByDataQa('filter-voucher-first-choice'),
      voucherOnly: page.findByDataQa('filter-voucher-all'),
      voucherHide: page.findByDataQa('filter-voucher-hide'),
      noFilter: page.findByDataQa('filter-voucher-no-filter')
    }
  }

  static url = `${config.employeeUrl}/applications`

  actionsMenu = (applicationId: string) =>
    this.page
      .find(`[data-application-id="${applicationId}"]`)
      .find('[data-qa="application-actions-menu"]')

  #actionsMenuItemSelector = (id: string) =>
    this.page.findByDataQa(`menu-item-${id}`)

  async toggleArea(areaName: string) {
    await this.#areaFilter.fillAndSelectFirst(areaName)
  }

  toggleUnit = async (unitName: string) => {
    await this.#unitFilter.fillAndSelectFirst(unitName)
  }

  #application = (id: string) => this.page.find(`[data-application-id="${id}"]`)

  async assertApplicationIsVisible(applicationId: string) {
    await this.#application(applicationId).waitUntilVisible()
  }

  async assertApplicationCount(n: number) {
    await waitUntilEqual(() => this.#applications.count(), n)
  }

  async filterByApplicationType(type: ApplicationTypeToggle) {
    await this.page.findByDataQa(`application-type-filter-${type}`).click()
  }
}
