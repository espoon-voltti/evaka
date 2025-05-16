// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  ApplicationStatusOption,
  ApplicationTypeToggle
} from 'lib-common/generated/api-types/application'
import type { DecisionType } from 'lib-common/generated/api-types/decision'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import config from '../../../config'
import { waitUntilEqual } from '../../../utils'
import type { Page, ElementCollection } from '../../../utils/page'
import {
  MultiSelect,
  Element,
  Radio,
  Checkbox,
  Combobox
} from '../../../utils/page'
import { PlacementDraftPage } from '../placement-draft-page'

import ApplicationReadView from './application-read-view'

export default class ApplicationListView {
  applicationStatus: Element
  allApplications: Element
  #areaFilter: MultiSelect
  #unitFilter: MultiSelect
  searchButton: Element
  #applications: ElementCollection
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
  actionBar: {
    sendDecisionsWithoutProposal: Element
    withdrawPlacementProposal: Element
  }
  constructor(private page: Page) {
    this.applicationStatus = page.findByDataQa('application-status')
    this.allApplications = page.findByDataQa('application-status-filter-ALL')
    this.#areaFilter = new MultiSelect(page.findByDataQa('area-filter'))
    this.#unitFilter = new MultiSelect(page.findByDataQa('unit-selector'))
    this.searchButton = page.findByDataQa('search-button')
    this.#applications = page
      .find('[data-qa="table-of-applications"]')
      .findAll('[data-qa="table-application-row"]')
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
    const actionBarRoot = page.findByDataQa('action-bar')
    this.actionBar = {
      sendDecisionsWithoutProposal: actionBarRoot.findByDataQa(
        'action-bar-sendDecisionsWithoutProposal'
      ),
      withdrawPlacementProposal: actionBarRoot.findByDataQa(
        'action-bar-withdrawPlacementProposal'
      )
    }
  }

  static url = `${config.employeeUrl}/applications`

  async toggleArea(areaName: string) {
    await this.#areaFilter.fillAndSelectFirst(areaName)
  }

  toggleUnit = async (unitName: string) => {
    await this.#unitFilter.fillAndSelectFirst(unitName)
  }

  async filterByApplicationType(type: ApplicationTypeToggle) {
    await this.page.findByDataQa(`application-type-filter-${type}`).click()
  }

  async filterByApplicationStatus(type: ApplicationStatusOption | 'ALL') {
    await this.page.findByDataQa(`application-status-filter-${type}`).click()
  }

  #application = (id: string) => this.page.find(`[data-application-id="${id}"]`)

  applicationRow(id: string) {
    const element = this.#application(id)
    return new ApplicationRow(this.page, element)
  }

  async assertApplicationIsVisible(applicationId: string) {
    await this.#application(applicationId).waitUntilVisible()
  }

  async assertApplicationCount(n: number) {
    await waitUntilEqual(() => this.#applications.count(), n)
  }
}

export class ApplicationRow extends Element {
  status: Element
  actionsMenuButton: Element
  actionsMenuItems: {
    sendDecisionsWithoutProposal: Element
    cancelApplication: Element
  }
  checkbox: Checkbox
  constructor(
    private page: Page,
    private root: Element
  ) {
    super(root)
    this.status = this.find('[data-qa="application-status"]')
    this.actionsMenuButton = root.findByDataQa('application-actions-menu')
    this.actionsMenuItems = {
      sendDecisionsWithoutProposal: root.findByDataQa(
        'menu-item-send-decisions-without-proposal'
      ),
      cancelApplication: root.findByDataQa('menu-item-cancel-application')
    }
    this.checkbox = new Checkbox(root.findByDataQa('application-row-checkbox'))
  }

  async openApplication() {
    const applicationDetails = new Promise<ApplicationReadView>((res) => {
      this.page.onPopup((page) => res(new ApplicationReadView(page)))
    })
    await this.click()
    return applicationDetails
  }

  async primaryActionCheck() {
    await this.root.findByDataQa('primary-action-check').click()
    return new ApplicationReadView(this.page)
  }

  async primaryActionCreatePlacementPlan() {
    await this.root.findByDataQa('primary-action-create-placement-plan').click()
    return new PlacementDraftPage(this.page)
  }

  async primaryActionEditDecisions() {
    await this.root.findByDataQa('primary-action-edit-decisions').click()
    return new DecisionEditorPage(this.page)
  }

  async assertStartDate(date: LocalDate) {
    await this.root.findByDataQa('start-date').assertTextEquals(date.format())
  }

  async assertServiceWorkerNoteMatches(matchingText: string) {
    const note = this.root.findByDataQa('service-worker-note')
    await note.hover()
    const tooltip = await note.text
    const match = tooltip.match(matchingText)
    return match ? match.length > 0 : false
  }
}

export class DecisionEditorPage {
  constructor(private page: Page) {}

  async waitUntilLoaded() {
    await this.page.findByDataQa('save-decisions-button').waitUntilVisible()
  }

  async selectUnit(type: DecisionType, unitId: UUID) {
    const selector = new Combobox(
      this.page.findByDataQa(`unit-selector-${type}`)
    )
    await selector.fillAndSelectItem('', unitId)
  }

  async save() {
    await this.page.findByDataQa('save-decisions-button').click()
  }

  async cancel() {
    await this.page.findByDataQa('cancel-decisions-button').click()
  }
}
