// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from 'e2e-playwright/utils'
import { Checkbox, Combobox, RawTextInput } from 'e2e-playwright/utils/element'
import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { Page } from 'playwright'

export default class CitizenCalendarPage {
  constructor(
    private readonly page: Page,
    private readonly type: 'desktop' | 'mobile'
  ) {}

  #openCalendarActionsModal = this.page.locator(
    '[data-qa="open-calendar-actions-modal"]'
  )

  #childCheckbox = (childId: string) =>
    new Checkbox(this.page, `[data-qa="child-${childId}"]`)
  #startDateInput = new RawTextInput(this.page, '[data-qa="start-date"]')
  #endDateInput = new RawTextInput(this.page, '[data-qa="end-date"]')
  #repetitionCombobox = new Combobox(this.page, '[data-qa="repetition"]')
  #dailyStartTimeInput = new RawTextInput(
    this.page,
    '[data-qa="daily-start-time"]'
  )
  #dailyEndTimeInput = new RawTextInput(this.page, '[data-qa="daily-end-time"]')
  #weeklyStartTimeInputs = [0, 1, 2, 3, 4, 5, 6].map(
    (index) =>
      new RawTextInput(this.page, `[data-qa="weekly-start-time-${index}"]`)
  )
  #weeklyEndTimeInputs = [0, 1, 2, 3, 4, 5, 6].map(
    (index) =>
      new RawTextInput(this.page, `[data-qa="weekly-end-time-${index}"]`)
  )
  #absenceChip = (type: string) =>
    new Checkbox(this.page, `[data-qa="absence-${type}"]`)
  #modalSendButton = this.page.locator('[data-qa="modal-okBtn"]')

  #dayCell = (date: LocalDate) =>
    this.page.locator(
      `[data-qa="${this.type}-calendar-day-${date.formatIso()}"]`
    )

  async createReperatingDailyReservation(
    dateRange: FiniteDateRange,
    startTime: string,
    endTime: string
  ) {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page
        .locator('[data-qa="calendar-action-reservations"]')
        .click()
    } else {
      await this.page.locator('[data-qa="open-reservations-modal"]').click()
    }

    await this.#startDateInput.clear()
    await this.#startDateInput.type(dateRange.start.format())
    await this.#endDateInput.type(dateRange.end.format())
    await this.#dailyStartTimeInput.type(startTime)
    await this.#dailyEndTimeInput.type(endTime)

    await this.#modalSendButton.click()
  }

  async createReperatingWeeklyReservation(
    dateRange: FiniteDateRange,
    weeklyTimes: { startTime: string; endTime: string }[]
  ) {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page
        .locator('[data-qa="calendar-action-reservations"]')
        .click()
    } else {
      await this.page.locator('[data-qa="open-reservations-modal"]').click()
    }

    await this.#startDateInput.clear()
    await this.#startDateInput.type(dateRange.start.format())
    await this.#endDateInput.type(dateRange.end.format())
    await this.#repetitionCombobox.fill('Viikoittain')
    await this.#repetitionCombobox.findItem('Viikoittain').click()
    await weeklyTimes.reduce(async (promise, { startTime, endTime }, index) => {
      await promise
      await this.#weeklyStartTimeInputs[index].type(startTime)
      await this.#weeklyEndTimeInputs[index].type(endTime)
    }, Promise.resolve())

    await this.#modalSendButton.click()
  }

  async markAbsence(
    child: { id: string },
    totalChildren: number,
    dateRange: FiniteDateRange,
    absenceType: 'SICKLEAVE' | 'OTHER_ABSENCE' | 'PLANNED_ABSENCE'
  ) {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page.locator('[data-qa="calendar-action-absences"]').click()
    } else {
      await this.page.locator('[data-qa="open-absences-modal"]').click()
    }

    await this.deselectChildren(3)
    await this.#childCheckbox(child.id).click()
    await this.#startDateInput.clear()
    await this.#startDateInput.type(dateRange.start.format())
    await this.#endDateInput.type(dateRange.end.format())
    await this.#absenceChip(absenceType).click()

    await this.#modalSendButton.click()
  }

  async assertReservations(
    date: LocalDate,
    absence: boolean,
    reservations: { startTime: string; endTime: string }[]
  ) {
    await waitUntilEqual(
      () => this.#dayCell(date).locator('[data-qa="reservations"]').innerText(),
      [
        ...(absence ? ['Poissa'] : []),
        ...reservations.map(
          ({ startTime, endTime }) => `${startTime} – ${endTime}`
        )
      ].join(', ')
    )
  }

  async deselectChildren(n: number) {
    for (let i = 0; i < n; i++) {
      await this.page.locator('[data-qa*="child"]').nth(i).click()
    }
  }
}
