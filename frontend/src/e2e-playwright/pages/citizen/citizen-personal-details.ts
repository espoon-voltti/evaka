// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  waitUntilEqual,
  waitUntilFalse,
  waitUntilTrue
} from 'e2e-playwright/utils'
import { Page, Checkbox, Select, TextInput } from 'e2e-playwright/utils/page'

export default class CitizenPersonalDetails {
  constructor(private readonly page: Page) {}

  #missingEmailBox = this.page.find('[data-qa="missing-email-box"]')
  #startEditing = this.page.find('[data-qa="start-editing"]')
  #preferredName = this.page.find('[data-qa="preferred-name"]')
  #phone = this.page.find('[data-qa="phone"]')
  #backupPhone = this.page.find('[data-qa="backup-phone"]')
  #email = this.page.find('[data-qa="email"]')
  #noEmail = new Checkbox(this.page.find('[data-qa="no-email"]'))
  #save = this.page.find('[data-qa="save"]')

  async checkMissingEmailWarningIsShown() {
    await waitUntilTrue(() => this.#missingEmailBox.visible)
  }

  async editPersonalData(data: {
    preferredName: string
    phone: string
    backupPhone: string
    email: string | null
  }) {
    await this.#startEditing.click()
    await new Select(this.#preferredName.find('select')).selectOption({
      label: data.preferredName
    })
    await new TextInput(this.#phone.find('input')).fill(data.phone)
    await new TextInput(this.#backupPhone.find('input')).fill(data.backupPhone)
    if (data.email === null) {
      if (!(await this.#noEmail.checked)) {
        await this.#noEmail.click()
      }
    } else {
      await new TextInput(this.#email.find('input')).fill(data.email)
    }
    await this.#save.click()
    await waitUntilFalse(() => this.#startEditing.disabled)
  }

  async checkPersonalData(data: {
    preferredName: string
    phone: string
    backupPhone: string
    email: string | null
  }) {
    await waitUntilEqual(
      () => this.#preferredName.textContent,
      data.preferredName
    )
    await waitUntilEqual(() => this.#phone.textContent, data.phone)
    await waitUntilEqual(() => this.#backupPhone.textContent, data.backupPhone)
    await waitUntilEqual(
      () => this.#email.textContent,
      data.email === null ? 'Sähköpostiosoite puuttuu' : data.email
    )
  }
}
