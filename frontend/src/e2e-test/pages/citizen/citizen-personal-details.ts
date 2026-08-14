// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { Checkbox, Element, Select, TextInput } from '../../utils/page'

export default class CitizenPersonalDetails {
  personDetailsSection: PersonDetailsSection
  contactDetailsSection: ContactDetailsSection
  loginDetailsSection: LoginDetailsSection
  notificationSettingsSection: CitizenNotificationSettingsSection
  familySizeSection: FamilySizeSection
  addEmailTask: Element
  passkeysSection: PasskeysSection
  verifyEmailTask: Element
  addPhoneTask: Element

  constructor(page: Page) {
    this.personDetailsSection = new PersonDetailsSection(
      page.findByDataQa('person-details-section')
    )
    this.contactDetailsSection = new ContactDetailsSection(
      page.findByDataQa('contact-details-section')
    )
    this.loginDetailsSection = new LoginDetailsSection(
      page.findByDataQa('login-details-section')
    )
    this.notificationSettingsSection = new CitizenNotificationSettingsSection(
      page.findByDataQa('notification-settings-section')
    )
    this.familySizeSection = new FamilySizeSection(
      page.findByDataQa('family-size-section')
    )
    this.addEmailTask = page.findByDataQa('task-add-email')
    this.passkeysSection = new PasskeysSection(
      page.findByDataQa('passkeys-section')
    )
    this.verifyEmailTask = page.findByDataQa('task-verify-email')
    this.addPhoneTask = page.findByDataQa('task-add-phone')
  }
}

export class PersonDetailsSection extends Element {
  #startEditing = this.findByDataQa('start-editing')
  #preferredName = this.findByDataQa('preferred-name')
  #save = this.findByDataQa('save')

  async editPreferredName(preferredName: string) {
    await this.#startEditing.click()
    await new Select(this.#preferredName).selectOption({
      label: preferredName
    })
    await this.#save.click()
    await expect(this.#startEditing).toBeEnabled()
  }

  async assertPreferredName(preferredName: string) {
    await expect(this.#preferredName).toHaveText(preferredName)
  }
}

export class ContactDetailsSection extends Element {
  #startEditing = this.findByDataQa('start-editing')
  // in edit mode these resolve to the inputs, in view mode to the value texts
  #phone = this.findByDataQa('phone')
  #backupPhone = this.findByDataQa('backup-phone')
  #email = this.findByDataQa('email')
  #save = this.findByDataQa('save')
  verifiedEmailStatus = this.findByDataQa('verified-email-status')
  unverifiedEmailStatus = this.findByDataQa('unverified-email-status')
  sendVerificationCode = this.findByDataQa('send-verification-code')
  verificationCodeField = new TextInput(
    this.findByDataQa('verification-code-field')
  )
  verifyEmail = this.findByDataQa('verify-email')
  updateUsername = this.findByDataQa('update-username')

  async editContactDetails(
    data: {
      phone: string | null
      backupPhone: string
      email: string | null
    },
    expectValid: boolean
  ) {
    await this.#startEditing.click()
    if (data.phone) await new TextInput(this.#phone).fill(data.phone)
    await new TextInput(this.#backupPhone).fill(data.backupPhone)

    await new TextInput(this.#email).fill(data.email ?? '')

    if (expectValid) {
      await this.#save.click()
      await expect(this.#startEditing).toBeEnabled()
    }
  }

  async assertSaveIsDisabled() {
    await expect(this.#save).toBeDisabled()
  }

  async checkContactDetails(data: {
    phone: string | null
    backupPhone: string
    email: string | null
  }) {
    await expect(this.#phone).toHaveText(data.phone ?? '-')
    await expect(this.#backupPhone).toHaveText(data.backupPhone || '-')
    await expect(this.#email).toHaveText(data.email ?? '-')
  }
}

export class FamilySizeSection extends Element {
  #adults: Element
  #children: Element
  constructor(element: Element) {
    super(element)
    this.#adults = element.findByDataQa('family-adults')
    this.#children = element.findByDataQa('family-children')
  }

  #member(personId: string): Element {
    return this.findByDataQa(`family-member-${personId}`)
  }

  async assertAdultCount(count: number) {
    await expect(this.#adults).toContainText(`Aikuiset ${count}`)
  }

  async assertChildCount(count: number) {
    await expect(this.#children).toContainText(`Lapset ${count}`)
  }

  async assertMember(personId: string, name: string, isSelf = false) {
    await expect(this.#member(personId)).toHaveText(
      isSelf ? `${name} (sinä)` : name
    )
  }
}

export class PasskeysSection extends Element {
  addPasskey = this.findByDataQa('add-passkey')
  addError = this.findByDataQa('add-passkey-error')
  passkeys = this.findAllByDataQa('passkey')

  passkeyName(nth: number) {
    return this.passkeys.nth(nth).findByDataQa('passkey-name')
  }

  passkeyLastUsed(nth: number) {
    return this.passkeys.nth(nth).findByDataQa('passkey-last-used')
  }

  deletePasskey(nth: number) {
    return this.passkeys.nth(nth).findByDataQa('delete-passkey')
  }

  async editPasskeyName(nth: number, name: string) {
    const passkey = this.passkeys.nth(nth)
    await passkey.findByDataQa('edit-passkey').click()
    await new TextInput(passkey.findByDataQa('passkey-name-input')).fill(name)
    await passkey.findByDataQa('save-passkey-name').click()
    await expect(this.passkeyName(nth)).toHaveText(name)
  }
}

export class DeletePasskeyModal extends Element {
  ok: Element

  constructor(page: Page) {
    super(page.findByDataQa('delete-passkey-modal'))
    this.ok = this.findByDataQa('modal-okBtn')
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
