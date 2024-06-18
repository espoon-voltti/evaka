// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { OtherGuardianAgreementStatus } from 'lib-common/generated/api-types/application'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'

import { waitUntilFalse } from '../../../utils'
import {
  Checkbox,
  Combobox,
  DatePickerDeprecated,
  FileInput,
  Page,
  Radio,
  TextInput,
  Element
} from '../../../utils/page'

import ApplicationReadView from './application-read-view'

export default class ApplicationEditView {
  #saveButton: Element
  #urgentCheckbox: Checkbox
  #urgentAttachmentFileUpload: Element
  #preferredStartDate: DatePickerDeprecated
  #startTime: TextInput
  #endTime: TextInput
  #connectedDaycare: Checkbox
  #connectedDaycarePreferredStartDate: DatePickerDeprecated
  #connectedDaycarePreferredStartDateInputWarning: Element
  #preferredUnit: Combobox
  #applicantPhone: TextInput
  #applicantEmail: TextInput
  #shiftCareCheckbox: Checkbox
  #shiftCareAttachmentFileUpload: Element
  #guardianName: Element
  #guardianSsn: Element
  #guardianAddress: Element
  #secondGuardianToggle: Checkbox
  #secondGuardianPhone: TextInput
  #secondGuardianEmail: TextInput
  constructor(private readonly page: Page) {
    this.#saveButton = page.findByDataQa('save-application')
    this.#urgentCheckbox = new Checkbox(page.findByDataQa('checkbox-urgent'))
    this.#urgentAttachmentFileUpload = page.findByDataQa('file-upload-urgent')
    this.#preferredStartDate = new DatePickerDeprecated(
      page.findByDataQa('datepicker-start-date')
    )
    this.#startTime = new TextInput(page.findByDataQa('start-time'))
    this.#endTime = new TextInput(page.findByDataQa('end-time'))
    this.#connectedDaycare = new Checkbox(
      page.findByDataQa('checkbox-service-need-connected')
    )
    this.#connectedDaycarePreferredStartDate = new DatePickerDeprecated(
      page.findByDataQa('datepicker-connected-daycare-preferred-start-date')
    )
    this.#connectedDaycarePreferredStartDateInputWarning = page.findByDataQa(
      'input-warning-connected-daycare-preferred-start-date'
    )
    this.#preferredUnit = new Combobox(page.findByDataQa('preferred-unit'))
    this.#applicantPhone = new TextInput(
      page.findByDataQa('application-person-phone')
    )
    this.#applicantEmail = new TextInput(
      page.findByDataQa('application-person-email')
    )
    this.#shiftCareCheckbox = new Checkbox(
      page.findByDataQa('checkbox-service-need-shift-care')
    )
    this.#shiftCareAttachmentFileUpload = page.findByDataQa(
      'file-upload-shift-care'
    )
    this.#guardianName = page.findByDataQa('guardian-name')
    this.#guardianSsn = page.findByDataQa('guardian-ssn')
    this.#guardianAddress = page.findByDataQa('guardian-address')
    this.#secondGuardianToggle = new Checkbox(
      page.findByDataQa('application-second-guardian-toggle')
    )
    this.#secondGuardianPhone = new TextInput(
      page.findByDataQa('application-second-guardian-phone')
    )
    this.#secondGuardianEmail = new TextInput(
      page.findByDataQa('application-second-guardian-email')
    )
  }

  async saveApplication() {
    await this.#saveButton.click()
    return new ApplicationReadView(this.page)
  }

  async fillStartDate(date: string) {
    await this.#preferredStartDate.fill(date)
  }

  async checkConnectedDaycare() {
    await this.#connectedDaycare.check()
  }

  async fillTimes(start = '08:00', end = '16:00') {
    await this.#startTime.fill(start)
    await this.#endTime.fill(end)
  }

  async fillConnectedDaycarePreferredStartDate(date: string) {
    await this.#connectedDaycarePreferredStartDateInputWarning.waitUntilVisible()
    await this.#connectedDaycarePreferredStartDate.fill(date)
    await this.#connectedDaycarePreferredStartDateInputWarning.waitUntilHidden()
  }

  async selectPreschoolPlacementType(type: PlacementType) {
    await new Radio(
      this.page.findByDataQa(`preschool-placement-type-${type}`)
    ).check()
  }

  async selectPreschoolServiceNeedOption(nameFi: string) {
    await new Radio(
      this.page.findByDataQa(`preschool-service-need-option-${nameFi}`)
    ).check()
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

  async setDueDate(date: LocalDate) {
    await new DatePickerDeprecated(this.page.findByDataQa('due-date')).fill(
      date.format()
    )
  }

  async uploadUrgentAttachment(filePath: string) {
    await new FileInput(
      this.#urgentAttachmentFileUpload.find('[data-qa="btn-upload-file"]')
    ).setInputFiles(filePath)
  }

  async assertUrgentAttachmentUploaded(fileName: string) {
    await this.#urgentAttachmentFileUpload
      .find('[data-qa="file-download-button"]')
      .assertTextEquals(fileName)
  }

  async assertUrgencyAttachmentReceivedAtVisible(fileName: string) {
    const attachment = this.page.findByDataQa(`urgent-attachment-${fileName}`)
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
    await this.#shiftCareAttachmentFileUpload
      .find('[data-qa="file-download-button"]')
      .assertTextEquals(fileName)
  }

  async assertShiftCareAttachmentsDeleted() {
    await waitUntilFalse(
      () =>
        this.#shiftCareAttachmentFileUpload.find(
          '[data-qa="file-download-button"]'
        ).visible
    )
  }

  async assertGuardian(
    expectedName: string,
    expectedSsn: string,
    expectedAddress: string
  ) {
    await this.#guardianName.findText(expectedName).waitUntilVisible()
    await this.#guardianSsn.findText(expectedSsn).waitUntilVisible()
    await this.#guardianAddress.findText(expectedAddress).waitUntilVisible()
  }

  #guardianAgreementStatus = (status: OtherGuardianAgreementStatus) =>
    new Radio(
      this.page.findByDataQa(
        `radio-other-guardian-agreement-status-${status ?? 'null'}`
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
