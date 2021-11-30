// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { waitUntilEqual } from 'e2e-playwright/utils'
import { CheckboxLocator } from 'e2e-playwright/utils/element'
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
    new CheckboxLocator(this.page.locator(`[data-qa="child-${childId}"]`))
  #startDateInput = this.page.locator('[data-qa="start-date"]')
  #endDateInput = this.page.locator('[data-qa="end-date"]')
  #repetitionSelect = this.page.locator('[data-qa="repetition"] select')
  #dailyStartTimeInput = this.page.locator('[data-qa="daily-start-time-0"]')
  #dailyEndTimeInput = this.page.locator('[data-qa="daily-end-time-0"]')
  #weeklyStartTimeInputs = [0, 1, 2, 3, 4, 5, 6].map((index) =>
    this.page.locator(`[data-qa="weekly-${index}-start-time-0"]`)
  )
  #weeklyEndTimeInputs = [0, 1, 2, 3, 4, 5, 6].map((index) =>
    this.page.locator(`[data-qa="weekly-${index}-end-time-0"]`)
  )
  #absenceChip = (type: string) =>
    new CheckboxLocator(this.page.locator(`[data-qa="absence-${type}"]`))
  #modalSendButton = this.page.locator('[data-qa="modal-okBtn"]')

  #dayCell = (date: LocalDate) =>
    this.page.locator(
      `[data-qa="${this.type}-calendar-day-${date.formatIso()}"]`
    )

  async createRepeatingDailyReservation(
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
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page
        .locator('[data-qa="calendar-action-reservations"]')
        .click()
    } else {
      await this.page.locator('[data-qa="open-reservations-modal"]').click()
    }

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
    await this.#startDateInput.fill(dateRange.start.format())
    await this.#endDateInput.fill(dateRange.end.format())
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
      await this.page.locator('div[data-qa*="child"]').nth(i).click()
    }
  }
}
