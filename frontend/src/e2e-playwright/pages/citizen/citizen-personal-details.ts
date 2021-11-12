// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  waitUntilEqual,
  waitUntilFalse,
  waitUntilTrue
} from 'e2e-playwright/utils'
import { Checkbox } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class CitizenPersonalDetails {
  constructor(private readonly page: Page) {}

  #missingEmailBox = this.page.locator('[data-qa="missing-email-box"]')
  #startEditing = this.page.locator('[data-qa="start-editing"]')
  #preferredName = this.page.locator('[data-qa="preferred-name"]')
  #phone = this.page.locator('[data-qa="phone"]')
  #backupPhone = this.page.locator('[data-qa="backup-phone"]')
  #email = this.page.locator('[data-qa="email"]')
  #noEmail = new Checkbox(this.page, '[data-qa="no-email"]')
  #save = this.page.locator('[data-qa="save"]')

  async checkMissingEmailWarningIsShown() {
    await waitUntilTrue(() => this.#missingEmailBox.isVisible())
  }

  async editPersonalData(data: {
    preferredName: string
    phone: string
    backupPhone: string
    email: string | null
  }) {
    await this.#startEditing.click()
    await this.#preferredName
      .locator('select')
      .selectOption({ label: data.preferredName })
    await this.#phone.locator('input').type(data.phone)
    await this.#backupPhone.locator('input').type(data.backupPhone)
    if (data.email === null) {
      if (!(await this.#noEmail.checked)) {
        await this.#noEmail.click()
      }
    } else {
      await this.#email.locator('input').type(data.email)
    }
    await this.#save.click()
    await waitUntilFalse(() => this.#startEditing.isDisabled())
  }

  async checkPersonalData(data: {
    preferredName: string
    phone: string
    backupPhone: string
    email: string | null
  }) {
    await waitUntilEqual(
      () => this.#preferredName.textContent(),
      data.preferredName
    )
    await waitUntilEqual(() => this.#phone.textContent(), data.phone)
    await waitUntilEqual(
      () => this.#backupPhone.textContent(),
      data.backupPhone
    )
    await waitUntilEqual(
      () => this.#email.textContent(),
      data.email === null ? 'Sähköpostiosoite puuttuu' : data.email
    )
  }
}
