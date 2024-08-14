// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { Element, Modal, Page, Select, TextInput } from '../../../utils/page'

import { UnitCalendarPageBase } from './unit-calendar-page-base'
import { UnitMonthCalendarPage } from './unit-month-calendar-page'

export class UnitWeekCalendarPage extends UnitCalendarPageBase {
  async openMonthCalendar(): Promise<UnitMonthCalendarPage> {
    await this.monthModeButton.click()
    return new UnitMonthCalendarPage(this.page)
  }

  childReservations = new UnitChildReservationsTable(
    this.page,
    this.page.findByDataQa('child-reservations-table')
  )
}

export class UnitChildReservationsTable extends Element {
  constructor(
    public page: Page,
    element: Element
  ) {
    super(element)
  }

  #reservationCell = (date: LocalDate, row: number) =>
    this.findByDataQa(`reservation-${date.formatIso()}-${row}`)
  #attendanceCell = (date: LocalDate, row: number) =>
    this.findByDataQa(`attendance-${date.formatIso()}-${row}`)

  #ellipsisMenu = (childId: UUID) =>
    this.findByDataQa(`ellipsis-menu-${childId}`)

  childReservationRows(childId: UUID) {
    return this.findAllByDataQa(`reservation-row-child-${childId}`)
  }

  childAttendanceRows(childId: UUID) {
    return this.findAllByDataQa(`attendance-row-child-${childId}`)
  }

  reservationCells = (childId: UUID, date: LocalDate) =>
    this.childReservationRows(childId).findAllByDataQa(`td-${date.formatIso()}`)

  attendanceCells = (childId: UUID, date: LocalDate) =>
    this.childAttendanceRows(childId).findAllByDataQa(`td-${date.formatIso()}`)

  childAbsenceCells(childId: UUID) {
    return this.findAllByDataQa(
      `reservation-row-child-${childId}`
    ).findAllByDataQa('absence')
  }

  outsideOpeningHoursWarning = (childId: UUID, date: LocalDate, n: number) =>
    this.reservationCells(childId, date)
      .nth(n)
      .findByDataQa('reservation-end')
      .findByDataQa('outside-opening-times')

  childInOtherUnit(childId: UUID) {
    return this.findAllByDataQa(
      `reservation-row-child-${childId}`
    ).findAllByDataQa('in-other-unit')
  }

  childInOtherGroup(childId: UUID) {
    return this.findAllByDataQa(
      `reservation-row-child-${childId}`
    ).findAllByDataQa('in-other-group')
  }

  missingReservations(childId: UUID) {
    return this.findAllByDataQa(
      `reservation-row-child-${childId}`
    ).findAllByDataQa('reservation-missing')
  }

  missingHolidayReservations(childId: UUID) {
    return this.findAllByDataQa(
      `reservation-row-child-${childId}`
    ).findAllByDataQa('holiday-reservation-missing')
  }

  getReservation(date: LocalDate, row: number): Promise<[string, string]> {
    const cell = this.#reservationCell(date, row)
    return Promise.all([
      cell.findByDataQa('reservation-start').text,
      cell.findByDataQa('reservation-end').text
    ])
  }

  getAttendance(date: LocalDate, row: number): Promise<[string, string]> {
    const cell = this.#attendanceCell(date, row)
    return Promise.all([
      cell.findByDataQa('attendance-start').text,
      cell.findByDataQa('attendance-end').text
    ])
  }

  getBackupCareRequiredWarning(date: LocalDate, row: number): Promise<string> {
    const cell = this.#attendanceCell(date, row)
    return cell.findByDataQa('backup-care-required-warning').text
  }

  async openReservationModal(childId: UUID): Promise<ReservationModal> {
    await this.#ellipsisMenu(childId).click()
    await this.findByDataQa('menu-item-reservation-modal').click()

    return new ReservationModal(this.page.findByDataQa('modal'))
  }

  async assertCannotOpenChildDateModal(
    childId: UUID,
    date: LocalDate
  ): Promise<void> {
    const cell = this.reservationCells(childId, date).nth(0)
    await cell.hover()
    await cell.findByDataQa('open-details').waitUntilHidden()
  }

  async openChildDateModal(
    childId: UUID,
    date: LocalDate
  ): Promise<ChildDatePresenceModal> {
    const cell = this.reservationCells(childId, date).nth(0)
    await cell.hover()
    const editBtn = cell.findByDataQa('open-details')
    await editBtn.click()

    return new ChildDatePresenceModal(this.page.findByDataQa('modal'))
  }
}

export class ChildDatePresenceModal extends Modal {
  addReservationBtn = this.findByDataQa('add-reservation')
  #reservation = (n: number) => this.findByDataQa(`reservation-${n}`)
  reservationStart = (n: number) =>
    new TextInput(this.#reservation(n).findByDataQa('start'))
  reservationEnd = (n: number) =>
    new TextInput(this.#reservation(n).findByDataQa('end'))
  reservationRemove = (n: number) =>
    this.#reservation(n).findByDataQa('remove-btn')

  addAttendanceBtn = this.findByDataQa('add-attendance')
  #attendance = (n: number) => this.findByDataQa(`attendance-${n}`)
  attendanceStart = (n: number) =>
    new TextInput(this.#attendance(n).findByDataQa('start'))
  attendanceEnd = (n: number) =>
    new TextInput(this.#attendance(n).findByDataQa('end'))
  attendanceRemove = (n: number) =>
    this.#attendance(n).findByDataQa('remove-btn')

  addBillableAbsenceBtn = this.findByDataQa('add-billable-absence')
  billableAbsenceType = new Select(
    this.findByDataQa('billable-absence').findByDataQa('type-select')
  )
  billableAbsenceRemove =
    this.findByDataQa('billable-absence').findByDataQa('remove-btn')

  addNonbillableAbsenceBtn = this.findByDataQa('add-nonbillable-absence')
  nonbillableAbsenceType = new Select(
    this.findByDataQa('nonbillable-absence').findByDataQa('type-select')
  )
  nonbillableAbsenceRemove = this.findByDataQa(
    'nonbillable-absence'
  ).findByDataQa('remove-btn')

  #absenceWarnings = this.findByDataQa('absence-warnings')
  #warningMissingNonbillableAbsence = this.findByDataQa(
    'missing-nonbillable-absence'
  )
  #warningExtraNonbillableAbsence = this.findByDataQa(
    'extra-nonbillable-absence'
  )
  #warningMissingBillableAbsence = this.findByDataQa('missing-billable-absence')
  #warningExtraBillableAbsence = this.findByDataQa('extra-billable-absence')

  async assertWarnings(
    expectedWarnings: (
      | 'missing-nonbillable-absence'
      | 'extra-nonbillable-absence'
      | 'missing-billable-absence'
      | 'extra-billable-absence'
    )[]
  ) {
    await this.#absenceWarnings.waitUntilAttached()
    if (expectedWarnings.includes('missing-nonbillable-absence')) {
      await this.#warningMissingNonbillableAbsence.waitUntilVisible()
    } else {
      await this.#warningMissingNonbillableAbsence.waitUntilHidden()
    }
    if (expectedWarnings.includes('extra-nonbillable-absence')) {
      await this.#warningExtraNonbillableAbsence.waitUntilVisible()
    } else {
      await this.#warningExtraNonbillableAbsence.waitUntilHidden()
    }
    if (expectedWarnings.includes('missing-billable-absence')) {
      await this.#warningMissingBillableAbsence.waitUntilVisible()
    } else {
      await this.#warningMissingBillableAbsence.waitUntilHidden()
    }
    if (expectedWarnings.includes('extra-billable-absence')) {
      await this.#warningExtraBillableAbsence.waitUntilVisible()
    } else {
      await this.#warningExtraBillableAbsence.waitUntilHidden()
    }
  }
}

export class ReservationModal extends Modal {
  #repetitionSelect = new Select(this.find('[data-qa="repetition"]'))
  startDate = new TextInput(
    this.findByDataQa('reservation-date-range').findByDataQa('start-date')
  )
  endDate = new TextInput(
    this.findByDataQa('reservation-date-range').findByDataQa('end-date')
  )

  async selectRepetitionType(value: 'DAILY' | 'WEEKLY' | 'IRREGULAR') {
    await this.#repetitionSelect.selectOption(value)
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

  async setAbsent() {
    const absentButton = this.find(`[data-qa="set-absent-button"]`)
    await absentButton.click()
  }

  async addReservation(endDate: LocalDate) {
    await this.selectRepetitionType('IRREGULAR')
    await this.endDate.fill(endDate.format())
    await this.setStartTime('10:00', 0)
    await this.setEndTime('16:00', 0)
    await this.save()
  }

  async addAbsence(date: LocalDate) {
    await this.selectRepetitionType('IRREGULAR')
    await this.endDate.fill(date.format())
    // dismiss datepicker
    await this.endDate.press('Escape')
    await this.setAbsent()
    await this.save()
  }
}
