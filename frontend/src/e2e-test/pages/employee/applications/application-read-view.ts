// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DecisionType } from 'lib-common/generated/api-types/decision'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../../config'
import { waitUntilTrue } from '../../../utils'
import { DatePickerDeprecated, Page, Radio } from '../../../utils/page'
import MessagesPage from '../messages/messages-page'

import ApplicationEditView from './application-edit-view'

export default class ApplicationReadView {
  constructor(private page: Page) {}

  #title = this.page.findByDataQa('application-title').find('h1')
  #editButton = this.page.findByDataQa('edit-application')
  #vtjGuardianName = this.page.findByDataQa('vtj-guardian-name')
  #vtjGuardianPhone = this.page.findByDataQa('vtj-guardian-phone')
  #vtjGuardianEmail = this.page.findByDataQa('vtj-guardian-email')
  #givenOtherGuardianPhone = this.page.findByDataQa('second-guardian-phone')
  #giveOtherGuardianEmail = this.page.findByDataQa('second-guardian-email')
  #applicationStatus = this.page.findByDataQa('application-status')
  #sendMessageButton = this.page.findByDataQa('send-message-button')
  notesList = this.page.findByDataQa('application-notes-list')
  notes = this.notesList.findAllByDataQa('note-container')

  async waitUntilLoaded() {
    await this.page.findByDataQa('application-read-view').waitUntilVisible()
    await this.page
      .find('[data-qa="vtj-guardian-section"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async assertDecisionAvailableForDownload(type: DecisionType) {
    await this.page
      .find(`[data-qa="application-decision-${type}"]`)
      .find('[data-qa="application-decision-download-available"]')
      .waitUntilVisible()
  }

  async assertDecisionDownloadPending(type: DecisionType) {
    await this.page
      .find(`[data-qa="application-decision-${type}"]`)
      .find('[data-qa="application-decision-download-pending"]')
      .waitUntilVisible()
  }

  async navigateToApplication(id: UUID) {
    await this.page.goto(`${config.employeeUrl}/applications/${id}`)
  }

  async assertPageTitle(expectedTitle: string) {
    await this.#title.assertTextEquals(expectedTitle)
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
    await this.#vtjGuardianName.waitUntilHidden()
  }

  async assertGivenOtherGuardianInfo(
    expectedPhone: string,
    expectedEmail: string
  ) {
    await this.#givenOtherGuardianPhone.assertTextEquals(expectedPhone)
    await this.#giveOtherGuardianEmail.assertTextEquals(expectedEmail)
  }

  async setDecisionStartDate(type: DecisionType, startDate: string) {
    const datePicker = new DatePickerDeprecated(
      this.page
        .find(`[data-qa="application-decision-${type}"]`)
        .find('[data-qa="decision-start-date-picker"]')
    )
    await datePicker.fill(startDate)
  }

  async acceptDecision(type: DecisionType) {
    const decision = this.page.findByDataQa(`application-decision-${type}`)

    const acceptRadio = new Radio(
      decision.find('[data-qa="decision-radio-accept"]')
    )
    await acceptRadio.check()

    const submit = decision.find('[data-qa="decision-send-answer-button"]')
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

  async assertUrgentAttachmentExists(fileName: string) {
    await this.page
      .find(`[data-qa="urgent-attachment-${fileName}"]`)
      .waitUntilVisible()
  }

  async assertUrgencyAttachmentReceivedAtVisible(
    fileName: string,
    byPaper = true
  ) {
    const attachment = this.page.findByDataQa(`urgent-attachment-${fileName}`)
    await attachment.waitUntilVisible()

    const text = attachment.find(`[data-qa="attachment-received-at"]`)
    await text.waitUntilVisible()

    await waitUntilTrue(async () =>
      ((await text.text) ?? '').startsWith(
        byPaper ? 'Toimitettu paperisena' : 'Toimitettu sähköisesti'
      )
    )
  }

  async assertUrgentAttachmentDoesNotExists(fileName: string) {
    await this.page
      .find(`[data-qa="urgent-attachment-${fileName}"]`)
      .waitUntilHidden()
  }

  async assertExtendedCareAttachmentExists(fileName: string) {
    await this.page
      .find(`[data-qa="extended-care-attachment-${fileName}"]`)
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

  async clickMessageThreadLinkInNote(index: number): Promise<MessagesPage> {
    const popup = await this.page.capturePopup(async () => {
      await this.notes
        .nth(index)
        .findByDataQa('note-message-thread-link')
        .click()
    })
    return new MessagesPage(popup)
  }

  async assertNote(index: number, note: string) {
    await this.notes
      .nth(index)
      .findByDataQa('application-note-content')
      .assertTextEquals(note)
  }

  async assertNoteNotEditable(index: number) {
    await this.notes.nth(index).findByDataQa('edit-note').waitUntilHidden()
  }

  async assertNoteNotDeletable(index: number) {
    await this.notes.nth(index).findByDataQa('delete-note').waitUntilHidden()
  }

  async assertNoNotes() {
    await this.notesList.waitUntilAttached()
    await this.notes.nth(0).waitUntilHidden()
  }

  async reload() {
    return this.page.reload()
  }
}
