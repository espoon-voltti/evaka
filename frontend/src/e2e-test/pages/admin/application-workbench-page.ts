// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationStatus } from 'lib-common/generated/api-types/application'

import { waitUntilEqual, waitUntilTrue } from '../../utils'
import type { Page } from '../../utils/page'
import { Checkbox, TextInput } from '../../utils/page'
import ApplicationListView from '../employee/applications/application-list-view'
import { PlacementDraftPage } from '../employee/placement-draft-page'

import ApplicationDetailsPage from './application-details-page'

export class SearchFilter {
  constructor(private page: Page) {}

  #statusRadioAll = this.page.findByDataQa('application-status-filter-ALL')

  async filterByTransferOnly() {
    await this.page.findByDataQa('filter-transfer-only').click()
  }

  async filterByTransferExclude() {
    await this.page.findByDataQa('filter-transfer-exclude').click()
  }

  async filterByApplicationStatus(status: ApplicationStatus) {
    await this.#statusRadioAll.click()
    await this.#statusRadioAll.click()
    await new Checkbox(
      this.page.findByDataQa(`application-status-filter-all-${status}`)
    ).click()
  }
}

export class DecisionEditorPage {
  constructor(private page: Page) {}

  async save() {
    await this.page.findByDataQa('save-decisions-button').click()
  }

  async cancel() {
    await this.page.findByDataQa('cancel-decisions-button').click()
  }
}

export class ApplicationWorkbenchPage {
  constructor(private page: Page) {}

  searchFilter = new SearchFilter(this.page)
  decisionEditorPage = new DecisionEditorPage(this.page)

  #applicationNoteContent = this.page.findByDataQa('application-note-content')

  #applicationPlacementProposalStatusIndicator = this.page.findByDataQa(
    'placement-proposal-status'
  )
  #applicationPlacementProposalStatusTooltip = this.page.findByDataQa(
    'placement-proposal-status-tooltip'
  )
  applicationsSent = this.page.findByDataQa('application-status-filter-SENT')
  #applicationsWaitingPlacement = this.page.findByDataQa(
    'application-status-filter-WAITING_PLACEMENT'
  )
  #applicationsWaitingDecision = this.page.findByDataQa(
    'application-status-filter-WAITING_DECISION'
  )
  #applicationsWaitingUnitConfirmation = this.page.findByDataQa(
    'application-status-filter-WAITING_UNIT_CONFIRMATION'
  )
  applicationsAll = this.page.findByDataQa('application-status-filter-ALL')
  // only matches when applications have been loaded
  #applicationList = this.page.findByDataQa('applications-list')
  #btnCloseApplication = this.page.findByDataQa('close-application')
  #agreementStatus = this.page.findByDataQa('agreement-status')
  #otherGuardianTel = this.page.findByDataQa('second-guardian-phone')
  #otherGuardianEmail = this.page.findByDataQa('second-guardian-email')

  #withdrawPlacementProposalsButton = this.page.findByDataQa(
    'action-bar-withdrawPlacementProposal'
  )

  async addNote(note: string) {
    await this.page.findByDataQa('add-note').click()
    await this.fillNote(note)
    await this.page.findByDataQa('save-note').click()
  }

  async deleteNote() {
    await this.page.findByDataQa('delete-note').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  getApplicationListItem(applicationId: string) {
    return this.#applicationList.find(
      `tr[data-application-id="${applicationId}"]`
    )
  }

  async openApplicationById(id: string) {
    const popup = await this.page.capturePopup(async () => {
      await this.getApplicationListItem(id).click()
    })
    return new ApplicationDetailsPage(popup)
  }

  async openDaycarePlacementDialogById(id: string) {
    await this.getApplicationById(id)
      .findByDataQa('primary-action-create-placement-plan')
      .click()
    return new PlacementDraftPage(this.page)
  }

  async verifyApplication(applicationId: string) {
    const list = new ApplicationListView(this.page)
    await list.actionsMenu(applicationId).click()
    await list.actionsMenuItems.setVerified.click()
  }

  async clickApplicationCheckbox(applicationId: string) {
    await new Checkbox(
      this.page.findByDataQa(`application-row-checkbox-${applicationId}`)
    ).click()
  }

  async moveToWaitingPlacement(applicationId: string) {
    await this.clickApplicationCheckbox(applicationId)
    await this.page.findByDataQa('action-bar-moveToWaitingPlacement').click()
  }

  async sendDecisionsWithoutProposal(applicationId: string) {
    await this.clickApplicationCheckbox(applicationId)
    await this.page
      .findByDataQa('action-bar-sendDecisionsWithoutProposal')
      .click()
  }

  async withdrawPlacementProposal(applicationId: string) {
    await this.clickApplicationCheckbox(applicationId)
    await this.#withdrawPlacementProposalsButton.click()
  }

  async assertWithdrawPlacementProposalsButtonDisabled() {
    await waitUntilTrue(() => this.#withdrawPlacementProposalsButton.disabled)
  }

  getApplicationById(id: string) {
    return this.page.find(
      `[data-qa="table-application-row"][data-application-id="${id}"]`
    )
  }

  async closeApplication() {
    if (await this.#btnCloseApplication.hasAttribute('disabled')) {
      // Application was opened in new window/tab
      throw new Error('close the window here somehow')
    } else {
      await this.#btnCloseApplication.click()
    }
  }

  async waitUntilLoaded() {
    await this.#applicationList.waitUntilVisible()
  }

  async openPlacementQueue() {
    await this.#applicationsWaitingPlacement.click()
    await this.waitUntilLoaded()
  }

  async openDecisionQueue() {
    await this.#applicationsWaitingDecision.click()
    await this.waitUntilLoaded()
  }

  async openPlacementProposalQueue() {
    await this.#applicationsWaitingUnitConfirmation.click()
    await this.waitUntilLoaded()
  }

  async assertApplicationStatusTextMatches(
    index: number,
    matchingText: string
  ) {
    await this.#applicationPlacementProposalStatusIndicator.hover()
    const tooltip = await this.#applicationPlacementProposalStatusTooltip
      .innerText
    const match = tooltip.match(matchingText)
    return match ? match.length > 0 : false
  }

  async verifyNoteExists(note: string) {
    await waitUntilTrue(async () =>
      (await this.#applicationNoteContent.innerText).includes(note)
    )
  }

  async fillNote(note: string) {
    const noteTextArea = new TextInput(
      this.page.findByDataQa('note-container').find('textarea')
    )
    await noteTextArea.fill(note)
  }

  async openDecisionEditorById(applicationId: string) {
    await this.getApplicationById(applicationId)
      .findByDataQa('primary-action-edit-decisions')
      .click()
  }

  async assertAgreementStatusNotAgreed() {
    await waitUntilTrue(async () =>
      (await this.#agreementStatus.innerText).includes('Ei ole sovittu yhdessä')
    )
  }

  async assertContactDetails(expectedTel: string, expectedEmail: string) {
    await waitUntilEqual(() => this.#otherGuardianTel.innerText, expectedTel)
    await waitUntilEqual(
      () => this.#otherGuardianEmail.innerText,
      expectedEmail
    )
  }

  async assertDecisionGuardians(
    expectedGuardian: string,
    expectedOtherGuardian?: string
  ) {
    await waitUntilEqual(
      () => this.page.findByDataQa('guardian-name').innerText,
      expectedGuardian
    )
    if (expectedOtherGuardian !== undefined) {
      await waitUntilEqual(
        () => this.page.findByDataQa('other-guardian-name').innerText,
        expectedOtherGuardian
      )
    }
  }
}
