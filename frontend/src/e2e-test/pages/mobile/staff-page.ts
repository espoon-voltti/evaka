// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { DevDaycareGroup } from '../../generated/api-types'
import { waitUntilEqual } from '../../utils'
import {
  AsyncButton,
  Checkbox,
  Combobox,
  Element,
  ElementCollection,
  Page,
  PinInput,
  Select,
  TextInput
} from '../../utils/page'

export default class StaffPage {
  #staffCount: Element
  #staffOtherCount: Element
  #cancelButton: Element
  #confirmButton: Element
  #occupancyRealized: Element
  #updated: Element
  constructor(readonly page: Page) {
    this.#staffCount = page.findByDataQa('staff-count')
    this.#staffOtherCount = page.findByDataQa('staff-other-count')
    this.#cancelButton = page.findByDataQa('cancel-button')
    this.#confirmButton = page.findByDataQa('confirm-button')
    this.#occupancyRealized = page.findByDataQa('realized-occupancy')
    this.#updated = page.findByDataQa('updated')
  }

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
  previousAttendancesButton: Element
  arrivalTime: Element
  departureTime: Element

  #addNewExternalMemberButton: Element
  #tabs: { present: Element; absent: Element }
  pinInput: Element

  previousAttendancesPage: {
    attendanceOfDate: (
      date: LocalDate,
      i: number
    ) => {
      times: Element
      groupOrType: Element
    }
    editAttendancesOfDateButton: (date: LocalDate) => Element
  }
  anyArrivalPage: {
    arrivedInput: TextInput
    markArrived: Element
  }
  externalArrivalPage: {
    nameInput: TextInput
    groupSelect: Combobox
  }
  staffArrivalPage: {
    groupSelect: Select
    occupancyEffectCheckbox: Checkbox
    timeInputWarningText: Element
    arrivalTypeCheckbox: (type: StaffAttendanceType) => Checkbox
    arrivalIsBeforeDeparture: Element
  }
  staffDeparturePage: {
    departureTime: TextInput
    markDepartedBtn: Element
    timeInputWarningText: Element
    departureTypeCheckbox: (type: StaffAttendanceType) => Checkbox
    departureIsBeforeArrival: Element
  }
  anyMemberPage: {
    back: Element
    status: Element
    markDeparted: Element
  }
  staffMemberPage: {
    markArrivedBtn: Element
    shiftTimeText: Element
    attendanceTimeTexts: ElementCollection
    attendanceTimes: ElementCollection
  }
  externalMemberPage: {
    arrivalTime: Element
    departureTimeInput: TextInput
    departureIsBeforeArrival: Element
  }

  constructor(private readonly page: Page) {
    this.editButton = this.page.findByDataQa('edit')
    this.previousAttendancesButton = this.page.findByDataQa(
      'previous-attendances'
    )
    this.arrivalTime = this.page.findByDataQa('arrival-time')
    this.departureTime = this.page.findByDataQa('departure-time')
    this.#addNewExternalMemberButton = page.findByDataQa(
      'add-external-member-btn'
    )
    this.#tabs = {
      present: page.findByDataQa('present-tab'),
      absent: page.findByDataQa('absent-tab')
    }

    this.pinInput = page.findByDataQa('pin-input')
    this.previousAttendancesPage = {
      attendanceOfDate: (date: LocalDate, i: number) => ({
        times: this.page
          .findByDataQa(`previous-attendances-${date.formatIso()}`)
          .findAllByDataQa('attendance')
          .nth(i)
          .findByDataQa('times'),
        groupOrType: this.page
          .findByDataQa(`previous-attendances-${date.formatIso()}`)
          .findAllByDataQa('attendance')
          .nth(i)
          .findByDataQa('group-or-type')
      }),
      editAttendancesOfDateButton: (date) =>
        this.page
          .findByDataQa(`previous-attendances-${date.formatIso()}`)
          .findByDataQa('edit')
    }
    this.anyArrivalPage = {
      arrivedInput: new TextInput(page.findByDataQa('input-arrived')),
      markArrived: page.findByDataQa('mark-arrived-btn')
    }
    this.externalArrivalPage = {
      nameInput: new TextInput(page.findByDataQa('input-name')),
      groupSelect: new Combobox(page.findByDataQa('input-group'))
    }
    this.staffArrivalPage = {
      groupSelect: new Select(page.findByDataQa('group-select')),
      occupancyEffectCheckbox: new Checkbox(
        page.findByDataQa('has-occupancy-effect')
      ),
      timeInputWarningText: page.findByDataQa('input-arrived-info'),
      arrivalTypeCheckbox: (type: StaffAttendanceType) =>
        new Checkbox(page.findByDataQa(`attendance-type-${type}`)),
      arrivalIsBeforeDeparture: page.findByDataQa(
        'arrival-before-departure-notification'
      )
    }
    this.staffDeparturePage = {
      departureTime: new TextInput(page.findByDataQa('set-time')),
      markDepartedBtn: page.findByDataQa('mark-departed-btn'),
      timeInputWarningText: page.findByDataQa('set-time-info'),
      departureTypeCheckbox: (type: StaffAttendanceType) =>
        new Checkbox(page.findByDataQa(`attendance-type-${type}`)),
      departureIsBeforeArrival: page.findByDataQa(
        'departure-before-arrival-notification'
      )
    }

    this.anyMemberPage = {
      back: page.findByDataQa('back-btn'),
      status: page.findByDataQa('employee-status'),
      markDeparted: page.findByDataQa('mark-departed-btn')
    }
    this.staffMemberPage = {
      attendanceTimes: page.findAllByDataQa('attendance-time'),
      markArrivedBtn: page.findByDataQa('mark-arrived-btn'),
      shiftTimeText: page.findByDataQa('shift-time'),
      attendanceTimeTexts: page.findAllByDataQa('attendance-time')
    }
    this.externalMemberPage = {
      arrivalTime: page.findByDataQa('arrival-time'),
      departureTimeInput: new TextInput(
        page.findByDataQa('departure-time-input')
      ),
      departureIsBeforeArrival: page.findByDataQa(
        'departure-before-arrival-notification'
      )
    }
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
          .allTexts()
          .then((texts) => texts.join(',')),
      expectedText
    )
  }

  async markNewExternalStaffArrived(
    time: string,
    name: string,
    group: DevDaycareGroup
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
    group: DevDaycareGroup
    hasOccupancyEffect?: boolean
  }) {
    await this.staffMemberPage.markArrivedBtn.click()
    await this.pinInput.locator.type(args.pin)
    await this.anyArrivalPage.arrivedInput.fill(args.time)
    await this.staffArrivalPage.groupSelect.selectOption(args.group.id)
    if (args.hasOccupancyEffect === true) {
      await this.staffArrivalPage.occupancyEffectCheckbox.check()
    } else if (args.hasOccupancyEffect === false) {
      await this.staffArrivalPage.occupancyEffectCheckbox.uncheck()
    }
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
  cancelButton: Element
  occupancyEffect: Checkbox

  constructor(page: Page) {
    this.page = page
    this.addLink = page.findByDataQa('add')
    this.cancelButton = page.findByDataQa('cancel')
    this.occupancyEffect = new Checkbox(
      this.page.findByDataQa('occupancy-effect')
    )
  }

  async selectGroup(index: number, groupId: UUID) {
    const editor = new Element(this.page.findAllByDataQa('group').nth(index))
    const name = editor.findByDataQa('group-name')
    try {
      await name.click()
    } catch (e) {
      // already clicked
    }
    const select = new Select(editor.findByDataQa('group-selector'))
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

  async submit(pinCode: string) {
    await this.page.findByDataQa('save').click()
    await new PinInput(this.page.findByDataQa('pin-input')).fill(pinCode)
    await new AsyncButton(this.page.findByDataQa('confirm')).click()
  }
}
