// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../utils/page'

export default class ApplicationDetailsPage {
  constructor(private page: Page) {}

  #guardianName = this.page.find('[data-qa="guardian-name"]')

  #vtjGuardianName = this.page.find('[data-qa="vtj-guardian-name"]')

  #otherGuardianAgreementStatus = this.page.find('[data-qa="agreement-status"]')

  #otherGuardianSameAddress = this.page.find(
    '[data-qa="other-vtj-guardian-lives-in-same-address"]'
  )

  #noOtherVtjGuardianText = this.page.find('[data-qa="no-other-vtj-guardian"]')

  #applicationStatus = this.page.find('[data-qa="application-status"]')

  async assertGuardianName(expectedName: string) {
    await this.#guardianName.findText(expectedName).waitUntilVisible()
  }

  async assertNoOtherVtjGuardian() {
    await this.#noOtherVtjGuardianText.waitUntilVisible()
  }

  async assertVtjGuardianName(expectedName: string) {
    await this.#vtjGuardianName.findText(expectedName).waitUntilVisible()
  }

  async assertOtherGuardianSameAddress(status: boolean) {
    await this.#otherGuardianSameAddress
      .findText(status ? 'Kyllä' : 'Ei')
      .waitUntilVisible()
  }

  async assertOtherGuardianAgreementStatus(_status: false) {
    const expectedText = 'Ei ole sovittu yhdessä'
    await this.#otherGuardianAgreementStatus
      .findText(expectedText)
      .waitUntilVisible()
  }

  async assertApplicationStatus(text: string) {
    await this.#applicationStatus.findText(text).waitUntilVisible()
  }
}
