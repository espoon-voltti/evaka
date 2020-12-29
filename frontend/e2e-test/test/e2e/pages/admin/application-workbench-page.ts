// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import SearchFilter from './search-filters'
import ClubPlacementModal from './club-placement-modal'
import PlacementPage from '../employee/placement-page'
import { format, getYear } from 'date-fns'
import DecisionEditorPage from '../employee/decision-editor-page'
import EmployeeHome from '../../pages/employee/home'
import config from '../../config'
import ApplicationReadView from '../employee/applications/application-read-view'
import ApplicationListView from '../employee/applications/application-list-view'
import { scrollThenClick } from '../../utils/helpers'

export class ApplicationWorkbenchPage {
  readonly applicationListPath = 'employee/applications'
  readonly searchFilter: SearchFilter = SearchFilter.newSearch()
  readonly clubPlacementModal: ClubPlacementModal = new ClubPlacementModal()
  readonly placementPage: PlacementPage = new PlacementPage()
  readonly placementApplicationList = new PlacementApplicationList()
  readonly decisionEditorPage = new DecisionEditorPage()

  readonly reviewTab: Selector = Selector('[data-qa="review-queue"]')
  readonly mainNavbar = Selector('#main-navbar-menu')
  readonly headerApplications = Selector('[data-qa="header-review-queue"]')
  readonly applicationNoteContent = Selector(
    '[data-qa="application-note-content"]'
  )
  readonly btnDecisionConfirmation = Selector('[data-qa="btn-decision"]')

  readonly applicationsInbox: Selector = Selector(
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
  readonly allApplications: Selector = Selector(
    '[data-qa="application-status-filter-ALL"]'
  )

  readonly logo: Selector = Selector('.logo')
  readonly footer: Selector = Selector('.footer').find('span')
  readonly btnSelectApplication: Selector = Selector(
    '.select-application input'
  )
  readonly btnMarkApplicationAsChecked: Selector = Selector(
    '[data-qa="checked-by-admin-input"]'
  )
  readonly btnCheckApplication: Selector = Selector(
    '[data-qa="btn-check-application"]'
  )
  readonly btnSendToPlacementQueue: Selector = Selector(
    '[data-qa="btn-send-to-placement-queue"]'
  )
  // only matches when applications have been loaded
  readonly applicationList: Selector = Selector('[data-qa="applications-list"]')
  readonly pageLink: Selector = Selector('.page-link')
  readonly application: Selector = Selector('[data-qa="table-application-row"]')
  readonly btnCloseApplication: Selector = Selector(
    '[data-qa="close-application"]'
  )
  readonly btnEditApplication: Selector = Selector(
    '[data-qa="button-edit-application"]'
  )
  readonly applicationStatus: Selector = Selector(
    '[data-qa=application-details-application-status]'
  )
  readonly applicationModal: Selector = Selector('.application-details-modal')
  private readonly btnCreateManualApplication: Selector = Selector(
    '[data-qa="btn-create-manual-application"]'
  )
  readonly btnCreateManualApplicationModal: Selector = Selector(
    '[data-qa="btn-manual-application-modal-confirm"]'
  )
  readonly manualApplicationModalTitle: Selector = Selector(
    '[data-qa="manual-application-modal-title"]'
  )
  readonly manualApplicationErrorText: Selector = Selector(
    '[data-qa="message-modal-text-content"]'
  )

  readonly manualApplicationChildSSN: Selector = Selector(
    'input[name="child.socialSecurityNumber"]'
  )
  readonly manualApplicationGuardianSSN: Selector = Selector(
    'input[name="guardian.socialSecurityNumber"]'
  )
  readonly manualApplicationTypeSelection: Selector = Selector(
    '[name="applicationType"]'
  )
  readonly manualApplicationTypeSelectionOptions: Selector = this.manualApplicationTypeSelection.find(
    'option'
  )
  readonly manualApplicationSentDate = Selector('[data-qa="sent-date"]')
  readonly applicationOrigin: Selector = Selector('.application-origin')
  readonly hideFromGuardian: Selector = Selector('.hide-from-guardian')
  readonly transferApplicationIndicator = Selector(
    '[data-qa="transfer-application-indicator"]'
  )
  readonly paperApplicationIndicator = Selector(
    '[data-qa="paper-application-indicator"]'
  )
  readonly hiddenFromGuardianIndicator = Selector(
    '[data-qa="hidden-from-guardian-indicator"]'
  )

  readonly noSsnRadioButton: Selector = Selector(
    '[data-qa=socialSecurityNumberTypes-noSsn]'
  ).find('label')

  readonly childSearchInput: Selector = Selector('input[name=childNoSsn]')

  readonly guardianSearchInput: Selector = Selector('input[name=guardianNoSsn]')

  readonly noSsnWorkflowChildSelector: Selector = Selector(
    'select[name=childNoSsn]'
  )

  readonly noSsnWorkflowChildSelectorOptions: Selector = this.noSsnWorkflowChildSelector.find(
    'option'
  )

  readonly noSsnWorkflowGuardianSelector: Selector = Selector(
    'select[name=guardianNoSsn]'
  )

  readonly noSsnWorkflowGuardianSelectorOptions: Selector = this.noSsnWorkflowGuardianSelector.find(
    'option'
  )
  readonly decisionStatus: Selector = Selector('[data-qa="decision-status"]')

  readonly agreementStatus = Selector('[data-qa="agreement-status"]')
  readonly otherGuardianTel = Selector('[data-qa="second-guardian-phone"]')
  readonly otherGuardianEmail = Selector('[data-qa="second-guardian-email"]')

  getManualApplicationId() {
    return Selector('.new-application-view').getAttribute('data-qa')
  }

  async fillManualSsnDaycareApplication(
    childSsn: string,
    guardianSsn: string,
    additionalOptions?: {
      sentDate?: string
      doNotSend?: boolean
    }
  ): Promise<string> {
    await t.click(this.manualApplicationTypeSelection)
    await t.click(
      this.manualApplicationTypeSelectionOptions.withText(
        'Varhaiskasvatushakemus'
      )
    )
    await this.fillManualApplicationChildSSN(childSsn)
    await this.fillManualApplicationGuardianSSN(guardianSsn)

    if (additionalOptions?.sentDate) {
      await t.selectText(this.manualApplicationSentDate.find('input'))
      await t.typeText(
        this.manualApplicationSentDate.find('input'),
        additionalOptions.sentDate
      )
    }

    await t.click(this.btnCreateManualApplicationModal)

    return await this.fillApplication(additionalOptions)
  }

  async fillApplication(additionalOptions?: {
    doNotSend?: boolean
  }): Promise<string> {
    await this.fillManualApplicationStartDate(
      format(new Date(getYear(new Date()), 8, 1), 'dd.MM.yyyy')
    )

    await t.click(Selector('#daycare-start'))
    await t.click(Selector('#daycare-end'))

    await t.click(Selector('.unit-selector-wrapper'))
    await t.click(Selector('.multiselect__option--highlight'))

    await this.fillManualApplicationPhoneNumber('123456789')
    await this.fillManualApplicationEmail('e2e-testuser@test.com')

    const applicationId = await this.getManualApplicationId()

    if (!additionalOptions || !additionalOptions.doNotSend)
      await t.click(Selector('[data-qa="button-to-review"]'))
    return applicationId
  }

  async addNote(note: string) {
    await t.click(Selector('[data-qa="add-note"]'))
    await this.fillNote(note)
    await t.click(Selector('[data-qa="save-note"]'))
  }

  async deleteNote() {
    await t.click(Selector('[data-qa="delete-note"]'))
    await t.click(Selector('[data-qa="modal-okBtn"]'))
  }

  static async open(url: string) {
    await t.navigateTo(url)
    return new ApplicationWorkbenchPage()
  }

  isVisible() {
    return this.reviewTab.visible
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

  async createDecision(applicationId: string) {
    const application = this.applicationList.find(
      `[data-application-id="${applicationId}"]`
    )
    const createDecisionBtn = application.find(
      '[data-qa="btn-create-decision"]'
    )
    await t.click(createDecisionBtn)
  }

  async openPlacementById(id: string) {
    const button = this.applicationList
      .find(`[data-application-id="${id}"]`)
      .find('[data-qa="btn-open-placement"]')
    await t.expect(button.hasAttribute('disabled')).notOk()
    await t.click(button)
  }

  async openDaycarePlacementDialogById(id: string) {
    const link = this.getApplicationById(id).find(
      '[data-qa="primary-action-create-placement-plan"]'
    )
    await scrollThenClick(t, link)
  }

  async openApplicationByIndex(i: number) {
    await t.click(this.application.nth(i))
  }

  async selectApplication(i: number) {
    await t.click(this.btnSelectApplication.nth(i))
  }

  async markApplicationAsChecked() {
    await t.click(this.btnMarkApplicationAsChecked.with({ timeout: 10000 }))
  }

  async verifyApplication(applicationId: string) {
    await scrollThenClick(t, ApplicationListView.actionsMenu(applicationId))
    await t.click(ApplicationListView.actionsMenuItems.setVerified)
  }

  async clickApplicationCheckbox(applicationId: string) {
    await scrollThenClick(
      t,
      Selector(`[data-qa="application-row-checkbox-${applicationId}"]`, {
        timeout: 50
      })
    )
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

  getApplicationId(i: number) {
    return this.application.nth(i).getAttribute('data-application-id')
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

  getApplicationStatus() {
    return this.applicationStatus.textContent
  }

  async openApplicationInbox() {
    await t.click(this.applicationsInbox)
    await t.expect(this.applicationList.visible).ok()
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

  async openAllApplicationsQueue() {
    await t.click(this.allApplications)
    await t.expect(this.applicationList.visible).ok()
  }

  async verifyNoteExists(note: string) {
    await t.expect(this.applicationNoteContent.innerText).contains(note)
  }

  async confirmDecision() {
    await t.click(this.btnDecisionConfirmation)
  }

  async waitUntilApplicationsLoaded() {
    await t.expect(Selector('[data-qa="applications-list"]').exists).ok()
  }

  // Browses through all the pages in application list for specified application
  async applicationExists(id: string): Promise<boolean> {
    const application: Selector = Selector(`[data-application-id="${id}"]`)
    const pageCount = await this.pageLink.count

    for (let i = 0; i <= pageCount; i++) {
      if (await application.with({ timeout: 3000 }).exists) {
        return true
      } else {
        if (await this.pageLink.nth(i + 1).exists) {
          await t.click(this.pageLink.nth(i + 1))
        }
      }
    }
    return false
  }

  async redoSearch() {
    await t.click(this.searchFilter.statusCheckBoxAccepted)
    await t.expect(this.applicationList.visible).ok()
    await t.click(this.searchFilter.statusCheckBoxAccepted)
    await t.expect(this.applicationList.visible).ok()
  }

  async applicationExistsWithRetry(id: string) {
    let tries = 5
    let waitSecs = 1000 * 1
    while (!(await this.applicationExists(id)) && tries-- > 0) {
      await this.redoSearch()
      await t.wait(waitSecs)
      waitSecs *= 2
    }

    await t.expect(this.getApplicationById(id).visible).ok()
  }

  async assertApplicationIsInApplicationList(applicationId: string) {
    await this.openPlacementQueue()
    await t.expect(await this.applicationExists(applicationId)).ok()
  }

  async fillManualApplicationChildSSN(ssn: string) {
    await t
      .expect(this.manualApplicationChildSSN.visible)
      .ok()
      .typeText(this.manualApplicationChildSSN, ssn, {
        paste: true,
        replace: true
      })
  }

  async fillManualApplicationGuardianSSN(ssn: string) {
    await t
      .expect(this.manualApplicationGuardianSSN.visible)
      .ok()
      .typeText(this.manualApplicationGuardianSSN, ssn, {
        paste: true,
        replace: true
      })
  }

  async fillManualApplicationStartDate(date: string) {
    const dateInput = Selector('#daycare-care-details-datepicker .date-input')
    await t.click(dateInput)
    await t.typeText(dateInput, date)
    await t.pressKey('enter')
  }

  async fillManualApplicationPhoneNumber(phoneNumber: string) {
    const phoneInput = Selector('[name="phoneNumber"]')
    await t.expect(phoneInput).ok().typeText(phoneInput, phoneNumber, {
      paste: true,
      replace: true
    })
  }

  async fillManualApplicationEmail(email: string) {
    const emailInput = Selector('[name="email"]')
    await t.expect(emailInput).ok().typeText(emailInput, email, {
      paste: true,
      replace: true
    })
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

  async createGuardianWithoutSSN(
    firstName: string,
    lastName: string
  ): Promise<string> {
    const employeeHome = new EmployeeHome()
    await employeeHome.navigateToPersonSearch()
    await t.click(employeeHome.createPersonButton)

    await t.typeText(employeeHome.createPersonModal.firstNameInput, firstName)
    await t.typeText(employeeHome.createPersonModal.lastNameInput, lastName)
    await t.typeText(
      employeeHome.createPersonModal.dateOfBirthInput,
      '25.03.1979',
      { replace: true }
    )
    await t.typeText(
      employeeHome.createPersonModal.streetAddressInput,
      'Katu 1'
    )
    await t.typeText(employeeHome.createPersonModal.postalCodeInput, '02100')
    await t.typeText(employeeHome.createPersonModal.postOfficeInput, 'Espoo')
    await t.typeText(employeeHome.createPersonModal.phoneInput, '123123')

    await t.click(employeeHome.createPersonModal.confirmButton)
    await employeeHome.personSearch.filterByName(`${firstName} ${lastName}`)
    await employeeHome.personSearch.navigateToNthPerson(0)

    return await Selector('.person-profile-wrapper').getAttribute(
      'data-person-id'
    )
  }

  async createChildWithoutSSN(
    firstName: string,
    lastName: string
  ): Promise<string> {
    const employeeHome = new EmployeeHome()
    await employeeHome.navigateToPersonSearch()
    await t.click(employeeHome.createPersonButton)

    await t.typeText(employeeHome.createPersonModal.firstNameInput, firstName)
    await t.typeText(employeeHome.createPersonModal.lastNameInput, lastName)
    await t.typeText(
      employeeHome.createPersonModal.dateOfBirthInput,
      '11.09.2015',
      { replace: true }
    )
    await t.typeText(
      employeeHome.createPersonModal.streetAddressInput,
      'Katu 1'
    )
    await t.typeText(employeeHome.createPersonModal.postalCodeInput, '02100')
    await t.typeText(employeeHome.createPersonModal.postOfficeInput, 'Espoo')

    await t.click(employeeHome.createPersonModal.confirmButton)
    await employeeHome.personSearch.filterByName(`${firstName} ${lastName}`)
    await employeeHome.personSearch.navigateToNthPerson(0)

    return await Selector('.child-information-wrapper').getAttribute(
      'data-person-id'
    )
  }

  async openManualApplicationModal() {
    // Make sure application list is stable and isn't loading anything.
    // Otherwise the button in the next step might move vertically and TestCafe
    // might click the wrong element
    await t.expect(this.applicationList.visible).ok()

    await t.click(this.btnCreateManualApplication)
    await t.expect(this.manualApplicationModalTitle.exists).ok()
  }

  async createNoSSNApplication(childName: string, guardianName: string) {
    await t.navigateTo(config.adminUrl)
    await this.openManualApplicationModal()

    await this.selectDaycare()
    await this.selectNoSsn()
    await this.searchForChild(childName)
    await this.selectChild(childName)
    await this.searchForGuardian(guardianName)
    await this.selectGuardian(guardianName)

    await t.click(this.btnCreateManualApplicationModal)
  }

  async selectNoSsn() {
    await t.click(this.noSsnRadioButton)
  }

  async searchForChild(searchTerm: string) {
    await t.typeText(this.childSearchInput, searchTerm)
  }

  async searchForGuardian(searchTerm: string) {
    await t.typeText(this.guardianSearchInput, searchTerm)
  }

  async selectGuardian(name: string) {
    await t.click(this.noSsnWorkflowGuardianSelector)
    await t.click(this.noSsnWorkflowGuardianSelectorOptions.withText(name))
  }

  async selectChild(name: string) {
    await t.click(this.noSsnWorkflowChildSelector)
    await t.click(this.noSsnWorkflowChildSelectorOptions.withText(name))
  }

  async createApplication() {
    await t.click(this.btnCreateManualApplicationModal)
  }

  async selectDaycare() {
    await t.click(this.manualApplicationTypeSelection)
    await t.click(
      this.manualApplicationTypeSelectionOptions.withText(
        'Varhaiskasvatushakemus'
      )
    )
  }

  async confirmApplication() {
    await t.click(Selector('[data-qa="button-to-review"]'))
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

  async assertDecisionMessageContains(expectedMessageContent: string) {
    await t
      .expect(Selector('.message-container').innerText)
      .contains(expectedMessageContent)
  }
}

class PlacementApplicationList {
  readonly list: Selector = Selector('.application-list')
  readonly application: Selector = Selector('.application-item')

  async openPlacementById(id: string) {
    const button = this.list
      .find(`[data-application-id="${id}"]`)
      .find('[data-qa="btn-open-placement"]')
    await t.click(button)
  }

  getApplicationId(i: number) {
    return this.application.nth(i).getAttribute('data-application-id')
  }
}
