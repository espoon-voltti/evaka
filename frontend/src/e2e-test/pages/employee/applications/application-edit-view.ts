// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import { format } from 'date-fns'
import { OtherGuardianAgreementStatus } from 'e2e-test-common/dev-api/types'
import {
  scrollThenClick,
  selectFirstComboboxOption
} from '../../../utils/helpers'

export default class ApplicationEditView {
  readonly readView = Selector('[data-qa="application-read-view"]')
  readonly editView = Selector('[data-qa="application-edit-view"]')
  readonly title = Selector('h1')
  readonly applicationGuardianName = Selector('[data-qa="guardian-name"]')
  readonly applicationGuardianSsn = Selector('[data-qa="guardian-ssn"]')
  readonly applicationGuardianAddress = Selector('[data-qa="guardian-address"]')
  readonly givenOtherGuardianPhone = Selector(
    '[data-qa="second-guardian-phone"]'
  )
  readonly giveOtherGuardianEmail = Selector(
    '[data-qa="second-guardian-email"]'
  )
  readonly preferredStartDate = Selector(
    '[data-qa="datepicker-start-date"]'
  ).find('input')
  readonly startTime = Selector('[data-qa="start-time"]')
  readonly endTime = Selector('[data-qa="end-time"]')
  readonly preferredUnit = Selector('[data-qa="preferred-unit"]')
  readonly applicantPhone = Selector('[data-qa="application-person-phone"]')
  readonly applicantEmail = Selector('[data-qa="application-person-email"]')
  readonly secondGuardianToggle = Selector(
    '[data-qa="application-second-guardian-toggle"]'
  )
  readonly secondGuardianPhone = Selector(
    '[data-qa="application-second-guardian-phone"]'
  )
  readonly secondGuardianEmail = Selector(
    '[data-qa="application-second-guardian-email"]'
  )
  readonly guardianAgreementStatus = (status: OtherGuardianAgreementStatus) =>
    Selector(
      `[data-qa="radio-other-guardian-agreement-status-${status ?? 'null'}"]`,
      { timeout: 50 }
    )
  readonly urgentCheckbox = Selector('[data-qa="checkbox-urgent"] label')
  readonly urgentFileUpload = Selector('[data-qa="file-upload-urgent"]')
  readonly shiftCareCheckbox = Selector(
    '[data-qa="checkbox-service-need-shift-care"] label'
  )
  readonly shiftCareFileUpload = Selector('[data-qa="file-upload-shift-care"]')
  readonly saveButton = Selector('[data-qa="save-application"]')

  async assertPageTitle(expectedTitle: string) {
    await t.expect(this.title.innerText).eql(expectedTitle)
  }

  async assertApplicationGuardian(
    expectedName: string,
    expectedSsn: string,
    expectedAddress: string
  ) {
    await t.expect(this.applicationGuardianName.innerText).eql(expectedName)
    await t.expect(this.applicationGuardianSsn.innerText).eql(expectedSsn)
    await t
      .expect(this.applicationGuardianAddress.innerText)
      .eql(expectedAddress)
  }

  async assertOtherVtjGuardian(
    expectedName: string,
    expectedPhone: string,
    expectedEmail: string
  ) {
    await t.expect(this.applicationGuardianName.innerText).eql(expectedName)
    await t.expect(this.applicationGuardianSsn.innerText).eql(expectedPhone)
    await t.expect(this.applicationGuardianAddress.innerText).eql(expectedEmail)
  }

  async assertOtherVtjGuardianMissing() {
    await t
      .expect(this.applicationGuardianName.with({ timeout: 3 }).visible)
      .notOk()
  }

  async assertGivenOtherGuardianInfo(
    expectedPhone: string,
    expectedEmail: string
  ) {
    await t.expect(this.givenOtherGuardianPhone.innerText).eql(expectedPhone)
    await t.expect(this.giveOtherGuardianEmail.innerText).eql(expectedEmail)
  }

  async fillStartDate(date: Date) {
    await t.click(this.preferredStartDate)
    await t.typeText(this.preferredStartDate, format(date, 'dd.MM.yyyy'))
    await t.pressKey('tab enter')
  }

  async fillTimes(start = '08:00', end = '16:00') {
    await t.click(this.startTime)
    await t.typeText(this.startTime, start)
    await t.click(this.endTime)
    await t.typeText(this.endTime, end)
  }

  async pickUnit(unitName: string) {
    await selectFirstComboboxOption(this.preferredUnit, unitName)
  }

  async fillApplicantPhoneAndEmail(phone: string, email: string) {
    await scrollThenClick(t, this.applicantPhone)
    await t.typeText(this.applicantPhone, phone)
    await scrollThenClick(t, this.applicantEmail)
    await t.typeText(this.applicantEmail, email)
  }

  async fillSecondGuardianContactInfo(phone: string, email: string) {
    if (!(await this.secondGuardianToggle.checked)) {
      await scrollThenClick(t, this.secondGuardianToggle)
    }
    await scrollThenClick(t, this.secondGuardianPhone)
    await t.typeText(this.secondGuardianPhone, phone)
    await scrollThenClick(t, this.secondGuardianEmail)
    await t.typeText(this.secondGuardianEmail, email)
  }

  async setGuardianAgreementStatus(value: OtherGuardianAgreementStatus) {
    await scrollThenClick(t, this.guardianAgreementStatus(value))
  }

  async saveApplication() {
    await t.click(this.saveButton)
  }
}
