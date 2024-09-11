// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { waitUntilEqual } from '../../../utils'
import {
  Page,
  Element,
  DatePicker,
  Select,
  TextInput,
  AsyncButton,
  Checkbox
} from '../../../utils/page'

import { UnitCalendarEventsSection } from './unit'

/** Common elements and actions for both month and week calendar pages */
export class UnitCalendarPageBase {
  occupancies: UnitOccupanciesSection
  monthModeButton: Element
  weekModeButton: Element
  nextWeekButton: Element
  previousWeekButton: Element
  staffAttendances: UnitStaffAttendancesTable
  calendarEventsSection: UnitCalendarEventsSection

  constructor(protected readonly page: Page) {
    this.occupancies = new UnitOccupanciesSection(
      page.findByDataQa('occupancies')
    )
    this.monthModeButton = page.findByDataQa('choose-calendar-mode-month')
    this.weekModeButton = page.findByDataQa('choose-calendar-mode-week')
    this.nextWeekButton = page.findByDataQa('next-week')
    this.previousWeekButton = page.findByDataQa('previous-week')
    this.staffAttendances = new UnitStaffAttendancesTable(
      page,
      page.findByDataQa('staff-attendances-table')
    )
    this.calendarEventsSection = new UnitCalendarEventsSection(page)
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-attendances"][data-isloading="false"]')
      .waitUntilVisible()
  }

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

      await (
        currentRange.start.isBefore(date)
          ? this.nextWeekButton
          : this.previousWeekButton
      ).click()
      await this.waitForWeekLoaded()
    }
    throw Error(`Unable to seek to date ${date.formatIso()}`)
  }

  async waitForWeekLoaded() {
    await this.page
      .find('[data-qa="staff-attendances-status"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async assertDateRange(expectedRange: FiniteDateRange) {
    await waitUntilEqual(() => this.getSelectedDateRange(), expectedRange)
  }

  async clickAddPersonButton(): Promise<StaffAttendanceAddPersonModal> {
    await this.page.findByDataQa('add-person-button').click()

    return new StaffAttendanceAddPersonModal(
      this.page.findByDataQa('staff-attendance-add-person-modal')
    )
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

export class StaffAttendanceDetailsModal extends Element {
  openAttendanceWarning = this.findByDataQa(`open-attendance-warning`)
  openAttendanceInAnotherUnitWarning = this.findByDataQa(
    'open-attendance-in-another-unit-warning'
  )
  arrivalTimeInputInfo = this.findByDataQa('arrival-time-input-info')
  continuationAttendance = this.findByDataQa('continuation-attendance')
  newAttendanceButton = this.findByDataQa('new-attendance')

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
    await this.newAttendanceButton.click()
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
