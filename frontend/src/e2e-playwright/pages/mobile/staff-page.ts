// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RawElement } from 'e2e-playwright/utils/element'
import { Page } from 'playwright'

export default class StaffPage {
  constructor(private readonly page: Page) {}

  #staffCount = new RawElement(this.page, '[data-qa="staff-count"]')
  #staffOtherCount = new RawElement(this.page, '[data-qa="staff-other-count"]')
  #cancelButton = new RawElement(this.page, '[data-qa="cancel-button"]')
  #confirmButton = new RawElement(this.page, '[data-qa="confirm-button"]')
  #occupancyRealized = new RawElement(
    this.page,
    '[data-qa="realized-occupancy"]'
  )

  get staffCount() {
    return this.#staffCount.find('[data-qa="value"]').innerText
  }

  get staffOtherCount() {
    return this.#staffOtherCount.find('[data-qa="value"]').innerText
  }

  async incStaffCount() {
    return this.#staffCount.find('[data-qa="plus-button"]').click()
  }

  async decStaffCount() {
    return this.#staffCount.find('[data-qa="minus-button"]').click()
  }

  async incStaffOtherCount() {
    return this.#staffOtherCount.find('[data-qa="plus-button"]').click()
  }

  async decStaffOtherCount() {
    return this.#staffOtherCount.find('[data-qa="minus-button"]').click()
  }

  async cancel() {
    return this.#cancelButton.click()
  }

  async confirm() {
    return this.#confirmButton.click()
  }

  get buttonsDisabled() {
    return Promise.all([
      this.#cancelButton.disabled,
      this.#confirmButton.disabled
    ]).then(([cancel, confirm]) => cancel && confirm)
  }

  get buttonsEnabled() {
    return this.buttonsDisabled.then((disabled) => !disabled)
  }

  get occupancy() {
    return this.#occupancyRealized.innerText
  }
}
