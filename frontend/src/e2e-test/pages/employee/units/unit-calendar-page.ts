// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import {
  Modal,
  Page,
  Select,
  SelectionChip,
  TextInput
} from '../../../utils/page'

export class UnitCalendarPage {
  constructor(private readonly page: Page) {}

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-groups-page"][data-loading="false"]')
      .waitUntilVisible()
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

  async attendanceCell(
    date: LocalDate,
    row: number,
    col: number
  ): Promise<string> {
    return this.page.find(
      `[data-qa="attendance-${date.formatIso()}-${row}-${col}"]`
    ).innerText
  }

  async reservationCell(
    date: LocalDate,
    row: number,
    col: number
  ): Promise<string> {
    return this.page.find(
      `[data-qa="reservation-${date.formatIso()}-${row}-${col}"]`
    ).innerText
  }

  async openReservationModal(childId: UUID): Promise<ReservationModal> {
    await this.page.find(`[data-qa="ellipsis-menu-${childId}"]`).click()
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
