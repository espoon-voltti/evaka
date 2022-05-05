// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareGroup } from '../../dev-api/types'
import { waitUntilEqual } from '../../utils'
import { Combobox, Element, Page, TextInput } from '../../utils/page'

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

  #tabs = {
    present: this.page.findByDataQa('present-tab'),
    absent: this.page.findByDataQa('absent-tab')
  }

  #addNewExternalMemberButton = this.page.findByDataQa(
    'add-external-member-btn'
  )
  #externalArrivalPage = {
    arrivedInput: new TextInput(this.page.findByDataQa('input-arrived')),
    nameInput: new TextInput(this.page.findByDataQa('input-name')),
    groupSelect: new Combobox(this.page.findByDataQa('input-group')),
    markArrivedBtn: this.page.findByDataQa('mark-arrived-btn')
  }
  #externalMemberPage = {
    arrivalTime: this.page.findByDataQa('arrival-time'),
    departureTimeInput: new TextInput(
      this.page.findByDataQa('departure-time-input')
    ),
    markDepartedBtn: this.page.findByDataQa('mark-departed-link')
  }

  #staffLink = (name: string) =>
    this.page.find(`[data-qa="staff-link"]`, { hasText: name })
  #status = this.page.find('[data-qa="employee-status"]')

  async markNewExternalStaffArrived(
    time: string,
    name: string,
    group: DaycareGroup
  ) {
    await this.#addNewExternalMemberButton.click()

    await this.#externalArrivalPage.arrivedInput.fill(time)
    await this.#externalArrivalPage.nameInput.type(name)
    await this.#externalArrivalPage.groupSelect.fillAndSelectItem(
      group.name,
      group.id
    )

    await this.#externalArrivalPage.markArrivedBtn.click()
  }

  async assertPresentStaffCount(expected: number) {
    await waitUntilEqual(
      () => this.#tabs.present.textContent,
      `Läsnä(${expected})`
    )
  }

  async openStaffPage(name: string) {
    await this.#staffLink(name).click()
  }

  async assertEmployeeStatus(expected: string) {
    await waitUntilEqual(() => this.#status.textContent, expected)
  }

  async selectTab(tab: 'present' | 'absent') {
    await this.#tabs[tab].click()
  }

  async assertEmployeeArrivalTime(expected: string) {
    await waitUntilEqual(
      () => this.#externalMemberPage.arrivalTime.textContent,
      expected
    )
  }

  async markExternalStaffDeparted(time?: string) {
    if (time) {
      await this.#externalMemberPage.departureTimeInput.fill(time)
    }
    await this.#externalMemberPage.markDepartedBtn.click()
  }
}
