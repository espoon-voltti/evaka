// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Locator, Page } from 'playwright'

export default class StaffPage {
  constructor(private readonly page: Page) {}

  #staffCount = this.page.locator('[data-qa="staff-count"]')
  #staffOtherCount = this.page.locator('[data-qa="staff-other-count"]')
  #cancelButton = this.page.locator('[data-qa="cancel-button"]')
  #confirmButton = this.page.locator('[data-qa="confirm-button"]')
  #occupancyRealized = this.page.locator('[data-qa="realized-occupancy"]')
  #updated = this.page.locator('[data-qa="updated"]')

  private countButton(parent: Locator, which: 'plus' | 'minus') {
    return parent.locator(`[data-qa="${which}-button"]`)
  }

  get staffCount() {
    return this.#staffCount.locator('[data-qa="value"]').innerText()
  }

  get staffOtherCount() {
    return this.#staffOtherCount.locator('[data-qa="value"]').innerText()
  }

  async incDecButtonsVisible(): Promise<boolean[]> {
    return Promise.all(
      [this.#staffCount, this.#staffOtherCount]
        .map((parent) =>
          (['plus', 'minus'] as const).map((which) =>
            this.countButton(parent, which)
          )
        )
        .flat()
        .map((el) => el.isVisible())
    )
  }

  async incStaffCount() {
    return this.countButton(this.#staffCount, 'plus').click()
  }

  async decStaffCount() {
    return this.countButton(this.#staffCount, 'minus').click()
  }

  async incStaffOtherCount() {
    return this.countButton(this.#staffOtherCount, 'plus').click()
  }

  async decStaffOtherCount() {
    return this.countButton(this.#staffOtherCount, 'minus').click()
  }

  async cancel() {
    return this.#cancelButton.click()
  }

  async confirm() {
    return this.#confirmButton.click()
  }

  get buttonsDisabled() {
    return Promise.all([
      this.#cancelButton.isDisabled(),
      this.#confirmButton.isDisabled()
    ]).then(([cancel, confirm]) => cancel && confirm)
  }

  get buttonsEnabled() {
    return this.buttonsDisabled.then((disabled) => !disabled)
  }

  get updated() {
    return this.#updated.innerText()
  }

  get occupancy() {
    return this.#occupancyRealized.innerText()
  }
}
