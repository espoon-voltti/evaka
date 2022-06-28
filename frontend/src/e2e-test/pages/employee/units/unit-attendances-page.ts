// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { waitUntilEqual } from '../../../utils'
import {
  DatePickerDeprecated,
  Element,
  Modal,
  Page,
  Select,
  SelectionChip,
  TextInput
} from '../../../utils/page'

export class UnitAttendancesPage {
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

  async selectMode(mode: 'week' | 'month') {
    const foo = new SelectionChip(
      this.page.find(`[data-qa="choose-calendar-mode-${mode}"]`)
    )
    await foo.click()
  }

  async childRowCount(childId: UUID): Promise<number> {
    return await this.page
      .findAll(`[data-qa="reservation-row-child-${childId}"]`)
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

  async staffInAttendanceTable(): Promise<string[]> {
    return this.page.findAllByDataQa('staff-attendance-name').allInnerTexts()
  }

  async assertPositiveOccupancyCoefficientCount(
    expectedCount: number
  ): Promise<void> {
    const icons = this.page.findAllByDataQa('icon-occupancy-coefficient-pos')
    await waitUntilEqual(() => icons.count(), expectedCount)
  }
  async assertZeroOccupancyCoefficientCount(
    expectedCount: number
  ): Promise<void> {
    const icons = this.page.findAllByDataQa('icon-occupancy-coefficient')
    await waitUntilEqual(() => icons.count(), expectedCount)
  }

  async clickEditOnRow(rowIx: number): Promise<void> {
    await this.page
      .findByDataQa(`attendance-row-${rowIx}`)
      .findByDataQa('row-menu')
      .click()
    await this.page.findByDataQa('menu-item-edit-row').click()
  }

  async clickCommitOnRow(rowIx: number): Promise<void> {
    await this.page
      .findByDataQa(`attendance-row-${rowIx}`)
      .findByDataQa('inline-editor-state-button')
      .click()
  }

  async assertNoTimeInputsVisible(): Promise<void> {
    const startInputs = this.page.findAllByDataQa('input-start-time')
    const endInputs = this.page.findAllByDataQa('input-end-time')
    await waitUntilEqual(() => startInputs.count(), 0)
    await waitUntilEqual(() => endInputs.count(), 0)
  }

  async assertCountTimeInputsVisible(count: number): Promise<void> {
    const startInputs = this.page.findAllByDataQa('input-start-time')
    const endInputs = this.page.findAllByDataQa('input-end-time')
    await waitUntilEqual(() => startInputs.count(), count)
    await waitUntilEqual(() => endInputs.count(), count)
  }

  async setNthStartTime(nth: number, time: string): Promise<void> {
    const input = new TextInput(
      this.page.findAllByDataQa('input-start-time').nth(nth)
    )
    await input.fill(time)
  }

  async setNthArrivalDeparture(
    nth: number,
    arrival: string,
    departure: string
  ): Promise<void> {
    await new TextInput(
      this.page.findAllByDataQa('input-start-time').nth(nth)
    ).fill(arrival)
    await new TextInput(
      this.page.findAllByDataQa('input-end-time').nth(nth)
    ).fill(departure)
  }

  async assertArrivalDeparture({
    rowIx,
    nth,
    arrival,
    departure
  }: {
    rowIx: number
    nth: number
    arrival: string
    departure: string
  }): Promise<void> {
    await waitUntilEqual(
      () =>
        this.page
          .findByDataQa(`attendance-row-${rowIx}`)
          .findAllByDataQa('arrival-time')
          .nth(nth).innerText,
      arrival
    )
    await waitUntilEqual(
      () =>
        this.page
          .findByDataQa(`attendance-row-${rowIx}`)
          .findAllByDataQa('departure-time')
          .nth(nth).innerText,
      departure
    )
  }

  async navigateToPreviousWeek() {
    await this.page.findByDataQa('previous-week').click()
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

  async addOvernightReservation() {
    await this.selectRepetitionType('IRREGULAR')
    await this.setEndDate(LocalDate.todayInSystemTz().addDays(1).format())
    await this.setStartTime('22:00', 0)
    await this.setEndTime('23:59', 0)
    await this.setStartTime('00:00', 1)
    await this.setEndTime('08:00', 1)
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
