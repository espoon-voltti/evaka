// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { DecisionType } from 'lib-common/generated/api-types/decision'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import config from '../../../config'
import { expect } from '../../../playwright'
import type { Page, Element, ElementCollection } from '../../../utils/page'
import { Radio, TextInput, DatePicker } from '../../../utils/page'
import MessagesPage from '../messages/messages-page'

import ApplicationEditView from './application-edit-view'

export default class ApplicationReadView {
  #editButton: Element
  #guardianName: Element
  #vtjGuardianName: Element
  #vtjGuardianPhone: Element
  #vtjGuardianEmail: Element
  #givenOtherGuardianPhone: Element
  #giveOtherGuardianEmail: Element
  #applicationStatus: Element
  #sendMessageButton: Element
  #title: Element
  confidentialRadioYes: Radio
  confidentialRadioNo: Radio
  setVerifiedButton: Element
  private notes: ElementCollection
  constructor(private page: Page) {
    this.#editButton = page.findByDataQa('edit-application')
    this.#guardianName = page.findByDataQa('guardian-name')
    this.#vtjGuardianName = page.findByDataQa('vtj-guardian-name')
    this.#vtjGuardianPhone = page.findByDataQa('vtj-guardian-phone')
    this.#vtjGuardianEmail = page.findByDataQa('vtj-guardian-email')
    this.#givenOtherGuardianPhone = page.findByDataQa('second-guardian-phone')
    this.#giveOtherGuardianEmail = page.findByDataQa('second-guardian-email')
    this.#applicationStatus = page.findByDataQa('application-status')
    this.#sendMessageButton = page.findByDataQa('send-message-button')
    this.#title = this.page.findByDataQa('application-title').find('h1')
    this.notes = this.page.findAllByDataQa('note-container')
    this.confidentialRadioYes = new Radio(
      this.page.findByDataQa('confidential-yes')
    )
    this.confidentialRadioNo = new Radio(
      this.page.findByDataQa('confidential-no')
    )
    this.setVerifiedButton = this.page.findByDataQa('set-verified-btn')
  }

  async waitUntilLoaded() {
    await expect(this.page.findByDataQa('application-read-view')).toBeVisible()
    await expect(
      this.page.find('[data-qa="vtj-guardian-section"][data-isloading="false"]')
    ).toBeVisible()
  }

  async assertDecisionAvailableForDownload(type: DecisionType) {
    await expect(
      this.page
        .findByDataQa(`application-decision-${type}`)
        .findByDataQa('application-decision-download-available')
    ).toBeVisible()
  }

  async assertDecisionDownloadPending(type: DecisionType) {
    await expect(
      this.page
        .findByDataQa(`application-decision-${type}`)
        .findByDataQa('application-decision-download-pending')
    ).toBeVisible()
  }

  async navigateToApplication(id: UUID) {
    await this.page.goto(`${config.employeeUrl}/applications/${id}`)
  }

  async assertPageTitle(expectedTitle: string) {
    await expect(this.#title).toHaveText(expectedTitle, { useInnerText: true })
  }

  async assertGuardianName(expectedName: string) {
    await expect(this.#guardianName.findText(expectedName)).toBeVisible()
  }

  async assertOtherVtjGuardianName(expectedName: string) {
    await expect(this.#vtjGuardianName).toHaveText(expectedName, {
      useInnerText: true
    })
  }

  async assertOtherVtjGuardian(
    expectedName: string,
    expectedPhone: string,
    expectedEmail: string
  ) {
    await expect(this.#vtjGuardianName).toHaveText(expectedName, {
      useInnerText: true
    })
    await expect(this.#vtjGuardianPhone).toHaveText(expectedPhone, {
      useInnerText: true
    })
    await expect(this.#vtjGuardianEmail).toHaveText(expectedEmail, {
      useInnerText: true
    })
  }

  async assertOtherVtjGuardianMissing() {
    await expect(this.page.findByDataQa('no-other-vtj-guardian')).toBeVisible()
    await expect(this.#vtjGuardianName).toBeHidden()
  }

  async assertOtherGuardianSameAddress(status: boolean) {
    await expect(
      this.page
        .findByDataQa('other-vtj-guardian-lives-in-same-address')
        .findText(status ? 'Kyllä' : 'Ei')
    ).toBeVisible()
  }

  async assertOtherGuardianAgreementStatus(_status: false) {
    const expectedText = 'Ei ole sovittu yhdessä'
    await expect(
      this.page.findByDataQa('agreement-status').findText(expectedText)
    ).toBeVisible()
  }

  async assertGivenOtherGuardianInfo(
    expectedPhone: string,
    expectedEmail: string
  ) {
    await expect(this.#givenOtherGuardianPhone).toHaveText(expectedPhone, {
      useInnerText: true
    })
    await expect(this.#giveOtherGuardianEmail).toHaveText(expectedEmail, {
      useInnerText: true
    })
  }

  async setDecisionStartDate(type: DecisionType, startDate: string) {
    const datePicker = new DatePicker(
      this.page
        .findByDataQa(`application-decision-${type}`)
        .findByDataQa('decision-start-date-picker')
    )
    await datePicker.fill(startDate)
  }

  async acceptDecision(type: DecisionType) {
    const decision = this.page.findByDataQa(`application-decision-${type}`)

    const acceptRadio = new Radio(
      decision.findByDataQa('decision-radio-accept')
    )
    await acceptRadio.check()

    const submit = decision.findByDataQa('decision-send-answer-button')
    await submit.click()
    await expect(submit).toBeHidden()
  }

  async assertDecisionDisabled(type: DecisionType) {
    const decision = this.page.findByDataQa(`application-decision-${type}`)
    await expect(
      decision.findByDataQa('decision-send-answer-button')
    ).toBeHidden()
  }

  async assertApplicationStatus(text: string) {
    await expect(this.#applicationStatus.findText(text)).toBeVisible()
  }

  async assertUrgencyAttachmentReceivedAtVisible(
    fileName: string,
    byPaper = true
  ) {
    const attachment = this.page.findByDataQa(`urgent-attachment-${fileName}`)
    await expect(attachment).toBeVisible()

    const text = attachment.findByDataQa(`attachment-received-at`)
    await expect(text).toBeVisible()

    await expect(text).toContainText(
      byPaper ? 'Toimitettu paperisena' : 'Toimitettu sähköisesti'
    )
  }

  async assertExtendedCareAttachmentExists(fileName: string) {
    await expect(
      this.page.findByDataQa(`extended-care-attachment-${fileName}`)
    ).toBeVisible()
  }

  async assertExtendedCareAttachmentDoesNotExist(fileName: string) {
    await expect(
      this.page.findByDataQa(`extended-care-attachment-${fileName}`)
    ).toBeHidden()
  }

  async assertApplicantIsDead() {
    await expect(this.page.findByDataQa('applicant-dead')).toBeVisible()
  }

  async assertDueDate(dueDate: LocalDate) {
    await expect(this.page.findByDataQa('application-due-date')).toHaveText(
      dueDate.format(),
      { useInnerText: true }
    )
  }

  async startEditing(): Promise<ApplicationEditView> {
    await this.#editButton.click()
    return new ApplicationEditView(this.page)
  }

  async openMessagesPage(): Promise<MessagesPage> {
    const popup = await this.page.capturePopup(
      async () => await this.#sendMessageButton.click()
    )
    return new MessagesPage(popup)
  }

  async addNote(note: string) {
    await this.page.findByDataQa('add-note').click()
    const noteTextArea = new TextInput(this.notes.nth(0).find('textarea'))
    await noteTextArea.fill(note)
    await this.page.findByDataQa('save-note').click()
  }

  async assertNote(index: number, note: string) {
    await expect(
      this.notes.nth(index).findByDataQa('application-note-content')
    ).toHaveText(note, { useInnerText: true })
  }

  async assertNoNotes() {
    await expect(
      this.page.findByDataQa('application-notes-list')
    ).toBeAttached()
    await expect(this.notes.nth(0)).toBeHidden()
  }

  async clickMessageThreadLinkInNote(index: number): Promise<MessagesPage> {
    const popup = await this.page.capturePopup(async () => {
      await this.notes
        .nth(index)
        .findByDataQa('note-message-thread-link')
        .click()
    })
    return new MessagesPage(popup)
  }

  async assertNoteNotEditable(index: number) {
    await expect(this.notes.nth(index).findByDataQa('edit-note')).toBeHidden()
  }

  async editNote(index: number, note: string) {
    const noteContainer = this.notes.nth(index)
    await noteContainer.findByDataQa('edit-note').click()
    const input = new TextInput(noteContainer.find('textarea'))
    await input.fill(note)
    await noteContainer.findByDataQa('save-note').click()
  }

  async assertNoteNotDeletable(index: number) {
    await expect(this.notes.nth(index).findByDataQa('delete-note')).toBeHidden()
  }

  async deleteNote(index: number) {
    await this.notes.nth(index).findByDataQa('delete-note').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async reload() {
    return this.page.reload()
  }
}
