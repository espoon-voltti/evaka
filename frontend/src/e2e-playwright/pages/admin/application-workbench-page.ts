// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationStatus } from 'lib-common/generated/enums'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { Checkbox, Page, TextInput } from '../../utils/page'
// import ApplicationReadView from '../employee/applications/application-read-view'
import ApplicationListView from '../employee/applications/application-list-view'
import { PlacementDraftPage } from '../employee/placement-draft-page'
import ApplicationDetailsPage from './application-details-page'

export class SearchFilter {
  constructor(private page: Page) {}

  #statusRadioAll = this.page.find('[data-qa="application-status-filter-ALL"]')

  async filterByTransferOnly() {
    await this.page.find('[data-qa="filter-transfer-only"]').click()
  }

  async filterByTransferExclude() {
    await this.page.find('[data-qa="filter-transfer-exclude"]').click()
  }

  async filterByApplicationStatus(status: ApplicationStatus) {
    await this.#statusRadioAll.click()
    await this.#statusRadioAll.click()
    await new Checkbox(
      this.page.find(`[data-qa="application-status-filter-all-${status}"]`)
    ).click()
  }
}

export class PlacementPage {
  constructor(private page: Page) {}

  #list = this.page
    .find('[data-qa="placement-list"]')
    .findAll('[data-qa="placement-item"]')

  async placeIn(placementNumber: number) {
    await this.#list
      .nth(placementNumber)
      .find('[data-qa="select-placement-unit"]')
      .click()
  }

  async sendPlacement() {
    await this.page.find('[data-qa="send-placement-button"').click()
  }
}

export class DecisionEditorPage {
  constructor(private page: Page) {}

  async save() {
    await this.page.find('[data-qa="save-decisions-button"').click()
  }

  async cancel() {
    await this.page.find('[data-qa="cancel-decisions-button"').click()
  }
}

export class ApplicationWorkbenchPage {
  constructor(private page: Page) {}

  searchFilter = new SearchFilter(this.page)
  placementPage = new PlacementPage(this.page)
  decisionEditorPage = new DecisionEditorPage(this.page)

  #applicationNoteContent = this.page.find(
    '[data-qa="application-note-content"]'
  )
  applicationsSent = this.page.find(
    '[data-qa="application-status-filter-SENT"]'
  )
  #applicationsWaitingPlacement = this.page.find(
    '[data-qa="application-status-filter-WAITING_PLACEMENT"]'
  )
  #applicationsWaitingDecision = this.page.find(
    '[data-qa="application-status-filter-WAITING_DECISION"]'
  )
  #applicationsWaitingUnitConfirmation = this.page.find(
    '[data-qa="application-status-filter-WAITING_UNIT_CONFIRMATION"]'
  )
  applicationsAll = this.page.find('[data-qa="application-status-filter-ALL"]')
  // only matches when applications have been loaded
  #applicationList = this.page.find('[data-qa="applications-list"]')
  #btnCloseApplication = this.page.find('[data-qa="close-application"]')
  #agreementStatus = this.page.find('[data-qa="agreement-status"]')
  #otherGuardianTel = this.page.find('[data-qa="second-guardian-phone"]')
  #otherGuardianEmail = this.page.find('[data-qa="second-guardian-email"]')

  #withdraPlacementProposalsButton = this.page.find(
    `[data-qa="action-bar-withdrawPlacementProposal"]`
  )

  async addNote(note: string) {
    await this.page.find('[data-qa="add-note"]').click()
    await this.fillNote(note)
    await this.page.find('[data-qa="save-note"]').click()
  }

  async deleteNote() {
    await this.page.find('[data-qa="delete-note"]').click()
    await this.page.find('[data-qa="modal-okBtn"]').click()
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
      .find('[data-qa="primary-action-create-placement-plan"]')
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
      this.page.find(`[data-qa="application-row-checkbox-${applicationId}"]`)
    ).click()
  }

  async moveToWaitingPlacement(applicationId: string) {
    await this.clickApplicationCheckbox(applicationId)
    await this.page
      .find('[data-qa="action-bar-moveToWaitingPlacement"]')
      .click()
  }

  async sendDecisionsWithoutProposal(applicationId: string) {
    await this.clickApplicationCheckbox(applicationId)
    await this.page
      .find('[data-qa="action-bar-sendDecisionsWithoutProposal"]')
      .click()
  }

  async withdrawPlacementProposal(applicationId: string) {
    await this.clickApplicationCheckbox(applicationId)
    await this.#withdraPlacementProposalsButton.click()
  }

  async assertWithdrawPlacementProposalsButtonDisabled() {
    await waitUntilTrue(() => this.#withdraPlacementProposalsButton.disabled)
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

  async verifyNoteExists(note: string) {
    await waitUntilTrue(async () =>
      (await this.#applicationNoteContent.innerText).includes(note)
    )
  }

  async waitUntilApplicationsLoaded() {
    await this.page.find('[data-qa="applications-list"]').waitUntilVisible()
  }

  async fillNote(note: string) {
    const noteTextArea = new TextInput(
      this.page.find('[data-qa="note-container"]').find('textarea')
    )
    await noteTextArea.fill(note)
  }

  async openDecisionEditorById(applicationId: string) {
    await this.getApplicationById(applicationId)
      .find('[data-qa="primary-action-edit-decisions"]')
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
      () => this.page.find('[data-qa="guardian-name"]').innerText,
      expectedGuardian
    )
    if (expectedOtherGuardian !== undefined) {
      await waitUntilEqual(
        () => this.page.find('[data-qa="other-guardian-name"]').innerText,
        expectedOtherGuardian
      )
    }
  }
}
