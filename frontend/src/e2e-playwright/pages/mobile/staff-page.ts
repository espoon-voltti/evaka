// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Element, Page, TextInput } from 'e2e-playwright/utils/page'
import { waitUntilEqual } from '../../utils'

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

export class StaffAttendancePage {
  constructor(private readonly page: Page) {}

  #presentTab = this.page.find('[data-qa="present-tab"]')
  #notPresentTab = this.page.find('[data-qa="not-present-tab"]')

  #addNewExternalMemberButton = this.page.find(
    '[data-qa="add-external-member-btn"]'
  )
  #arrivedInput = new TextInput(this.page.find('[data-qa="input-arrived"]'))
  #nameInput = new TextInput(this.page.find('[data-qa="input-name"]'))
  #groupSelector = new TextInput(this.page.find('[data-qa="input-group"]'))

  #markArrivedBtn = this.page.find('[data-qa="mark-arrived-btn"]')

  #staffLink = (nth: number) =>
    this.page.findAll('[data-qa="staff-link"]').nth(nth)
  #status = this.page.find('[data-qa="employee-status"]')
  #arrivalTime = this.page.find('[data-qa="arrival-time"]')
  #markDepartedBtn = this.page.find('[data-qa="mark-departed-link"]')

  async clickAddNewExternalMemberButton() {
    await this.#addNewExternalMemberButton.click()
    await this.#arrivedInput.waitUntilVisible()
  }

  async setArrivedInfo(time: string, name: string, groupName: string) {
    await this.#arrivedInput.fill(time)

    await this.#nameInput.type(name)

    await this.#groupSelector.click()
    await this.#groupSelector.type(groupName)
    await this.page.find('[data-qa="item"]').click()

    await this.#markArrivedBtn.click()
  }

  async assertPresentStaffCount(expected: number) {
    await waitUntilEqual(
      () => this.#presentTab.textContent,
      `Läsnä(${expected})`
    )
  }

  async clickStaff(nth: number) {
    await this.#staffLink(nth).click()
  }

  async assertEmployeeStatus(expected: string) {
    await waitUntilEqual(() => this.#status.textContent, expected)
  }

  async clickPresentTab() {
    await this.#presentTab.click()
  }

  async clickNotPresentTab() {
    await this.#notPresentTab.click()
  }

  async assertArrivalTime(expected: string) {
    await waitUntilEqual(() => this.#arrivalTime.textContent, expected)
  }

  async clickMarkDepartedButton() {
    await this.#markDepartedBtn.click()
  }
}
