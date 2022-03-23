// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { waitUntilEqual } from '../../utils'
import { Checkbox, Element, Page, Select, TextInput } from '../../utils/page'

export default class CitizenCalendarPage {
  constructor(
    private readonly page: Page,
    private readonly type: 'desktop' | 'mobile'
  ) {}

  #openCalendarActionsModal = this.page.findByDataQa(
    'open-calendar-actions-modal'
  )
  #dayCell = (date: LocalDate) =>
    this.page.findByDataQa(`${this.type}-calendar-day-${date.formatIso()}`)

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="calendar-page"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async openReservationsModal() {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page.findByDataQa('calendar-action-reservations').click()
    } else {
      await this.page.findByDataQa('open-reservations-modal').click()
    }
    return new ReservationsModal(this.page)
  }

  async openAbsencesModal() {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page.findByDataQa('calendar-action-absences').click()
    } else {
      await this.page.findByDataQa('open-absences-modal').click()
    }
    return new AbsencesModal(this.page)
  }

  async openHolidayModal() {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page.findByDataQa('calendar-action-holidays').click()
    } else {
      await this.page.findByDataQa('open-holiday-modal').click()
    }
    return new HolidayModal(
      this.page.findByDataQa('fixed-period-selection-modal')
    )
  }

  async assertHolidayModalButtonVisible() {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page
        .findByDataQa('calendar-action-holidays')
        .waitUntilVisible()
    } else {
      await this.page.findByDataQa('open-holiday-modal').click()
    }
  }

  async openDayView(date: LocalDate) {
    await this.#dayCell(date).click()
    return new DayView(this.page, this.page.findByDataQa('calendar-dayview'))
  }

  async assertReservations(
    date: LocalDate,
    reservations: { startTime: string; endTime: string }[],
    absence = false,
    freeAbsence = false
  ) {
    await waitUntilEqual(
      () => this.#dayCell(date).findByDataQa('reservations').innerText,
      [
        ...(absence ? ['Poissa'] : []),
        ...(freeAbsence ? ['Maksuton poissaolo'] : []),
        ...reservations.map(
          ({ startTime, endTime }) => `${startTime} – ${endTime}`
        )
      ].join(', ')
    )
  }

  #bannerContainer = this.page.findByDataQa('holiday-period-banner-container')

  async getHolidayBannerContent(): Promise<string> {
    await waitUntilEqual(
      () => this.#bannerContainer.getAttribute('data-status'),
      'success'
    )
    return this.#bannerContainer.findByDataQa('holiday-period-banner').innerText
  }

  async assertHolidayBannerNotVisible(): Promise<void> {
    await waitUntilEqual(
      () => this.#bannerContainer.getAttribute('data-status'),
      'success'
    )
    await waitUntilEqual(() => this.#bannerContainer.findAll('> *').count(), 0)
  }

  async assertNoReservationsOrAbsences(date: LocalDate) {
    await waitUntilEqual(
      () => this.#dayCell(date).findByDataQa('reservations').innerText,
      'Ei varausta'
    )
  }
}

class ReservationsModal {
  constructor(private readonly page: Page) {}

  #startDateInput = new TextInput(this.page.find('[data-qa="start-date"]'))
  #endDateInput = new TextInput(this.page.find('[data-qa="end-date"]'))
  #repetitionSelect = new Select(this.page.find('[data-qa="repetition"]'))
  #dailyStartTimeInput = new TextInput(
    this.page.find('[data-qa="daily-start-time-0"]')
  )
  #dailyEndTimeInput = new TextInput(
    this.page.find('[data-qa="daily-end-time-0"]')
  )
  #weeklyStartTimeInputs = [0, 1, 2, 3, 4, 5, 6].map(
    (index) =>
      new TextInput(this.page.find(`[data-qa="weekly-${index}-start-time-0"]`))
  )
  #weeklyEndTimeInputs = [0, 1, 2, 3, 4, 5, 6].map(
    (index) =>
      new TextInput(this.page.find(`[data-qa="weekly-${index}-end-time-0"]`))
  )
  #modalSendButton = this.page.find('[data-qa="modal-okBtn"]')

  async createRepeatingDailyReservation(
    dateRange: FiniteDateRange,
    startTime: string,
    endTime: string
  ) {
    await this.#startDateInput.fill(dateRange.start.format())
    await this.#endDateInput.fill(dateRange.end.format())
    await this.#dailyStartTimeInput.fill(startTime)
    await this.#dailyEndTimeInput.fill(endTime)

    await this.#modalSendButton.click()
  }

  async createRepeatingWeeklyReservation(
    dateRange: FiniteDateRange,
    weeklyTimes: { startTime: string; endTime: string }[]
  ) {
    await this.#startDateInput.fill(dateRange.start.format())
    await this.#endDateInput.fill(dateRange.end.format())
    await this.#repetitionSelect.selectOption({ value: 'WEEKLY' })
    await weeklyTimes.reduce(async (promise, { startTime, endTime }, index) => {
      await promise
      await this.#weeklyStartTimeInputs[index].fill(startTime)
      await this.#weeklyEndTimeInputs[index].fill(endTime)
    }, Promise.resolve())

    await this.#modalSendButton.click()
  }
}

class AbsencesModal {
  constructor(private readonly page: Page) {}

  #childCheckbox = (childId: string) =>
    new Checkbox(this.page.find(`[data-qa="child-${childId}"]`))

  #startDateInput = new TextInput(this.page.find('[data-qa="start-date"]'))
  #endDateInput = new TextInput(this.page.find('[data-qa="end-date"]'))
  #absenceChip = (type: string) =>
    new Checkbox(this.page.find(`[data-qa="absence-${type}"]`))
  #modalSendButton = this.page.find('[data-qa="modal-okBtn"]')

  async markAbsence(
    child: { id: string },
    totalChildren: number,
    dateRange: FiniteDateRange,
    absenceType: 'SICKLEAVE' | 'OTHER_ABSENCE' | 'PLANNED_ABSENCE'
  ) {
    await this.deselectChildren(3)
    await this.#childCheckbox(child.id).click()
    await this.#startDateInput.fill(dateRange.start.format())
    await this.#endDateInput.fill(dateRange.end.format())
    await this.#absenceChip(absenceType).click()

    await this.#modalSendButton.click()
  }

  async deselectChildren(n: number) {
    for (let i = 0; i < n; i++) {
      await this.page.findAll('div[data-qa*="child"]').nth(i).click()
    }
  }

  async assertStartDate(text: string) {
    await waitUntilEqual(() => this.#startDateInput.inputValue, text)
  }

  async assertEndDate(text: string) {
    await waitUntilEqual(() => this.#endDateInput.inputValue, text)
  }
}

class DayView extends Element {
  constructor(private readonly page: Page, root: Element) {
    super(root)
  }

  #editButton = this.findByDataQa('edit')
  #createAbsenceButton = this.findByDataQa('create-absence')

  #reservationsOfChild(childId: UUID) {
    return this.findByDataQa(`reservations-of-${childId}`)
  }

  async assertNoReservation(childId: UUID) {
    await this.#reservationsOfChild(childId)
      .findByDataQa('no-reservations')
      .waitUntilVisible()
  }

  async assertReservations(childId: UUID, value: string) {
    await waitUntilEqual(
      () =>
        this.#reservationsOfChild(childId).findByDataQa('reservations')
          .textContent,
      value
    )
  }

  async assertAbsence(childId: UUID, value: string) {
    await waitUntilEqual(
      () =>
        this.#reservationsOfChild(childId).findByDataQa('absence').textContent,
      value
    )
  }

  async edit() {
    await this.#editButton.click()
    return new DayViewEditor(this)
  }

  async createAbsence() {
    await this.#createAbsenceButton.click()
    return new AbsencesModal(this.page)
  }

  async close() {
    await this.findByDataQa('day-view-close-button').click()
  }
}

class DayViewEditor extends Element {
  #saveButton = this.findByDataQa('save')

  #reservationsOfChild(childId: UUID) {
    return this.findByDataQa(`reservations-of-${childId}`)
  }

  async fillReservationTimes(
    childId: UUID,
    startTime: string,
    endTime: string
  ) {
    const child = this.#reservationsOfChild(childId)
    await new TextInput(child.findByDataQa('first-reservation-start')).fill(
      startTime
    )
    await new TextInput(child.findByDataQa('first-reservation-end')).fill(
      endTime
    )
  }

  async save() {
    await this.#saveButton.click()
  }
}

class HolidayModal extends Element {
  #childSection = (childId: string) =>
    this.findByDataQa(`holiday-section-${childId}`)
  #childHolidaySelect = (childId: string) =>
    new Select(this.#childSection(childId).findByDataQa('period-select'))
  #modalSendButton = this.findByDataQa('modal-okBtn')

  async markHolidays(values: { child: { id: string }; option: string }[]) {
    for (const { child, option } of values) {
      await this.#childHolidaySelect(child.id).selectOption(option)
    }
    await this.#modalSendButton.click()
    await this.waitUntilHidden()
  }

  async markNoHolidays(children: { id: string }[]) {
    for (const child of children) {
      await this.#childHolidaySelect(child.id).selectOption({ index: 0 })
    }
    await this.#modalSendButton.click()
    await this.waitUntilHidden()
  }

  async markHoliday(child: { id: string }, option: string) {
    await this.markHolidays([{ child, option }])
  }

  async markNoHoliday(child: { id: string }) {
    await this.markNoHolidays([child])
  }

  async assertSelectedFixedPeriods(
    values: { child: { id: string }; option: string }[]
  ) {
    for (const { child, option } of values) {
      await waitUntilEqual(
        () => this.#childHolidaySelect(child.id).selectedOption,
        option
      )
    }
  }

  async assertNotEligible(child: { id: string }) {
    await this.#childSection(child.id)
      .findByDataQa('not-eligible')
      .waitUntilVisible()
  }
}
