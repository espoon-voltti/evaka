// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { waitUntilEqual, waitUntilTrue } from '../../../utils'
import {
  AsyncButton,
  DatePicker,
  DatePickerDeprecated,
  Element,
  Modal,
  Page,
  Select,
  SelectionChip,
  TextInput,
  TreeDropdown
} from '../../../utils/page'

export class UnitCalendarPage {
  constructor(private readonly page: Page) {}

  get attendancesSection() {
    return new UnitAttendancesSection(this.page)
  }

  get calendarEventsSection() {
    return new UnitCalendarEventsSection(this.page)
  }

  async selectMode(mode: 'week' | 'month') {
    const selectionChip = new SelectionChip(
      this.page.find(`[data-qa="choose-calendar-mode-${mode}"]`)
    )
    await selectionChip.click()
  }

  async assertMode(expected: 'week' | 'month') {
    const selectionChip = new SelectionChip(
      this.page.find(`[data-qa="choose-calendar-mode-${expected}"]`)
    )
    await waitUntilTrue(() => selectionChip.checked)
  }

  private async getSelectedDateRange() {
    const rawRange = await this.page
      .find('[data-qa-date-range]')
      .getAttribute('data-qa-date-range')

    if (!rawRange) throw Error('Week range cannot be found')

    const [start, end] = rawRange
      .replace(/^\[/, '')
      .replace(/\]$/, '')
      .split(', ')
    return new FiniteDateRange(
      LocalDate.parseIso(start),
      LocalDate.parseIso(end)
    )
  }

  async changeWeekToDate(date: LocalDate) {
    for (let i = 0; i < 50; i++) {
      const currentRange = await this.getSelectedDateRange()
      if (currentRange.includes(date)) return

      await this.page
        .findByDataQa(
          currentRange.start.isBefore(date) ? 'next-week' : 'previous-week'
        )
        .click()
      await this.waitForWeekLoaded()
    }
    throw Error(`Unable to seek to date ${date.formatIso()}`)
  }

  async waitForWeekLoaded() {
    await waitUntilEqual(
      () =>
        this.page
          .findByDataQa('staff-attendances-status')
          .getAttribute('data-qa-value'),
      'success'
    )
  }

  async assertDateRange(expectedRange: FiniteDateRange) {
    await waitUntilEqual(() => this.getSelectedDateRange(), expectedRange)
  }
}

export class UnitCalendarEventsSection {
  constructor(private readonly page: Page) {}

  async openEventCreationModal() {
    await this.page.findByDataQa('create-new-event-btn').click()
    return new EventCreationModal(this.page.findByDataQa('modal'))
  }

  getEventOfDay(date: LocalDate, idx: number) {
    return this.page
      .findByDataQa(`calendar-event-day-${date.formatIso()}`)
      .findAllByDataQa('event')
      .nth(idx)
  }

  async assertNoEventsForDay(date: LocalDate) {
    await this.page
      .findByDataQa(`calendar-event-day-${date.formatIso()}`)
      .findAllByDataQa('event')
      .assertCount(0)
  }

  get eventEditModal() {
    return new EventEditModal(this.page.findByDataQa('modal'))
  }

  get eventDeleteModal() {
    return new EventDeleteModal(
      this.page.findByDataQa('deletion-modal').findByDataQa('modal')
    )
  }
}

export class EventCreationModal extends Modal {
  readonly title = new TextInput(this.findByDataQa('title-input'))
  readonly startDateInput = new TextInput(this.findByDataQa('start-date'))
  readonly endDateInput = new TextInput(this.findByDataQa('end-date'))
  readonly description = new TextInput(this.findByDataQa('description-input'))
  readonly attendees = new TreeDropdown(this.findByDataQa('attendees'))
}

export class EventEditModal extends Modal {
  readonly title = new TextInput(this.findByDataQa('title-input'))
  readonly description = new TextInput(this.findByDataQa('description-input'))

  async submit() {
    await this.findByDataQa('save').click()
  }

  async delete() {
    await this.findByDataQa('delete').click()
  }
}

export class EventDeleteModal extends Modal {}

export class UnitAttendancesSection {
  constructor(private readonly page: Page) {}

  #reservationCell = (date: LocalDate, row: number) =>
    this.page.findByDataQa(`reservation-${date.formatIso()}-${row}`)
  #attendanceCell = (date: LocalDate, row: number) =>
    this.page.findByDataQa(`attendance-${date.formatIso()}-${row}`)

  #ellipsisMenu = (childId: UUID) =>
    this.page.find(`[data-qa="ellipsis-menu-${childId}"]`)
  #editInline = this.page.find('[data-qa="menu-item-edit-row"]')

  occupancies = new UnitOccupanciesSection(
    this.page.find('[data-qa="occupancies"]')
  )
  staffAttendances = new UnitStaffAttendancesTable(
    this.page,
    this.page.findByDataQa('staff-attendances-table')
  )

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-attendances"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async setFilterStartDate(date: LocalDate) {
    await new DatePickerDeprecated(
      this.page.find('[data-qa="unit-filter-start-date"]')
    ).fill(date.format())
    await this.waitUntilLoaded()
  }

  async childRowCount(childId: UUID): Promise<number> {
    return await this.page
      .findAllByDataQa(`reservation-row-child-${childId}`)
      .count()
  }

  async childInOtherUnitCount(childId: UUID): Promise<number> {
    return await this.page
      .findAllByDataQa(`reservation-row-child-${childId}`)
      .findAll('[data-qa="in-other-unit"]')
      .count()
  }

  getReservationStart(date: LocalDate, row: number): Promise<string> {
    return this.#reservationCell(date, row).findByDataQa('reservation-start')
      .innerText
  }

  getReservationEnd(date: LocalDate, row: number): Promise<string> {
    return this.#reservationCell(date, row).findByDataQa('reservation-end')
      .innerText
  }

  getAttendanceStart(date: LocalDate, row: number): Promise<string> {
    return this.#attendanceCell(date, row).findByDataQa('attendance-start')
      .innerText
  }

  getAttendanceEnd(date: LocalDate, row: number): Promise<string> {
    return this.#attendanceCell(date, row).findByDataQa('attendance-end')
      .innerText
  }

  async openInlineEditor(childId: UUID) {
    await this.#ellipsisMenu(childId).click()
    await this.#editInline.click()
  }

  async closeInlineEditor() {
    await this.page.findByDataQa('inline-editor-state-button').click()
  }

  async setReservationTimes(
    date: LocalDate,
    startTime: string,
    endTime: string
  ) {
    const reservations = this.#reservationCell(date, 0)
    await new TextInput(reservations.findByDataQa('input-start-time')).fill(
      startTime
    )
    await new TextInput(reservations.findByDataQa('input-end-time')).fill(
      endTime
    )
    // Click table header to trigger last input's onblur
    await this.page.findAll('thead').first().click()
  }

  async setAttendanceTimes(
    date: LocalDate,
    startTime: string,
    endTime: string
  ) {
    const attendances = this.#attendanceCell(date, 0)
    await new TextInput(attendances.findByDataQa('input-start-time')).fill(
      startTime
    )
    await new TextInput(attendances.findByDataQa('input-end-time')).fill(
      endTime
    )
    // Click table header to trigger last input's onblur
    await this.page.findAll('thead').first().click()
  }

  async openReservationModal(childId: UUID): Promise<ReservationModal> {
    await this.#ellipsisMenu(childId).click()
    await this.page.find(`[data-qa="menu-item-reservation-modal"]`).click()

    return new ReservationModal(this.page.find('[data-qa="modal"]'))
  }

  async selectPeriod(period: '1 day' | '3 months' | '6 months' | '1 year') {
    await this.page
      .find(`[data-qa="unit-filter-period-${period.replace(' ', '-')}"]`)
      .click()
  }

  async selectGroup(groupId: UUID | 'no-group' | 'staff'): Promise<void> {
    const select = new Select(
      this.page.findByDataQa('attendances-group-select')
    )
    await select.selectOption(groupId)
  }

  async clickAddPersonButton(): Promise<StaffAttendanceAddPersonModal> {
    await this.page.findByDataQa('add-person-button').click()

    return new StaffAttendanceAddPersonModal(
      this.page.findByDataQa('staff-attendance-add-person-modal')
    )
  }
}

export class UnitStaffAttendancesTable extends Element {
  constructor(public page: Page, element: Element) {
    super(element)
  }

  async allStaff(): Promise<string[]> {
    return this.findAllByDataQa('staff-attendance-name').allInnerTexts()
  }

  async assertPositiveOccupancyCoefficientCount(
    expectedCount: number
  ): Promise<void> {
    const icons = this.findAllByDataQa('icon-occupancy-coefficient-pos')
    await waitUntilEqual(() => icons.count(), expectedCount)
  }

  async assertZeroOccupancyCoefficientCount(
    expectedCount: number
  ): Promise<void> {
    const icons = this.findAllByDataQa('icon-occupancy-coefficient')
    await waitUntilEqual(() => icons.count(), expectedCount)
  }

  personCountSum(nth: number) {
    return this.findAllByDataQa('person-count-sum').nth(nth).innerText
  }

  #attendanceCell = (date: LocalDate, row: number) =>
    this.findByDataQa(`attendance-${date.formatIso()}-${row}`)

  #getPlannedAttendanceStart(date: LocalDate, row: number): Promise<string> {
    return this.#attendanceCell(date, row).findByDataQa(
      'planned-attendance-start'
    ).innerText
  }

  #getPlannedAttendanceEnd(date: LocalDate, row: number): Promise<string> {
    return this.#attendanceCell(date, row).findByDataQa(
      'planned-attendance-end'
    ).innerText
  }

  async assertPlannedAttendance(
    date: LocalDate,
    row: number,
    startTime: string,
    endTime: string
  ) {
    await waitUntilEqual(
      () => this.#getPlannedAttendanceStart(date, row),
      startTime
    )
    await waitUntilEqual(
      () => this.#getPlannedAttendanceEnd(date, row),
      endTime
    )
  }

  async assertTableRow({
    rowIx = 0,
    name,
    nth = 0,
    timeNth = 0,
    arrival,
    departure
  }: {
    rowIx?: number
    name?: string
    nth?: number
    timeNth?: number
    arrival?: string
    departure?: string
  }): Promise<void> {
    const row = this.findByDataQa(`attendance-row-${rowIx}`)
    const day = row.findAllByDataQa('attendance-day').nth(nth)

    if (name !== undefined) {
      await waitUntilEqual(
        () => row.findByDataQa('staff-attendance-name').innerText,
        name
      )
    }
    if (arrival !== undefined) {
      await waitUntilEqual(
        () => day.findAllByDataQa('arrival-time').nth(timeNth ?? 0).innerText,
        arrival
      )
    }
    if (departure !== undefined) {
      await waitUntilEqual(
        () => day.findAllByDataQa('departure-time').nth(timeNth).innerText,
        departure
      )
    }
  }

  async openDetails(
    row: number,
    date: LocalDate
  ): Promise<StaffAttendanceDetailsModal> {
    const cell = this.#attendanceCell(date, row)
    await cell.hover()
    await cell.findByDataQa('open-details').click()

    return new StaffAttendanceDetailsModal(
      this.page.findByDataQa('staff-attendance-details-modal')
    )
  }
}

export class ReservationModal extends Modal {
  #repetitionSelect = new Select(this.find('[data-qa="repetition"]'))
  #startDate = new TextInput(this.find('[data-qa="reservation-start-date"]'))
  #endDate = new TextInput(this.find('[data-qa="reservation-end-date"]'))

  async selectRepetitionType(value: 'DAILY' | 'WEEKLY' | 'IRREGULAR') {
    await this.#repetitionSelect.selectOption(value)
  }

  async setStartDate(date: string) {
    await this.#startDate.fill(date)
  }

  async setEndDate(date: string) {
    await this.#endDate.fill(date)
  }

  async setStartTime(time: string, index: number) {
    await new TextInput(
      this.findAll('[data-qa="reservation-start-time"]').nth(index)
    ).fill(time)
  }

  async setEndTime(time: string, index: number) {
    await new TextInput(
      this.findAll('[data-qa="reservation-end-time"]').nth(index)
    ).fill(time)
  }

  async addNewTimeRow(index: number) {
    await this.findAll(`[data-qa="add-new-reservation-timerange"]`)
      .nth(index)
      .click()
  }

  async save() {
    await this.submit()
  }

  async addReservation(endDate: LocalDate) {
    await this.selectRepetitionType('IRREGULAR')
    await this.setEndDate(endDate.format())
    await this.setStartTime('10:00', 0)
    await this.setEndTime('16:00', 0)
    await this.save()
  }
}

export class UnitOccupanciesSection extends Element {
  #graph = this.find('canvas')
  #noDataPlaceholder = this.findByDataQa('no-data-placeholder')

  #elem = (
    which: 'minimum' | 'maximum' | 'no-valid-values',
    type: 'confirmed' | 'planned'
  ) => this.find(`[data-qa="occupancies-${which}-${type}"]`)

  async assertGraphIsVisible() {
    await this.#graph.waitUntilVisible()
  }

  async assertGraphHasNoData() {
    await this.#noDataPlaceholder.waitUntilVisible()
  }

  async assertNoValidValues() {
    await this.#elem('no-valid-values', 'confirmed').waitUntilVisible()
    await this.#elem('no-valid-values', 'planned').waitUntilVisible()
  }

  async assertConfirmed(minimum: string, maximum: string) {
    await waitUntilEqual(
      () => this.#elem('minimum', 'confirmed').innerText,
      minimum
    )
    await waitUntilEqual(
      () => this.#elem('maximum', 'confirmed').innerText,
      maximum
    )
  }

  async assertPlanned(minimum: string, maximum: string) {
    await waitUntilEqual(
      () => this.#elem('minimum', 'planned').innerText,
      minimum
    )
    await waitUntilEqual(
      () => this.#elem('maximum', 'planned').innerText,
      maximum
    )
  }
}

export class StaffAttendanceDetailsModal extends Element {
  async setGroup(row: number, groupId: UUID) {
    await new Select(
      this.findAllByDataQa('group-indicator')
        .nth(row)
        .findByDataQa('attendance-group-select')
    ).selectOption(groupId)
  }

  async setType(row: number, type: StaffAttendanceType) {
    await new Select(
      this.findAllByDataQa('attendance-type-select').nth(row)
    ).selectOption(type)
  }

  async setArrivalTime(row: number, time: string) {
    await new TextInput(
      this.findAllByDataQa('arrival-time-input').nth(row)
    ).fill(time)
  }

  async setDepartureTime(row: number, time: string) {
    await new TextInput(
      this.findAllByDataQa('departure-time-input').nth(row)
    ).fill(time)
  }

  async addNewAttendance() {
    await this.findByDataQa('new-attendance').click()
  }

  async save() {
    const button = new AsyncButton(this.findByDataQa('save'))
    await button.click()
    await button.waitUntilIdle()
  }

  async close() {
    await this.findByDataQa('close').click()
  }

  async summary() {
    return {
      plan: await this.findByDataQa('staff-attendance-summary-plan').innerText,
      realized: await this.findByDataQa('staff-attendance-summary-realized')
        .innerText,
      hours: await this.findByDataQa('staff-attendance-summary-hours').innerText
    }
  }

  gapWarning(index: number) {
    return this.findByDataQa(`attendance-gap-warning-${index}`).innerText
  }
}

export class StaffAttendanceAddPersonModal extends Element {
  async selectGroup(groupId: UUID) {
    await new Select(this.findByDataQa('add-person-group-select')).selectOption(
      groupId
    )
  }

  async setArrivalDate(date: string) {
    await new DatePicker(
      this.findByDataQa('add-person-arrival-date-picker')
    ).fill(date)
  }

  async setArrivalTime(time: string) {
    await new TextInput(
      this.findByDataQa('add-person-arrival-time-input')
    ).fill(time)
  }

  async setDepartureTime(time: string) {
    await new TextInput(
      this.findByDataQa('add-person-departure-time-input')
    ).fill(time)
  }

  async typeName(name: string) {
    await new TextInput(this.findByDataQa('add-person-name-input')).fill(name)
  }

  async save() {
    await this.findByDataQa('add-person-save-btn').click()
  }

  async cancel() {
    await this.findByDataQa('add-person-cancel-btn').click()
  }

  async timeErrorVisible() {
    await this.findByDataQa(
      'add-person-arrival-time-input-info'
    ).waitUntilVisible()
  }

  async nameErrorVisible() {
    await this.findByDataQa('add-person-name-input-info').waitUntilVisible()
  }
}
