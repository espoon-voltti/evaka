// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { waitUntilEqual } from '../../../utils'
import {
  AsyncButton,
  Checkbox,
  DatePicker,
  Element,
  Modal,
  Page,
  Select,
  TextInput
} from '../../../utils/page'

import { UnitCalendarPageBase } from './unit-calendar-page-base'
import { UnitMonthCalendarPage } from './unit-month-calendar-page'

export class UnitWeekCalendarPage extends UnitCalendarPageBase {
  occupancies: UnitOccupanciesSection

  constructor(page: Page) {
    super(page)
    this.occupancies = new UnitOccupanciesSection(
      page.findByDataQa('occupancies')
    )
  }

  async openMonthCalendar(): Promise<UnitMonthCalendarPage> {
    await this.monthModeButton.click()
    return new UnitMonthCalendarPage(this.page)
  }

  staffAttendances = new UnitStaffAttendancesTable(
    this.page,
    this.page.findByDataQa('staff-attendances-table')
  )
  childReservations = new UnitChildReservationsTable(
    this.page,
    this.page.findByDataQa('child-reservations-table')
  )

  async setFilterStartDate(date: LocalDate) {
    await new DatePicker(this.page.findByDataQa('unit-filter-start-date')).fill(
      date.format()
    )
    await this.waitUntilLoaded()
  }

  async selectPeriod(period: '1 day' | '3 months' | '6 months' | '1 year') {
    await this.page
      .find(`[data-qa="unit-filter-period-${period.replace(' ', '-')}"]`)
      .click()
  }

  // eslint-disable-next-line @typescript-eslint/no-redundant-type-constituents
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
  constructor(
    public page: Page,
    element: Element
  ) {
    super(element)
  }

  get allNames(): Promise<string[]> {
    return this.findAllByDataQa('staff-attendance-name').allTexts()
  }

  get rowCount(): Promise<number> {
    return this.find('tbody').findAll('tr').count()
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
    return this.findAllByDataQa('person-count-sum').nth(nth).text
  }

  #attendanceCell = (date: LocalDate, row: number) =>
    this.findByDataQa(`attendance-${date.formatIso()}-${row}`)

  async assertTableRow({
    rowIx = 0,
    hasStaffOccupancyEffect,
    name,
    nth = 0,
    plannedAttendances,
    attendances
  }: {
    rowIx?: number
    hasStaffOccupancyEffect?: boolean
    name?: string
    nth?: number
    timeNth?: number
    plannedAttendances?: [string, string][]
    attendances?: [string, string][]
  }): Promise<void> {
    const row = this.findByDataQa(`attendance-row-${rowIx}`)

    if (name !== undefined) {
      await row.findByDataQa('staff-attendance-name').assertTextEquals(name)
    }

    if (plannedAttendances !== undefined) {
      const plannedAttendanceDay = row
        .findAllByDataQa('planned-attendance-day')
        .nth(nth)
      for (const [i, [arrival, departure]] of plannedAttendances.entries()) {
        await plannedAttendanceDay
          .findAllByDataQa('planned-attendance-start')
          .nth(i)
          .assertTextEquals(arrival)
        await plannedAttendanceDay
          .findAllByDataQa('planned-attendance-end')
          .nth(i)
          .assertTextEquals(departure)
      }
    }
    if (attendances !== undefined) {
      const attendanceDay = row.findAllByDataQa('attendance-day').nth(nth)
      for (const [i, [arrival, departure]] of attendances.entries()) {
        await attendanceDay
          .findAllByDataQa('arrival-time')
          .nth(i)
          .assertTextEquals(arrival)
        await attendanceDay
          .findAllByDataQa('departure-time')
          .nth(i)
          .assertTextEquals(departure)
      }
    }

    if (hasStaffOccupancyEffect !== undefined) {
      if (hasStaffOccupancyEffect) {
        await row
          .findByDataQa('icon-occupancy-coefficient-pos')
          .waitUntilVisible()
      } else {
        await row.findByDataQa('icon-occupancy-coefficient').waitUntilVisible()
      }
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

  async assertTooltip(
    row: number,
    date: LocalDate,
    expectedTooltipText: string
  ): Promise<void> {
    const cell = this.#attendanceCell(date, row)
    await cell.hover()

    await cell
      .findByDataQa('attendance-tooltip')
      .assertTextEquals(expectedTooltipText)
  }

  async assertOpenDetailsVisible(
    row: number,
    date: LocalDate,
    visible: boolean
  ): Promise<void> {
    const cell = this.#attendanceCell(date, row)
    await cell.hover()

    if (visible) {
      await cell.findByDataQa('open-details').waitUntilVisible()
    } else {
      await cell.findByDataQa('open-details').waitUntilHidden()
    }
  }
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
    await this.#elem('minimum', 'confirmed').assertTextEquals(minimum)
    await this.#elem('maximum', 'confirmed').assertTextEquals(maximum)
  }

  async assertPlanned(minimum: string, maximum: string) {
    await this.#elem('minimum', 'planned').assertTextEquals(minimum)
    await this.#elem('maximum', 'planned').assertTextEquals(maximum)
  }
}

export class StaffAttendanceDetailsModal extends Element {
  openAttendanceWarning = this.findByDataQa(`open-attendance-warning`)
  arrivalTimeInputInfo = this.findByDataQa('arrival-time-input-info')

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

  async removeAttendance(row: number) {
    await this.findAllByDataQa('remove-attendance').nth(row).click()
  }

  async addNewAttendance() {
    await this.findByDataQa('new-attendance').click()
  }

  async save() {
    const button = new AsyncButton(this.findByDataQa('save'))
    await button.click()
    await button.waitUntilHidden()
  }

  async close() {
    await this.findByDataQa('close').click()
  }

  async summary() {
    return {
      plan: await this.findByDataQa('staff-attendance-summary-plan').text,
      realized: await this.findByDataQa('staff-attendance-summary-realized')
        .text,
      hours: await this.findByDataQa('staff-attendance-summary-hours').text
    }
  }

  gapWarning(index: number) {
    return this.findByDataQa(`attendance-gap-warning-${index}`).text
  }

  arrivalTimeInfo(index: number) {
    return this.findAllByDataQa('arrival-time-input-info').nth(index).text
  }

  departureTimeInfo(index: number) {
    return this.findAllByDataQa('departure-time-input-info').nth(index).text
  }

  async assertDepartureTimeInfoHidden(index: number) {
    return await this.findAllByDataQa('departure-time-input-info')
      .nth(index)
      .waitUntilHidden()
  }
}

export class StaffAttendanceAddPersonModal extends Element {
  async selectGroup(groupId: UUID) {
    await new Select(this.findByDataQa('add-person-group-select')).selectOption(
      groupId
    )
  }

  async setStaffOccupancyEffect(value: boolean) {
    const isResponsible = new Checkbox(
      this.findByDataQa('has-staff-occupancy-effect')
    )
    if (value) await isResponsible.check()
    else await isResponsible.uncheck()
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
