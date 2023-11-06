// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'

import { DaycareGroup } from '../../dev-api/types'
import { waitUntilEqual } from '../../utils'
import {
  AsyncButton,
  Checkbox,
  Combobox,
  Element,
  Page,
  Select,
  TextInput
} from '../../utils/page'

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
    return this.#staffCount.find('[data-qa="value"]').text
  }

  get staffOtherCount() {
    return this.#staffOtherCount.find('[data-qa="value"]').text
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
    return this.#updated.text
  }

  get occupancy() {
    return this.#occupancyRealized.text
  }
}

export class StaffAttendancePage {
  editButton: Element
  arrivalTime: Element
  departureTime: Element

  constructor(private readonly page: Page) {
    this.editButton = this.page.findByDataQa('edit')
    this.arrivalTime = this.page.findByDataQa('arrival-time')
    this.departureTime = this.page.findByDataQa('departure-time')
  }

  #tabs = {
    present: this.page.findByDataQa('present-tab'),
    absent: this.page.findByDataQa('absent-tab')
  }

  #addNewExternalMemberButton = this.page.findByDataQa(
    'add-external-member-btn'
  )
  pinInput = this.page.findByDataQa('pin-input')

  anyArrivalPage = {
    arrivedInput: new TextInput(this.page.findByDataQa('input-arrived')),
    markArrived: this.page.findByDataQa('mark-arrived-btn')
  }
  externalArrivalPage = {
    nameInput: new TextInput(this.page.findByDataQa('input-name')),
    groupSelect: new Combobox(this.page.findByDataQa('input-group'))
  }
  staffArrivalPage = {
    groupSelect: new Select(this.page.findByDataQa('group-select')),
    timeInputWarningText: this.page.findByDataQa('input-arrived-info'),
    arrivalTypeCheckbox: (type: StaffAttendanceType) =>
      new Checkbox(this.page.findByDataQa(`attendance-type-${type}`)),
    arrivalIsBeforeDeparture: this.page.findByDataQa(
      'arrival-before-departure-notification'
    )
  }
  staffDeparturePage = {
    departureTime: new TextInput(this.page.findByDataQa('set-time')),
    markDepartedBtn: this.page.findByDataQa('mark-departed-btn'),
    timeInputWarningText: this.page.findByDataQa('set-time-info'),
    departureTypeCheckbox: (type: StaffAttendanceType) =>
      new Checkbox(this.page.findByDataQa(`attendance-type-${type}`)),
    departureIsBeforeArrival: this.page.findByDataQa(
      'departure-before-arrival-notification'
    )
  }

  anyMemberPage = {
    back: this.page.findByDataQa('back-btn'),
    status: this.page.findByDataQa('employee-status'),
    markDeparted: this.page.findByDataQa('mark-departed-btn')
  }
  staffMemberPage = {
    attendanceTimes: this.page.findAllByDataQa('attendance-time'),
    markArrivedBtn: this.page.findByDataQa('mark-arrived-btn'),
    shiftTimeText: this.page.findByDataQa('shift-time'),
    attendanceTimeTexts: this.page.findAllByDataQa('attendance-time')
  }
  externalMemberPage = {
    arrivalTime: this.page.findByDataQa('arrival-time'),
    departureTimeInput: new TextInput(
      this.page.findByDataQa('departure-time-input')
    ),
    departureIsBeforeArrival: this.page.findByDataQa(
      'departure-before-arrival-notification'
    )
  }

  #staffLink = (name: string) =>
    this.page.find(`[data-qa="staff-link"]`, { hasText: name })

  async assertShiftTimeTextShown(expectedText: string) {
    await this.staffMemberPage.shiftTimeText.assertTextEquals(expectedText)
  }

  async assertAttendanceTimeTextShown(expectedText: string) {
    await waitUntilEqual(
      () =>
        this.staffMemberPage.attendanceTimeTexts
          .allInnerTexts()
          .then((texts) => texts.join(',')),
      expectedText
    )
  }

  async markNewExternalStaffArrived(
    time: string,
    name: string,
    group: DaycareGroup
  ) {
    await this.#addNewExternalMemberButton.click()

    await this.anyArrivalPage.arrivedInput.fill(time)
    await this.externalArrivalPage.nameInput.type(name)
    await this.externalArrivalPage.groupSelect.fillAndSelectItem(
      group.name,
      group.id
    )

    await this.anyArrivalPage.markArrived.click()
  }

  async assertMarkNewExternalStaffDisabled() {
    await this.#addNewExternalMemberButton.click()
    await this.anyArrivalPage.markArrived.assertDisabled(true)
    await this.anyArrivalPage.arrivedInput.waitUntilHidden()
  }

  async assertPresentStaffCount(expected: number) {
    await this.#tabs.present.assertTextEquals(`LÄSNÄ\n(${expected})`)
  }

  async openStaffPage(name: string) {
    await this.#staffLink(name).click()
  }

  async assertEmployeeStatus(expected: string) {
    await this.anyMemberPage.status.assertTextEquals(expected)
  }

  async selectTab(tab: 'present' | 'absent') {
    await this.#tabs[tab].click()
  }

  async goBackFromMemberPage() {
    await this.anyMemberPage.back.click()
  }

  async assertEmployeeAttendances(expectedArray: string[]) {
    const attendances = this.staffMemberPage.attendanceTimes
    await attendances.assertCount(expectedArray.length)
    return Promise.all(
      expectedArray.map(async (expected, index) => {
        const attendance = attendances.nth(index)
        await attendance.assertTextEquals(expected)
      })
    )
  }

  async assertExternalStaffArrivalTime(expected: string) {
    await this.externalMemberPage.arrivalTime.assertTextEquals(expected)
  }

  async selectAttendanceType(type: StaffAttendanceType) {
    await this.staffArrivalPage.arrivalTypeCheckbox(type).click()
  }

  async assertArrivalTypeCheckboxVisible(type: StaffAttendanceType) {
    await this.staffArrivalPage.arrivalTypeCheckbox(type).waitUntilVisible()
  }

  async markStaffArrived(args: {
    pin: string
    time: string
    group: DaycareGroup
  }) {
    await this.staffMemberPage.markArrivedBtn.click()
    await this.pinInput.locator.type(args.pin)
    await this.anyArrivalPage.arrivedInput.fill(args.time)
    await this.staffArrivalPage.groupSelect.selectOption(args.group.id)
    await this.anyArrivalPage.markArrived.click()
  }

  async assertMarkStaffArrivedDisabled() {
    await this.staffMemberPage.markArrivedBtn.assertDisabled(true)
  }

  async markStaffDeparted(args: { pin: string; time?: string }) {
    await this.anyMemberPage.markDeparted.click()
    await this.pinInput.locator.type(args.pin)
    if (args.time) {
      await this.staffDeparturePage.departureTime.fill(args.time)
    }
    await this.staffDeparturePage.markDepartedBtn.click()
  }

  async clickMarkDepartedButton() {
    await this.staffDeparturePage.markDepartedBtn.click()
  }

  async markExternalStaffDeparted(time?: string) {
    if (time) {
      await this.externalMemberPage.departureTimeInput.fill(time)
    }
    await this.anyMemberPage.markDeparted.click()
  }

  async clickStaffArrivedAndSetPin(pin: string) {
    await this.staffMemberPage.markArrivedBtn.click()
    await this.pinInput.locator.type(pin)
  }

  async clickStaffDepartedAndSetPin(pin: string) {
    await this.anyMemberPage.markDeparted.click()
    await this.pinInput.locator.type(pin)
  }

  async setArrivalTime(time: string) {
    await this.anyArrivalPage.arrivedInput.fill(time)
  }

  async setDepartureTime(time: string) {
    await this.staffDeparturePage.departureTime.fill(time)
  }

  async assertDoneButtonEnabled(enabled: boolean) {
    await waitUntilEqual(
      () => this.anyArrivalPage.markArrived.disabled,
      !enabled
    )
  }

  async clickDoneButton() {
    await this.anyArrivalPage.markArrived.click()
  }

  async assertMarkDepartedButtonEnabled(enabled: boolean) {
    await waitUntilEqual(
      () => this.staffDeparturePage.markDepartedBtn.disabled,
      !enabled
    )
  }

  async assertGroupSelectionVisible(visible: boolean) {
    await waitUntilEqual(
      () => this.staffArrivalPage.groupSelect.visible,
      visible
    )
  }

  async assertArrivalTimeInputInfo(expected: string) {
    await this.staffArrivalPage.timeInputWarningText.assertTextEquals(expected)
  }

  async assertDepartureTimeInputInfo(expected: string) {
    await this.staffDeparturePage.timeInputWarningText.assertTextEquals(
      expected
    )
  }

  async selectGroup(groupId: string) {
    await this.staffArrivalPage.groupSelect.selectOption(groupId)
  }

  async assertDepartureTypeVisible(
    type: StaffAttendanceType,
    visible: boolean
  ) {
    await waitUntilEqual(
      () => this.staffDeparturePage.departureTypeCheckbox(type).visible,
      visible
    )
  }
}

export class StaffAttendanceEditPage {
  private page: Page
  addLink: Element
  pinInput: TextInput
  cancelButton: Element
  saveButton: AsyncButton

  constructor(page: Page) {
    this.page = page
    this.addLink = page.findByDataQa('add')
    this.pinInput = new TextInput(page.findByDataQa('pin-input'))
    this.cancelButton = page.findByDataQa('cancel')
    this.saveButton = new AsyncButton(page.findByDataQa('save'))
  }

  async selectGroup(index: number, groupId: UUID) {
    const select = new Select(this.page.findAllByDataQa('group').nth(index))
    await select.selectOption({ value: groupId })
  }

  async selectType(index: number, type: StaffAttendanceType) {
    const select = new Select(this.page.findAllByDataQa('type').nth(index))
    await select.selectOption({ value: type })
  }

  async fillArrived(index: number, time: string) {
    const field = new TextInput(this.page.findAllByDataQa('arrived').nth(index))
    await field.fill(time)
  }

  async fillDeparted(index: number, time: string) {
    const field = new TextInput(
      this.page.findAllByDataQa('departed').nth(index)
    )
    await field.fill(time)
  }

  async remove(index: number) {
    const icon = this.page.findAllByDataQa('remove').nth(index)
    await icon.click()
  }
}
