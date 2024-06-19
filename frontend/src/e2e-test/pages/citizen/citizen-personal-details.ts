// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilFalse, waitUntilTrue } from '../../utils'
import { Checkbox, Element, Page, Select, TextInput } from '../../utils/page'

export default class CitizenPersonalDetails {
  personalDetailsSection: CitizenPersonalDetailsSection
  loginDetailsSection: LoginDetailsSection
  notificationSettingsSectiong: CitizenNotificationSettingsSection
  constructor(page: Page) {
    this.personalDetailsSection = new CitizenPersonalDetailsSection(
      page.findByDataQa('personal-details-section')
    )
    this.loginDetailsSection = new LoginDetailsSection(
      page.findByDataQa('login-details-section')
    )
    this.notificationSettingsSectiong = new CitizenNotificationSettingsSection(
      page.findByDataQa('notification-settings-section')
    )
  }
}

export class CitizenPersonalDetailsSection extends Element {
  #missingEmailOrPhoneBox = this.find('[data-qa="missing-email-or-phone-box"]')
  #startEditing = this.find('[data-qa="start-editing"]')
  #preferredName = this.find('[data-qa="preferred-name"]')
  #phone = this.find('[data-qa="phone"]')
  #backupPhone = this.find('[data-qa="backup-phone"]')
  #email = this.find('[data-qa="email"]')
  #noEmail = new Checkbox(this.find('[data-qa="no-email"]'))
  #save = this.find('[data-qa="save"]')

  async checkMissingEmailWarningIsShown() {
    await this.#missingEmailOrPhoneBox.waitUntilVisible()
    await this.#missingEmailOrPhoneBox.assertText((text) =>
      text.includes('Sähköpostiosoitteesi puuttuu')
    )
  }

  async checkMissingPhoneWarningIsShown() {
    await waitUntilTrue(() => this.#missingEmailOrPhoneBox.visible)
    await waitUntilTrue(async () =>
      ((await this.#missingEmailOrPhoneBox.text) ?? '').includes(
        'Puhelinnumerosi puuttuu'
      )
    )
  }

  async assertAlertIsNotShown() {
    await waitUntilFalse(() => this.#missingEmailOrPhoneBox.visible)
  }

  async editPersonalData(
    data: {
      preferredName: string
      phone: string | null
      backupPhone: string
      email: string | null
    },
    expectValid: boolean
  ) {
    await this.#startEditing.click()
    await new Select(this.#preferredName).selectOption({
      label: data.preferredName
    })
    if (data.phone)
      await new TextInput(this.#phone.find('input')).fill(data.phone)
    await new TextInput(this.#backupPhone.find('input')).fill(data.backupPhone)

    if (data.email === null) {
      if (!(await this.#noEmail.checked)) {
        await this.#noEmail.click()
      }
    } else {
      await new Checkbox(this.findByDataQa('no-email')).uncheck()
      await new TextInput(this.#email.find('input')).fill(data.email)
    }

    if (expectValid) {
      await this.#save.click()
      await waitUntilFalse(() => this.#startEditing.disabled)
    }
  }

  async assertSaveIsDisabled() {
    await waitUntilTrue(() => this.#save.hasAttribute('disabled'))
  }

  async checkPersonalData(data: {
    preferredName: string
    phone: string | null
    backupPhone: string
    email: string | null
  }) {
    await this.#preferredName.assertTextEquals(data.preferredName)
    await this.#phone.assertTextEquals(
      data.phone === null ? 'Puhelinnumerosi puuttuu' : data.phone
    )
    await this.#backupPhone.assertTextEquals(data.backupPhone)
    await this.#email.assertTextEquals(
      data.email === null ? 'Sähköpostiosoite puuttuu' : data.email
    )
  }
}

export class LoginDetailsSection extends Element {
  keycloakEmail = this.findByDataQa('keycloak-email')
}

export class CitizenNotificationSettingsSection extends Element {
  startEditing = this.findByDataQa('start-editing')
  cancel = this.findByDataQa('cancel')
  save = this.findByDataQa('save')

  checkboxes = {
    message: new Checkbox(this.findByDataQa('message')),
    bulletin: new Checkbox(this.findByDataQa('bulletin')),
    outdatedIncome: new Checkbox(this.findByDataQa('outdated-income')),
    calendarEvent: new Checkbox(this.findByDataQa('calendar-event')),
    decision: new Checkbox(this.findByDataQa('decision')),
    document: new Checkbox(this.findByDataQa('document')),
    informalDocument: new Checkbox(this.findByDataQa('informal-document')),
    missingAttendanceReservation: new Checkbox(
      this.findByDataQa('missing-attendance-reservation')
    )
  }

  async assertEditable(editable: boolean) {
    for (const checkbox of Object.values(this.checkboxes)) {
      await checkbox.assertDisabled(!editable)
    }
  }

  async assertAllChecked(checked: boolean) {
    for (const checkbox of Object.values(this.checkboxes)) {
      await checkbox.waitUntilChecked(checked)
    }
  }
}
