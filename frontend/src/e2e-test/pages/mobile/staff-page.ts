// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DaycareGroup } from '../../dev-api/types'
import { waitUntilEqual } from '../../utils'
import { Combobox, Element, Page, Select, TextInput } from '../../utils/page'

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
  #pinInput = this.page.findByDataQa('pin-input')

  #anyArrivalPage = {
    arrivedInput: new TextInput(this.page.findByDataQa('input-arrived')),
    markArrivedBtn: this.page.findByDataQa('mark-arrived-btn')
  }
  #externalArrivalPage = {
    nameInput: new TextInput(this.page.findByDataQa('input-name')),
    groupSelect: new Combobox(this.page.findByDataQa('input-group'))
  }
  #staffArrivalPage = {
    groupSelect: new Select(this.page.findByDataQa('group-select'))
  }
  #staffDeparturePage = {
    departureTime: new TextInput(this.page.findByDataQa('set-time')),
    markDepartedBtn: this.page.findByDataQa('mark-departed-btn')
  }

  #anyMemberPage = {
    back: this.page.findByDataQa('back-btn'),
    status: this.page.findByDataQa('employee-status'),
    arrivalTime: this.page.findByDataQa('arrival-time'),
    markDepartedLink: this.page.findByDataQa('mark-departed-link')
  }
  #staffMemberPage = {
    markArrivedBtn: this.page.findByDataQa('mark-arrived-link'),
    departureTime: this.page.findByDataQa('departure-time')
  }
  #externalMemberPage = {
    departureTimeInput: new TextInput(
      this.page.findByDataQa('departure-time-input')
    )
  }

  #staffLink = (name: string) =>
    this.page.find(`[data-qa="staff-link"]`, { hasText: name })

  async markNewExternalStaffArrived(
    time: string,
    name: string,
    group: DaycareGroup
  ) {
    await this.#addNewExternalMemberButton.click()

    await this.#anyArrivalPage.arrivedInput.fill(time)
    await this.#externalArrivalPage.nameInput.type(name)
    await this.#externalArrivalPage.groupSelect.fillAndSelectItem(
      group.name,
      group.id
    )

    await this.#anyArrivalPage.markArrivedBtn.click()
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
    await waitUntilEqual(() => this.#anyMemberPage.status.textContent, expected)
  }

  async selectTab(tab: 'present' | 'absent') {
    await this.#tabs[tab].click()
  }

  async goBackFromMemberPage() {
    await this.#anyMemberPage.back.click()
  }

  async assertEmployeeArrivalTime(expected: string) {
    await waitUntilEqual(
      () => this.#anyMemberPage.arrivalTime.textContent,
      expected
    )
  }

  async assertEmployeeDepartureTime(expected: string) {
    await waitUntilEqual(
      () => this.#staffMemberPage.departureTime.textContent,
      expected
    )
  }

  async markStaffArrived(args: {
    pin: string
    time: string
    group: DaycareGroup
  }) {
    await this.#staffMemberPage.markArrivedBtn.click()
    await this.#pinInput.locator.type(args.pin)
    await this.#anyArrivalPage.arrivedInput.fill(args.time)
    await this.#staffArrivalPage.groupSelect.selectOption(args.group.id)
    await this.#anyArrivalPage.markArrivedBtn.click()
  }

  async markStaffDeparted(args: { pin: string; time?: string }) {
    await this.#anyMemberPage.markDepartedLink.click()
    await this.#pinInput.locator.type(args.pin)
    if (args.time) {
      await this.#staffDeparturePage.departureTime.fill(args.time)
    }
    await this.#staffDeparturePage.markDepartedBtn.click()
  }

  async markExternalStaffDeparted(time?: string) {
    if (time) {
      await this.#externalMemberPage.departureTimeInput.fill(time)
    }
    await this.#anyMemberPage.markDepartedLink.click()
  }
}
