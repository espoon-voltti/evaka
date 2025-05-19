// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { OtherGuardianAgreementStatus } from 'lib-common/generated/api-types/application'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type LocalDate from 'lib-common/local-date'

import type { Page, Element } from '../../../utils/page'
import {
  Checkbox,
  Combobox,
  Radio,
  TextInput,
  FileUpload,
  DatePicker
} from '../../../utils/page'

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
  #preferredUnit: Combobox
  #applicantPhone: TextInput
  #applicantEmail: TextInput
  #shiftCareCheckbox: Checkbox
  shiftCareAttachmentFileUpload: FileUpload
  #guardianName: Element
  #guardianSsn: Element
  #guardianAddress: Element
  #secondGuardianToggle: Checkbox
  #secondGuardianPhone: TextInput
  #secondGuardianEmail: TextInput
  constructor(private readonly page: Page) {
    this.#saveButton = page.findByDataQa('save-application')
    this.#urgentCheckbox = new Checkbox(page.findByDataQa('checkbox-urgent'))
    this.urgentAttachmentFileUpload = new FileUpload(
      page.findByDataQa('file-upload-urgent')
    )
    this.#preferredStartDate = new DatePicker(
      page.findByDataQa('datepicker-start-date')
    )
    this.#startTime = new TextInput(page.findByDataQa('start-time'))
    this.#endTime = new TextInput(page.findByDataQa('end-time'))
    this.#connectedDaycare = new Checkbox(
      page.findByDataQa('checkbox-service-need-connected')
    )
    this.#connectedDaycarePreferredStartDate = new DatePicker(
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
    this.shiftCareAttachmentFileUpload = new FileUpload(
      page.findByDataQa('file-upload-shift-care')
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
    await new DatePicker(this.page.findByDataQa('due-date')).fill(date.format())
  }

  async setShiftCareNeeded() {
    if (await this.#shiftCareCheckbox.checked) {
      return
    }
    await this.#shiftCareCheckbox.click()
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
