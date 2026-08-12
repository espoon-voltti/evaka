// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { OtherGuardianAgreementStatus } from 'lib-common/generated/api-types/application'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { ServiceNeedOptionId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'

import { expect } from '../../../playwright'
import type { Page, Element } from '../../../utils/page'
import {
  Checkbox,
  DatePicker,
  FileUpload,
  Radio,
  TextInput
} from '../../../utils/page'
import { ApplicationEditorSections } from '../../application-editor-sections'

import ApplicationReadView from './application-read-view'

export default class ApplicationEditView {
  #saveButton: Element
  #urgentCheckbox: Checkbox
  urgentAttachmentFileUpload: FileUpload
  #preferredStartDate: DatePicker
  #startTime: TextInput
  #endTime: TextInput
  #connectedDaycare: Checkbox
  #connectedDaycarePreferredStartDate: DatePicker
  #connectedDaycarePreferredStartDateInputWarning: Element
  #preferredUnitsInput: TextInput
  #applicantPhone: TextInput
  #applicantEmail: TextInput
  #shiftCareCheckbox: Checkbox
  shiftCareAttachmentFileUpload: FileUpload
  serviceWorkerAttachmentFileUpload: FileUpload
  #guardianFirstName: Element
  #guardianLastName: Element
  #guardianSsn: Element
  #guardianAddress: Element
  #secondGuardianToggle: Checkbox
  #secondGuardianPhone: TextInput
  #secondGuardianEmail: TextInput
  #sections: ApplicationEditorSections
  constructor(private readonly page: Page) {
    this.#sections = new ApplicationEditorSections(page)
    this.#saveButton = page.findByDataQa('save-application')
    this.#urgentCheckbox = new Checkbox(page.findByDataQa('urgent-input'))
    this.urgentAttachmentFileUpload = new FileUpload(
      page.findByDataQa('urgent-file-upload')
    )
    this.#preferredStartDate = new DatePicker(
      page.findByDataQa('preferredStartDate-input')
    )
    this.#startTime = new TextInput(page.findByDataQa('startTime-input'))
    this.#endTime = new TextInput(page.findByDataQa('endTime-input'))
    this.#connectedDaycare = new Checkbox(
      page.findByDataQa('connectedDaycare-input')
    )
    this.#connectedDaycarePreferredStartDate = new DatePicker(
      page.findByDataQa('connectedDaycarePreferredStartDate-input')
    )
    this.#connectedDaycarePreferredStartDateInputWarning = page.findByDataQa(
      'connectedDaycarePreferredStartDate-input-info'
    )
    this.#preferredUnitsInput = new TextInput(
      page.find('[data-qa="preferredUnits-input"] input')
    )
    this.#applicantPhone = new TextInput(
      page.findByDataQa('guardianPhone-input')
    )
    this.#applicantEmail = new TextInput(
      page.findByDataQa('guardianEmail-input')
    )
    this.#shiftCareCheckbox = new Checkbox(page.findByDataQa('shiftCare-input'))
    this.shiftCareAttachmentFileUpload = new FileUpload(
      page.findByDataQa('shift-care-file-upload')
    )
    this.serviceWorkerAttachmentFileUpload = new FileUpload(
      page.findByDataQa('file-upload-service-worker')
    )
    this.#guardianFirstName = page.findByDataQa('guardian-first-name')
    this.#guardianLastName = page.findByDataQa('guardian-last-name')
    this.#guardianSsn = page.findByDataQa('guardian-ssn')
    this.#guardianAddress = page.findByDataQa('guardian-home-address')
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

  async cancelEditing() {
    await this.page.findByDataQa('cancel-editing').click()
    return new ApplicationReadView(this.page)
  }

  async fillStartDate(date: LocalDate) {
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
    await expect(
      this.#connectedDaycarePreferredStartDateInputWarning
    ).toBeVisible()
    await this.#connectedDaycarePreferredStartDate.fill(date)
    await expect(
      this.#connectedDaycarePreferredStartDateInputWarning
    ).toBeHidden()
  }

  async selectPreschoolPlacementType(type: PlacementType) {
    await new Radio(
      this.page.findByDataQa(`preschool-placement-type-${type}`)
    ).check()
  }

  async selectPreschoolServiceNeedOption(optionId: ServiceNeedOptionId) {
    await new Radio(
      this.page.findByDataQa(`service-need-option-${optionId}`)
    ).check()
  }

  async pickUnit(unitName: string) {
    await this.#sections.open('unitPreference')
    await this.#preferredUnitsInput.type(unitName)
    await this.page.keyboard.press('Enter')
  }

  async fillApplicantPhoneAndEmail(phone: string, email: string) {
    await this.#sections.open('contactInfo')
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
    await new DatePicker(this.page.findByDataQa('due-date')).fill(date.format())
  }

  async setShiftCareNeeded() {
    if (await this.#shiftCareCheckbox.checked) {
      return
    }
    await this.#shiftCareCheckbox.click()
  }

  async assertGuardian(
    expectedFirstName: string,
    expectedLastName: string,
    expectedSsn: string,
    expectedAddress: string
  ) {
    await this.#sections.open('contactInfo')
    // toHaveText (not findText().toBeVisible()) because an empty ssn/address
    // renders as a zero-size <span>, which Playwright reports as hidden.
    await expect(this.#guardianFirstName).toHaveText(expectedFirstName)
    await expect(this.#guardianLastName).toHaveText(expectedLastName)
    await expect(this.#guardianSsn).toHaveText(expectedSsn)
    await expect(this.#guardianAddress).toHaveText(expectedAddress)
  }

  #guardianAgreementStatus = (status: OtherGuardianAgreementStatus | null) =>
    new Radio(
      this.page.findByDataQa(
        `radio-other-guardian-agreement-status-${status ?? 'null'}`
      )
    )

  async fillSecondGuardianContactInfo(phone: string, email: string) {
    await this.#sections.open('contactInfo')
    await this.#secondGuardianToggle.check()
    await this.#secondGuardianPhone.fill(phone)
    await this.#secondGuardianEmail.fill(email)
  }

  async setGuardianAgreementStatus(
    status: OtherGuardianAgreementStatus | null
  ) {
    await this.#sections.open('contactInfo')
    await this.#guardianAgreementStatus(status).check()
  }
}
