// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilFalse, waitUntilTrue } from '../../utils'
import type { Page } from '../../utils/page'
import { Checkbox, Element, Select, TextInput } from '../../utils/page'

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
  #missingEmailOrPhoneBox: Element
  #startEditing: Element
  #preferredName: Element
  #phone: Element
  #backupPhone: Element
  #email: Element
  #noEmail: Checkbox
  #save: Element
  verifiedEmailStatus: Element
  unverifiedEmailStatus: Element
  sendVerificationCode: Element
  verificationCodeField: TextInput
  verifyEmail: Element
  updateUsername: Element

  constructor(element: Element) {
    super(element)

    this.#missingEmailOrPhoneBox = element.find(
      '[data-qa="missing-email-or-phone-box"]'
    )
    this.#startEditing = element.find('[data-qa="start-editing"]')
    this.#preferredName = element.find('[data-qa="preferred-name"]')
    this.#phone = element.find('[data-qa="phone"]')
    this.#backupPhone = element.find('[data-qa="backup-phone"]')
    this.#email = element.find('[data-qa="email"]')
    this.#noEmail = new Checkbox(element.find('[data-qa="no-email"]'))
    this.#save = element.find('[data-qa="save"]')
    this.verifiedEmailStatus = element.findByDataQa('verified-email-status')
    this.unverifiedEmailStatus = element.findByDataQa('unverified-email-status')
    this.sendVerificationCode = element.findByDataQa('send-verification-code')
    this.verificationCodeField = new TextInput(
      element.findByDataQa('verification-code-field')
    )
    this.verifyEmail = element.findByDataQa('verify-email')
    this.updateUsername = element.findByDataQa('update-username')
  }

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
  username: Element
  activateCredentials: Element
  weakLoginEnabled: Element
  weakLoginDisabled: Element
  updatePassword: Element

  constructor(element: Element) {
    super(element)
    this.username = element.findByDataQa('username')
    this.activateCredentials = element.findByDataQa('activate-credentials')
    this.weakLoginEnabled = element.findByDataQa('weak-login-enabled')
    this.weakLoginDisabled = element.findByDataQa('weak-login-disabled')
    this.updatePassword = element.findByDataQa('update-password')
  }
}

export class WeakCredentialsModal extends Element {
  username: Element
  password: TextInput
  passwordInfo: Element
  confirmPassword: TextInput
  confirmPasswordInfo: Element
  unacceptablePasswordAlert: Element
  ok: Element

  constructor(page: Page) {
    super(page.findByDataQa('weak-credentials-modal'))
    this.username = this.findByDataQa('username')
    this.password = new TextInput(this.findByDataQa('password'))
    this.passwordInfo = this.findByDataQa('password-info')
    this.confirmPassword = new TextInput(this.findByDataQa('confirm-password'))
    this.confirmPasswordInfo = this.findByDataQa('confirm-password-info')
    this.unacceptablePasswordAlert = this.findByDataQa(
      'unacceptable-password-alert'
    )
    this.ok = this.findByDataQa('modal-okBtn')
  }
}

export class CitizenNotificationSettingsSection extends Element {
  startEditing = this.findByDataQa('start-editing')
  cancel = this.findByDataQa('cancel')
  save = this.findByDataQa('save')

  checkboxes = {
    message: new Checkbox(this.findByDataQa('message')),
    bulletin: new Checkbox(this.findByDataQa('bulletin')),
    income: new Checkbox(this.findByDataQa('income')),
    calendarEvent: new Checkbox(this.findByDataQa('calendar-event')),
    decision: new Checkbox(this.findByDataQa('decision')),
    document: new Checkbox(this.findByDataQa('document')),
    informalDocument: new Checkbox(this.findByDataQa('informal-document')),
    attendanceReservation: new Checkbox(
      this.findByDataQa('attendance-reservation')
    ),
    discussionTime: new Checkbox(this.findByDataQa('discussion-time'))
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
