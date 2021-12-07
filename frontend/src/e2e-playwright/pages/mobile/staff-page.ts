// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Element, Page } from 'e2e-playwright/utils/page'

export default class StaffPage {
  constructor(private readonly page: Page) {}

  #staffCount = this.page.find('[data-qa="staff-count"]')
  #staffOtherCount = this.page.find('[data-qa="staff-other-count"]')
  #cancelButton = this.page.find('[data-qa="cancel-button"]')
  #confirmButton = this.page.find('[data-qa="confirm-button"]')
  #occupancyRealized = this.page.find('[data-qa="realized-occupancy"]')
  #updated = this.page.find('[data-qa="updated"]')

  private countButton(parent: Element, which: 'plus' | 'minus') {
    return parent.find(`[data-qa="${which}-button"]`)
  }

  get staffCount() {
    return this.#staffCount.find('[data-qa="value"]').innerText
  }

  get staffOtherCount() {
    return this.#staffOtherCount.find('[data-qa="value"]').innerText
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
        .map((el) => el.visible)
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
      this.#cancelButton.disabled,
      this.#confirmButton.disabled
    ]).then(([cancel, confirm]) => cancel && confirm)
  }

  get buttonsEnabled() {
    return this.buttonsDisabled.then((disabled) => !disabled)
  }

  get updated() {
    return this.#updated.innerText
  }

  get occupancy() {
    return this.#occupancyRealized.innerText
  }
}
