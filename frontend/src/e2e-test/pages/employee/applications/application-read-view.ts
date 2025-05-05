// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DecisionType } from 'lib-common/generated/api-types/decision'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../../config'
import { waitUntilTrue } from '../../../utils'
import {
  Page,
  Radio,
  Element,
  ElementCollection,
  TextInput,
  DatePicker
} from '../../../utils/page'
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
    await this.page.findByDataQa('application-read-view').waitUntilVisible()
    await this.page
      .find('[data-qa="vtj-guardian-section"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async assertDecisionAvailableForDownload(type: DecisionType) {
    await this.page
      .findByDataQa(`application-decision-${type}`)
      .findByDataQa('application-decision-download-available')
      .waitUntilVisible()
  }

  async assertDecisionDownloadPending(type: DecisionType) {
    await this.page
      .findByDataQa(`application-decision-${type}`)
      .findByDataQa('application-decision-download-pending')
      .waitUntilVisible()
  }

  async navigateToApplication(id: UUID) {
    await this.page.goto(`${config.employeeUrl}/applications/${id}`)
  }

  async assertPageTitle(expectedTitle: string) {
    await this.#title.assertTextEquals(expectedTitle)
  }

  async assertGuardianName(expectedName: string) {
    await this.#guardianName.findText(expectedName).waitUntilVisible()
  }

  async assertOtherVtjGuardianName(expectedName: string) {
    await this.#vtjGuardianName.assertTextEquals(expectedName)
  }

  async assertOtherVtjGuardian(
    expectedName: string,
    expectedPhone: string,
    expectedEmail: string
  ) {
    await this.#vtjGuardianName.assertTextEquals(expectedName)
    await this.#vtjGuardianPhone.assertTextEquals(expectedPhone)
    await this.#vtjGuardianEmail.assertTextEquals(expectedEmail)
  }

  async assertOtherVtjGuardianMissing() {
    await this.page.findByDataQa('no-other-vtj-guardian').waitUntilVisible()
    await this.#vtjGuardianName.waitUntilHidden()
  }

  async assertOtherGuardianSameAddress(status: boolean) {
    await this.page
      .findByDataQa('other-vtj-guardian-lives-in-same-address')
      .findText(status ? 'Kyllä' : 'Ei')
      .waitUntilVisible()
  }

  async assertOtherGuardianAgreementStatus(_status: false) {
    const expectedText = 'Ei ole sovittu yhdessä'
    await this.page
      .findByDataQa('agreement-status')
      .findText(expectedText)
      .waitUntilVisible()
  }

  async assertGivenOtherGuardianInfo(
    expectedPhone: string,
    expectedEmail: string
  ) {
    await this.#givenOtherGuardianPhone.assertTextEquals(expectedPhone)
    await this.#giveOtherGuardianEmail.assertTextEquals(expectedEmail)
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
    await submit.waitUntilHidden()
  }

  async assertDecisionDisabled(type: DecisionType) {
    const decision = this.page.findByDataQa(`application-decision-${type}`)
    await decision.findByDataQa('decision-send-answer-button').waitUntilHidden()
  }

  async assertApplicationStatus(text: string) {
    await this.#applicationStatus.findText(text).waitUntilVisible()
  }

  async assertUrgencyAttachmentReceivedAtVisible(
    fileName: string,
    byPaper = true
  ) {
    const attachment = this.page.findByDataQa(`urgent-attachment-${fileName}`)
    await attachment.waitUntilVisible()

    const text = attachment.findByDataQa(`attachment-received-at`)
    await text.waitUntilVisible()

    await waitUntilTrue(async () =>
      ((await text.text) ?? '').startsWith(
        byPaper ? 'Toimitettu paperisena' : 'Toimitettu sähköisesti'
      )
    )
  }

  async assertExtendedCareAttachmentExists(fileName: string) {
    await this.page
      .findByDataQa(`extended-care-attachment-${fileName}`)
      .waitUntilVisible()
  }

  async assertExtendedCareAttachmentDoesNotExist(fileName: string) {
    await this.page
      .findByDataQa(`extended-care-attachment-${fileName}`)
      .waitUntilHidden()
  }

  async assertApplicantIsDead() {
    await this.page.findByDataQa('applicant-dead').waitUntilVisible()
  }

  async assertDueDate(dueDate: LocalDate) {
    await this.page
      .findByDataQa('application-due-date')
      .assertTextEquals(dueDate.format())
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
    await this.notes
      .nth(index)
      .findByDataQa('application-note-content')
      .assertTextEquals(note)
  }

  async assertNoNotes() {
    await this.page.findByDataQa('application-notes-list').waitUntilAttached()
    await this.notes.nth(0).waitUntilHidden()
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
    await this.notes.nth(index).findByDataQa('edit-note').waitUntilHidden()
  }

  async editNote(index: number, note: string) {
    const noteContainer = this.notes.nth(index)
    await noteContainer.findByDataQa('edit-note').click()
    const input = new TextInput(noteContainer.find('textarea'))
    await input.fill(note)
    await noteContainer.findByDataQa('save-note').click()
  }

  async assertNoteNotDeletable(index: number) {
    await this.notes.nth(index).findByDataQa('delete-note').waitUntilHidden()
  }

  async deleteNote(index: number) {
    await this.notes.nth(index).findByDataQa('delete-note').click()
    await this.page.findByDataQa('modal-okBtn').click()
  }

  async reload() {
    return this.page.reload()
  }
}
