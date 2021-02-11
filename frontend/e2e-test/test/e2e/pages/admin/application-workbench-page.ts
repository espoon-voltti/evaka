// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import SearchFilter from './search-filters'
import PlacementPage from '../employee/placement-page'
import DecisionEditorPage from '../employee/decision-editor-page'
import ApplicationReadView from '../employee/applications/application-read-view'
import ApplicationListView from '../employee/applications/application-list-view'
import { Checkbox, scrollThenClick } from '../../utils/helpers'

export class ApplicationWorkbenchPage {
  readonly searchFilter: SearchFilter = SearchFilter.newSearch()
  readonly placementPage: PlacementPage = new PlacementPage()
  readonly decisionEditorPage = new DecisionEditorPage()

  readonly applicationNoteContent = Selector(
    '[data-qa="application-note-content"]'
  )
  readonly applicationsSent: Selector = Selector(
    '[data-qa="application-status-filter-SENT"]'
  )
  readonly applicationsWaitingPlacement: Selector = Selector(
    '[data-qa="application-status-filter-WAITING_PLACEMENT"]'
  )
  readonly applicationsWaitingDecision: Selector = Selector(
    '[data-qa="application-status-filter-WAITING_DECISION"]'
  )
  readonly applicationsWaitingUnitConfirmation: Selector = Selector(
    '[data-qa="application-status-filter-WAITING_UNIT_CONFIRMATION"]'
  )
  readonly applicationsAll: Selector = Selector(
    '[data-qa="application-status-filter-ALL"]'
  )
  // only matches when applications have been loaded
  readonly applicationList: Selector = Selector('[data-qa="applications-list"]')
  readonly application: Selector = Selector('[data-qa="table-application-row"]')
  readonly btnCloseApplication: Selector = Selector(
    '[data-qa="close-application"]'
  )
  readonly agreementStatus = Selector('[data-qa="agreement-status"]')
  readonly otherGuardianTel = Selector('[data-qa="second-guardian-phone"]')
  readonly otherGuardianEmail = Selector('[data-qa="second-guardian-email"]')

  async addNote(note: string) {
    await t.click(Selector('[data-qa="add-note"]'))
    await this.fillNote(note)
    await t.click(Selector('[data-qa="save-note"]'))
  }

  async deleteNote() {
    await t.click(Selector('[data-qa="delete-note"]'))
    await t.click(Selector('[data-qa="modal-okBtn"]'))
  }

  getApplicationListItem(applicationId: string) {
    return this.applicationList.find(
      `tr[data-application-id="${applicationId}"]`
    )
  }

  async openApplicationById(id: string) {
    await t.click(this.getApplicationListItem(id))
    await t.expect(new ApplicationReadView().view.visible).ok()
  }

  async openDaycarePlacementDialogById(id: string) {
    const link = this.getApplicationById(id).find(
      '[data-qa="primary-action-create-placement-plan"]'
    )
    await scrollThenClick(t, link)
  }

  async verifyApplication(applicationId: string) {
    await scrollThenClick(t, ApplicationListView.actionsMenu(applicationId))
    await t.click(ApplicationListView.actionsMenuItems.setVerified)
  }

  async clickApplicationCheckbox(applicationId: string) {
    await new Checkbox(
      Selector(`[data-qa="application-row-checkbox-${applicationId}"]`)
    ).click()
  }

  async moveToWaitingPlacement(applicationId: string) {
    await this.clickApplicationCheckbox(applicationId)
    await t.click(Selector(`[data-qa="action-bar-moveToWaitingPlacement"]`))
  }

  async sendDecisionsWithoutProposal(applicationId: string) {
    await this.clickApplicationCheckbox(applicationId)
    await t.click(
      Selector(`[data-qa="action-bar-sendDecisionsWithoutProposal"]`)
    )
  }

  async withdrawPlacementProposal(applicationId: string) {
    await this.clickApplicationCheckbox(applicationId)
    await t.click(Selector(`[data-qa="action-bar-withdrawPlacementProposal"]`))
  }

  getApplicationById(id: string) {
    return this.application.withAttribute('data-application-id', id)
  }

  async closeApplication() {
    if (await this.btnCloseApplication.hasAttribute('disabled')) {
      // Application was opened in new window/tab
      await t.closeWindow()
    } else {
      await t.click(this.btnCloseApplication)
    }
  }

  async openPlacementQueue() {
    await t.click(this.applicationsWaitingPlacement)
    await t.expect(this.applicationList.visible).ok()
  }

  async openDecisionQueue() {
    await t.click(this.applicationsWaitingDecision)
    await t.expect(this.applicationList.visible).ok()
  }

  async openPlacementProposalQueue() {
    await t.click(this.applicationsWaitingUnitConfirmation)
    await t.expect(this.applicationList.visible).ok()
  }

  async verifyNoteExists(note: string) {
    await t.expect(this.applicationNoteContent.innerText).contains(note)
  }

  async waitUntilApplicationsLoaded() {
    await t.expect(Selector('[data-qa="applications-list"]').exists).ok()
  }

  async fillNote(note: string) {
    const noteTextArea = Selector('[data-qa="note-container"]').find('textarea')
    await t.typeText(noteTextArea, note, {
      paste: true,
      replace: true
    })
  }

  async openDecisionEditorById(applicationId: string) {
    const link = this.getApplicationById(applicationId).find(
      '[data-qa="primary-action-edit-decisions"]'
    )
    await scrollThenClick(t, link)
  }

  async assertAgreementStatusNotAgreed() {
    await t
      .expect(this.agreementStatus.innerText)
      .contains('Ei ole sovittu yhdessä')
  }

  async assertContactDetails(expectedTel: string, expectedEmail: string) {
    await t.expect(this.otherGuardianTel.innerText).eql(expectedTel)
    await t.expect(this.otherGuardianEmail.innerText).eql(expectedEmail)
  }

  async assertDecisionGuardians(
    expectedGuardian: string,
    expectedOtherGuardian?: string
  ) {
    await t
      .expect(Selector('[data-qa="guardian-name"]').innerText)
      .eql(expectedGuardian)

    if (expectedOtherGuardian !== undefined) {
      await t
        .expect(Selector('[data-qa="other-guardian-name"]').innerText)
        .eql(expectedOtherGuardian)
    }
  }
}
