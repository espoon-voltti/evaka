// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { OtherGuardianAgreementStatus } from 'lib-common/generated/api-types/application'

import { waitUntilEqual, waitUntilFalse } from '../../../utils'
import {
  Checkbox,
  Combobox,
  DatePickerDeprecated,
  FileInput,
  Page,
  Radio,
  TextInput
} from '../../../utils/page'

import ApplicationReadView from './application-read-view'

export default class ApplicationEditView {
  constructor(private readonly page: Page) {}

  #saveButton = this.page.find('[data-qa="save-application"]')
  #urgentCheckbox = new Checkbox(this.page.find('[data-qa="checkbox-urgent"]'))
  #urgentAttachmentFileUpload = this.page.find('[data-qa="file-upload-urgent"]')
  #preferredStartDate = new DatePickerDeprecated(
    this.page.find('[data-qa="datepicker-start-date"]')
  )
  #startTime = new TextInput(this.page.find('[data-qa="start-time"]'))
  #endTime = new TextInput(this.page.find('[data-qa="end-time"]'))
  #preferredUnit = new Combobox(this.page.find('[data-qa="preferred-unit"]'))
  #applicantPhone = new TextInput(
    this.page.find('[data-qa="application-person-phone"]')
  )
  #applicantEmail = new TextInput(
    this.page.find('[data-qa="application-person-email"]')
  )
  #shiftCareCheckbox = new Checkbox(
    this.page.find('[data-qa="checkbox-service-need-shift-care"]')
  )
  #shiftCareAttachmentFileUpload = this.page.find(
    '[data-qa="file-upload-shift-care"]'
  )

  async saveApplication() {
    await this.#saveButton.click()
    return new ApplicationReadView(this.page)
  }

  async fillStartDate(date: string) {
    await this.#preferredStartDate.fill(date)
  }

  async fillTimes(start = '08:00', end = '16:00') {
    await this.#startTime.fill(start)
    await this.#endTime.fill(end)
  }

  async pickUnit(unitName: string) {
    await this.#preferredUnit.fillAndSelectFirst(unitName)
  }

  async fillApplicantPhoneAndEmail(phone: string, email: string) {
    await this.#applicantPhone.fill(phone)
    await this.#applicantEmail.fill(email)
  }

  async setUrgent() {
    if (await this.#urgentCheckbox.checked) {
      return
    }
    await this.#urgentCheckbox.click()
  }

  async uploadUrgentAttachment(filePath: string) {
    await new FileInput(
      this.#urgentAttachmentFileUpload.find('[data-qa="btn-upload-file"]')
    ).setInputFiles(filePath)
  }

  async assertUrgentAttachmentUploaded(fileName: string) {
    await waitUntilEqual(
      () =>
        this.#urgentAttachmentFileUpload.find(
          '[data-qa="file-download-button"]'
        ).innerText,
      fileName
    )
  }

  async assertUrgencyAttachmentReceivedAtVisible(fileName: string) {
    const attachment = this.page.find(
      `[data-qa="urgent-attachment-${fileName}"]`
    )
    await attachment.waitUntilVisible()
    await attachment
      .find('[data-qa="attachment-received-at"]')
      .waitUntilVisible()
  }

  async setShiftCareNeeded() {
    if (await this.#shiftCareCheckbox.checked) {
      return
    }
    await this.#shiftCareCheckbox.click()
  }

  async uploadShiftCareAttachment(filePath: string) {
    await new FileInput(
      this.#shiftCareAttachmentFileUpload.find('[data-qa="btn-upload-file"]')
    ).setInputFiles(filePath)
  }

  async deleteShiftCareAttachment(fileName: string) {
    await this.#shiftCareAttachmentFileUpload
      .find(`[data-qa="file-delete-button-${fileName}"]`)
      .click()
  }

  async assertShiftCareAttachmentUploaded(fileName: string) {
    await waitUntilEqual(
      () =>
        this.#shiftCareAttachmentFileUpload.find(
          '[data-qa="file-download-button"]'
        ).innerText,
      fileName
    )
  }

  async assertShiftCareAttachmentsDeleted() {
    await waitUntilFalse(
      () =>
        this.#shiftCareAttachmentFileUpload.find(
          '[data-qa="file-download-button"]'
        ).visible
    )
  }

  #guardianName = this.page.find('[data-qa="guardian-name"]')
  #guardianSsn = this.page.find('[data-qa="guardian-ssn"]')
  #guardianAddress = this.page.find('[data-qa="guardian-address"]')

  async assertGuardian(
    expectedName: string,
    expectedSsn: string,
    expectedAddress: string
  ) {
    await this.#guardianName.findText(expectedName).waitUntilVisible()
    await this.#guardianSsn.findText(expectedSsn).waitUntilVisible()
    await this.#guardianAddress.findText(expectedAddress).waitUntilVisible()
  }

  #secondGuardianToggle = new Checkbox(
    this.page.find('[data-qa="application-second-guardian-toggle"]')
  )
  #secondGuardianPhone = new TextInput(
    this.page.find('[data-qa="application-second-guardian-phone"]')
  )
  #secondGuardianEmail = new TextInput(
    this.page.find('[data-qa="application-second-guardian-email"]')
  )

  #guardianAgreementStatus = (status: OtherGuardianAgreementStatus) =>
    new Radio(
      this.page.find(
        `[data-qa="radio-other-guardian-agreement-status-${status ?? 'null'}"]`
      )
    )

  async fillSecondGuardianContactInfo(phone: string, email: string) {
    await this.#secondGuardianToggle.check()
    await this.#secondGuardianPhone.fill(phone)
    await this.#secondGuardianEmail.fill(email)
  }

  async setGuardianAgreementStatus(status: OtherGuardianAgreementStatus) {
    await this.#guardianAgreementStatus(status).check()
  }
}
