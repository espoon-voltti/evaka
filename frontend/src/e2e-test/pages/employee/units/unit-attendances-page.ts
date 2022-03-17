// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { waitUntilEqual } from '../../../utils'
import {
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
    await this.page.find('thead').click()
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
    await this.page.find('thead').click()
  }

  async openReservationModal(childId: UUID): Promise<ReservationModal> {
    await this.#ellipsisMenu(childId).click()
    await this.page.find(`[data-qa="menu-item-reservation-modal"]`).click()

    return new ReservationModal(this.page.find('[data-qa="modal"]'))
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

  async addReservation() {
    await this.selectRepetitionType('IRREGULAR')
    await this.setEndDate(LocalDate.today().format())
    await this.setStartTime('10:00', 0)
    await this.setEndTime('16:00', 0)
    await this.save()
  }

  async addOvernightReservation() {
    await this.selectRepetitionType('IRREGULAR')
    await this.setEndDate(LocalDate.today().addDays(1).format())
    await this.setStartTime('22:00', 0)
    await this.setEndTime('23:59', 0)
    await this.setStartTime('00:00', 1)
    await this.setEndTime('08:00', 1)
    await this.save()
  }
}

export class UnitOccupanciesSection extends Element {
  #elem = (
    which: 'minimum' | 'maximum' | 'no-valid-values',
    type: 'confirmed' | 'planned'
  ) => this.find(`[data-qa="occupancies-${which}-${type}"]`)

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
