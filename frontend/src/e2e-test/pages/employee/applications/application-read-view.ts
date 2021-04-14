// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import { DecisionType, UUID } from 'e2e-test-common/dev-api/types'
import config from 'e2e-test-common/config'

export default class ApplicationReadView {
  readonly url = config.employeeUrl
  readonly view = Selector('[data-qa="application-read-view"]')
  readonly title = this.view.find('h1')
  readonly editButton = Selector('[data-qa="edit-application"]')
  readonly vtjGuardianName = Selector('[data-qa="vtj-guardian-name"]')
  readonly vtjGuardianPhone = Selector('[data-qa="vtj-guardian-phone"]')
  readonly vtjGuardianEmail = Selector('[data-qa="vtj-guardian-email"]')
  readonly givenOtherGuardianPhone = Selector(
    '[data-qa="second-guardian-phone"]'
  )
  readonly giveOtherGuardianEmail = Selector(
    '[data-qa="second-guardian-email"]'
  )
  readonly applicationStatus = Selector('[data-qa="application-status"]')
  readonly decisionAcceptedStartDate = Selector(
    '[data-qa="application-status"]'
  )

  async assertDecisionAvailableForDownload(type: DecisionType) {
    const downloadLink = Selector(
      `[data-qa="application-decision-${type}"]`
    ).find('[data-qa="application-decision-download-available"]')
    await t.expect(downloadLink.exists).ok()
  }

  async assertDecisionDownloadPending(type: DecisionType) {
    const downloadPendingTxt = Selector(
      `[data-qa="application-decision-${type}"]`
    ).find('[data-qa="application-decision-download-pending"]')
    await t.expect(downloadPendingTxt.exists).ok()
  }

  async openApplicationByLink(id: UUID) {
    await t.navigateTo(`${this.url}/applications/${id}`)
  }

  async assertPageTitle(expectedTitle: string) {
    await t.expect(this.title.innerText).eql(expectedTitle)
  }

  async assertOtherVtjGuardian(
    expectedName: string,
    expectedPhone: string,
    expectedEmail: string
  ) {
    await t.expect(this.vtjGuardianName.innerText).eql(expectedName)
    await t.expect(this.vtjGuardianPhone.innerText).eql(expectedPhone)
    await t.expect(this.vtjGuardianEmail.innerText).eql(expectedEmail)
  }

  async assertOtherVtjGuardianMissing() {
    await t.expect(this.vtjGuardianName.with({ timeout: 3 }).visible).notOk()
  }

  async assertGivenOtherGuardianInfo(
    expectedPhone: string,
    expectedEmail: string
  ) {
    await t.expect(this.givenOtherGuardianPhone.innerText).eql(expectedPhone)
    await t.expect(this.giveOtherGuardianEmail.innerText).eql(expectedEmail)
  }

  async setDecisionStartDate(type: DecisionType, startDate: string) {
    const decision = Selector(`[data-qa="application-decision-${type}"]`)
    await t.expect(decision.exists).ok()

    const datePicker = decision.find('[data-qa="decision-start-date-picker"]')
    const input = datePicker
      .find('.react-datepicker__input-container')
      .nth(0)
      .find('input')
    await t.typeText(input, startDate, { replace: true })
    await t.click(datePicker.find('.react-datepicker__day--selected'))
    await t.expect(input.value).eql(startDate)
  }

  async acceptDecision(type: DecisionType) {
    const decision = Selector(`[data-qa="application-decision-${type}"]`)
    await t.expect(decision.exists).ok()

    const acceptRadio = decision.find('[data-qa="decision-radio-accept"]')
    await t.expect(acceptRadio.visible).ok()
    await t.click(acceptRadio)

    const submit = decision.find('[data-qa="decision-send-answer-button"]')
    await t.expect(submit.visible).ok()
    await t.click(submit)
  }

  async assertUrgentAttachmentExists(fileName: string) {
    await t
      .expect(Selector(`[data-qa="urgent-attachment-${fileName}"]`).exists)
      .ok()
  }

  async assertReceivedAtTextExists(fileName: string) {
    await t
      .expect(Selector(`[data-qa="attachment-${fileName}-received-at"]`).exists)
      .ok()
  }

  async assertUrgentAttachmentDoesNotExists(fileName: string) {
    await t
      .expect(Selector(`[data-qa="urgent-attachment-${fileName}"]`).exists)
      .notOk()
  }

  async assertExtendedCareAttachmentExists(fileName: string) {
    await t
      .expect(
        Selector(`[data-qa="extended-care-attachment-${fileName}"]`).exists
      )
      .ok()
  }

  async startEditing() {
    await t.click(this.editButton)
  }
}
