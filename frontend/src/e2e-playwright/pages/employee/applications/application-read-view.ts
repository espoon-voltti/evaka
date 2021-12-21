// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DatePickerDeprecated, Page, Radio } from '../../../utils/page'
import { DecisionType } from 'e2e-test-common/dev-api/types'
import config from 'e2e-test-common/config'
import { UUID } from 'lib-common/types'
import { waitUntilEqual, waitUntilTrue } from '../../../utils'

export default class ApplicationReadView {
  constructor(private page: Page) {}

  #title = this.page.find('h1')
  #editButton = this.page.find('[data-qa="edit-application"]')
  #vtjGuardianName = this.page.find('[data-qa="vtj-guardian-name"]')
  #vtjGuardianPhone = this.page.find('[data-qa="vtj-guardian-phone"]')
  #vtjGuardianEmail = this.page.find('[data-qa="vtj-guardian-email"]')
  #givenOtherGuardianPhone = this.page.find('[data-qa="second-guardian-phone"]')
  #giveOtherGuardianEmail = this.page.find('[data-qa="second-guardian-email"]')
  #applicationStatus = this.page.find('[data-qa="application-status"]')
  decisionAcceptedStartDate = this.page.find('[data-qa="application-status"]')

  async waitUntilLoaded() {
    await this.page.find('[data-qa="application-read-view"]').waitUntilVisible()
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
    await waitUntilEqual(() => this.#title.innerText, expectedTitle)
  }

  async assertOtherVtjGuardian(
    expectedName: string,
    expectedPhone: string,
    expectedEmail: string
  ) {
    await waitUntilEqual(() => this.#vtjGuardianName.innerText, expectedName)
    await waitUntilEqual(() => this.#vtjGuardianPhone.innerText, expectedPhone)
    await waitUntilEqual(() => this.#vtjGuardianEmail.innerText, expectedEmail)
  }

  async assertOtherVtjGuardianMissing() {
    await this.#vtjGuardianName.waitUntilHidden()
  }

  async assertGivenOtherGuardianInfo(
    expectedPhone: string,
    expectedEmail: string
  ) {
    await waitUntilEqual(
      () => this.#givenOtherGuardianPhone.innerText,
      expectedPhone
    )
    await waitUntilEqual(
      () => this.#giveOtherGuardianEmail.innerText,
      expectedEmail
    )
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
    const decision = this.page.find(`[data-qa="application-decision-${type}"]`)

    const acceptRadio = new Radio(
      decision.find('[data-qa="decision-radio-accept"]')
    )
    await acceptRadio.check()

    const submit = decision.find('[data-qa="decision-send-answer-button"]')
    await submit.click()
  }

  async assertApplicationStatus(text: string) {
    await this.#applicationStatus.find(`text=${text}`).waitUntilVisible()
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
    const attachment = this.page.find(
      `[data-qa="urgent-attachment-${fileName}"]`
    )
    await attachment.waitUntilVisible()

    const text = attachment.find(`[data-qa="attachment-received-at"]`)
    await text.waitUntilVisible()

    await waitUntilTrue(async () =>
      ((await text.textContent) ?? '').startsWith(
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

  async startEditing() {
    await this.#editButton.click()
  }
}
