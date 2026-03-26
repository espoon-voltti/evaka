// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import uniq from 'lodash/uniq'

import type FiniteDateRange from 'lib-common/finite-date-range'
import type { ChildId } from 'lib-common/generated/api-types/shared'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import { expect } from '../../playwright'
import type { Page, ElementCollection } from '../../utils/page'
import {
  Checkbox,
  Element,
  Radio,
  Select,
  TextInput,
  DatePicker,
  SelectionChip
} from '../../utils/page'

import {
  DiscussionReservationModal,
  DiscussionSurveyModal
} from './citizen-discussion-surveys'

export type FormatterReservation = {
  startTime: string
  endTime: string
  isOverdraft?: boolean
}
export type TwoPartReservation = [FormatterReservation, FormatterReservation]

export default class CitizenCalendarPage {
  #openCalendarActionsModal: Element
  reservationModal: Element
  #holidayCtas: ElementCollection
  expiringIncomeCta: Element
  #dailyServiceTimeNotificationModal: Element
  discussionSurveyModal: Element
  discussionReservationModal: Element
  cancelConfirmModal: Element
  discussionsCta: Element

  constructor(
    private readonly page: Page,
    private readonly type: 'desktop' | 'mobile'
  ) {
    this.#openCalendarActionsModal = page.findByDataQa(
      'open-calendar-actions-modal'
    )
    this.reservationModal = page.findByDataQa('reservation-modal')
    this.#holidayCtas = page.findAllByDataQa('holiday-period-cta')
    this.expiringIncomeCta = page.findByDataQa('expiring-income-cta')
    this.#dailyServiceTimeNotificationModal = page.findByDataQa(
      'daily-service-time-notification-modal'
    )
    this.discussionSurveyModal = page.findByDataQa('discussions-modal')
    this.cancelConfirmModal = page.findByDataQa('confirm-cancel-modal')
    this.discussionReservationModal = page.findByDataQa(
      'discussion-reservations-modal'
    )
    this.discussionsCta = page.findByDataQa('active-discussions-cta')
  }

  dayCell = (date: LocalDate) =>
    this.page.findByDataQa(`${this.type}-calendar-day-${date.formatIso()}`)
  monthlySummaryInfoButton = (year: number, month: number) =>
    this.page.findByDataQa(`monthly-summary-info-button-${month}-${year}`)
  navigateNextMonthButton = () => this.page.findByDataQa('next-month-button')
  navigatePreviousMonthButton = () =>
    this.page.findByDataQa('previous-month-button')
  navigateToTodayButton = () => this.page.findByDataQa('current-month-button')
  monthTitle = () => this.page.findByDataQa('calendar-month-title')
  mobileLoadPreviousDataButton = () =>
    this.page.findByDataQa('fetch-previous-button')

  async waitUntilLoaded() {
    await expect(
      this.page.find('[data-qa="calendar-page"][data-isloading="false"]')
    ).toBeVisible()
  }

  async assertEventCount(date: LocalDate, count: number) {
    await expect(this.dayCell(date).findByDataQa('event-count')).toHaveText(
      count.toString(),
      { useInnerText: true }
    )
  }

  async openReservationModal() {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page.findByDataQa('calendar-action-reservations').click()
    } else {
      await this.page.findByDataQa('open-reservations-modal').click()
    }
    return new ReservationModal(this.page.findByDataQa('reservation-modal'))
  }

  getReservationModal() {
    return new ReservationModal(this.reservationModal)
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

  async openDiscussionSurveyModal() {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await this.page.findByDataQa('calendar-action-discussions').click()
    } else {
      await this.page.findByDataQa('open-discussions-modal').click()
    }

    await expect(this.discussionSurveyModal).toBeVisible()
    return new DiscussionSurveyModal(this.discussionSurveyModal)
  }

  async openDiscussionReservationModal(surveyId: string, childId: string) {
    const surveyModal = await this.openDiscussionSurveyModal()

    await surveyModal.showReservationModal(surveyId, childId)

    return new DiscussionReservationModal(this.discussionReservationModal)
  }

  async assertHolidayModalVisible(): Promise<void> {
    return await expect(
      this.page.findByDataQa('fixed-period-selection-modal')
    ).toBeVisible()
  }

  async assertHolidayModalButtonVisible() {
    if (this.type === 'mobile') {
      await this.#openCalendarActionsModal.click()
      await expect(
        this.page.findByDataQa('calendar-action-holidays')
      ).toBeVisible()
    } else {
      await this.page.findByDataQa('open-holiday-modal').click()
    }
  }

  async openDayView(date: LocalDate) {
    await this.dayCell(date).click()
    return new DayView(this.page, this.page.findByDataQa('calendar-dayview'))
  }

  getMonthlySummary(year: number, month: number) {
    return new MonthlySummary(
      this.page.findByDataQa(
        `monthly-summary-info-container-${month}-${year}-text`
      )
    )
  }

  async navigateToNextMonth() {
    return await this.navigateNextMonthButton().click()
  }

  async navigateToNextMonths(n: number) {
    for (let i = 0; i < n; i++) {
      await this.navigateToNextMonth()
    }
  }

  async navigateToPreviousMonth() {
    if (this.type === 'mobile') {
      return await this.mobileLoadPreviousDataButton().click()
    }
    return await this.navigatePreviousMonthButton().click()
  }

  async navigateToPreviousMonths(n: number) {
    for (let i = 0; i < n; i++) {
      await this.navigateToPreviousMonth()
    }
  }

  async navigateToToday() {
    return await this.navigateToTodayButton().click()
  }

  async assertMonthTitle(title: string) {
    await expect(this.monthTitle()).toHaveText(title, { useInnerText: true })
  }

  async openMonthlySummary(year: number, month: number) {
    await this.monthlySummaryInfoButton(year, month).click()
    return this.getMonthlySummary(year, month)
  }

  async assertHoliday(date: LocalDate) {
    await expect(
      this.dayCell(date).findByDataQa('reservations').findByDataQa('holiday')
    ).toBeVisible()
  }

  readonly holidayPeriodBackground = 'rgb(253, 230, 219)'
  readonly nonEditableAbsenceBackground = 'rgb(218, 221, 226)'

  private async getDayBackgroundColor(date: LocalDate) {
    return await this.dayCell(date).evaluate(
      (el) => getComputedStyle(el).backgroundColor
    )
  }

  async assertDayHighlight(
    date: LocalDate,
    state: 'none' | 'holidayPeriod' | 'nonEditableAbsence'
  ) {
    const expectedBackground =
      state === 'holidayPeriod'
        ? this.holidayPeriodBackground
        : state === 'nonEditableAbsence'
          ? this.nonEditableAbsenceBackground
          : null
    if (expectedBackground !== null) {
      await expect
        .poll(() => this.getDayBackgroundColor(date))
        .toBe(expectedBackground)
    } else {
      await expect
        .poll(() => this.getDayBackgroundColor(date))
        .toMatch(/^(rgb\(255, 255, 255\)|rgba\(0, 0, 0, 0\))$/)
    }
  }

  async assertDay(
    date: LocalDate,
    groups: { childIds: UUID[]; text: string }[]
  ) {
    const day = this.dayCell(date)
    const rows = day
      .findByDataQa('reservations')
      .findAllByDataQa('reservation-group')

    if (groups.length === 0) {
      await expect(day).toBeVisible()
    }
    await expect(rows).toHaveCount(groups.length)

    for (const [i, group] of groups.entries()) {
      const row = rows.nth(i)
      const childIds = uniq(group.childIds)

      await expect(row.findAllByDataQa('child-image')).toHaveCount(
        childIds.length
      )
      for (const childId of childIds) {
        await expect(
          row.find(`[data-qa="child-image"][data-qa-child-id="${childId}"]`)
        ).toBeVisible()
      }

      await row.findByDataQa('reservation-text').assertText((elementText) => {
        // Remove soft hyphens
        const normalizedText = elementText.replace(/\u00AD/g, '')
        return normalizedText === group.text
      })
    }
  }

  async assertHasNoDate(date: LocalDate) {
    const day = this.page.findByDataQa(
      `${this.type}-calendar-day-${date.formatIso()}`
    )
    await expect(day).toBeHidden()
  }

  async assertTwoPartReservationFromDayCellGroup(
    date: LocalDate,
    twoPartReservation: TwoPartReservation,
    childId: UUID,
    formatter: (res: TwoPartReservation) => string,
    groupIndex = 0
  ) {
    const reservationRows = this.dayCell(date)
      .findByDataQa('reservations')
      .findAllByDataQa('reservation-group')

    const row = reservationRows.nth(groupIndex)

    await expect(
      row.find(`[data-qa="child-image"][data-qa-child-id="${childId}"]`)
    ).toBeVisible()

    await expect(row.findByDataQa('reservation-text')).toHaveText(
      formatter(twoPartReservation),
      { useInnerText: true }
    )
  }

  async closeToasts(): Promise<void> {
    const toastCloseButtons = this.page.findAllByDataQa('toast-close-button')
    const count = await toastCloseButtons.count()
    for (let i = 0; i < count; i++) {
      await toastCloseButtons.nth(i).click()
    }
  }

  async closeTimedToasts(): Promise<void> {
    const toastCloseButtons = this.page.findAllByDataQa(
      'timed-toast-close-button'
    )
    const count = await toastCloseButtons.count()
    for (let i = 0; i < count; i++) {
      await toastCloseButtons.nth(i).click()
    }
  }

  async assertHolidayCtaContent(content: string): Promise<void> {
    await expect(this.#holidayCtas.nth(0)).toHaveText(content, {
      useInnerText: true
    })
  }

  async clickHolidayCta(): Promise<void> {
    return this.#holidayCtas.nth(0).click()
  }

  async clickExpiringIncomeCta(): Promise<void> {
    return await this.expiringIncomeCta.click()
  }

  async assertHolidayCtaNotVisible(): Promise<void> {
    await this.page
      .find('[data-holiday-period-cta-status]')
      .assertAttributeEquals('data-holiday-period-cta-status', 'success')
    await expect(this.#holidayCtas).toHaveCount(0)
  }

  async assertExpiringIncomeCtaNotVisible(): Promise<void> {
    await this.page
      .find('[data-expiring-income-cta-status]')
      .assertAttributeEquals('data-expiring-income-cta-status', 'success')
    await expect(this.expiringIncomeCta).toBeHidden()
  }

  get dailyServiceTimeNotificationText() {
    return this.#dailyServiceTimeNotificationModal.findByDataQa('text')
  }

  async assertChildCountOnDay(date: LocalDate, expectedCount: number) {
    const childImages = this.dayCell(date).findAllByDataQa('child-image')
    await expect(childImages).toHaveCount(expectedCount)
  }

  async getPageActiveElementDetails() {
    return await this.page.page.evaluate(() => {
      const activeElement = document.activeElement
      let computedStyle: CSSStyleDeclaration = {} as CSSStyleDeclaration
      if (activeElement) {
        computedStyle = getComputedStyle(activeElement)
      }
      return {
        id: activeElement?.id,
        dataQa: activeElement?.getAttribute('data-qa'),
        computedStyle
      }
    })
  }

  readonly desktopFocusColor = 'rgb(0, 71, 182)'
  readonly mobileFocusColor = 'rgb(77, 127, 204)'

  async assertDayIsFocusedAndStyled(dayId: string) {
    const activeElement = await this.getPageActiveElementDetails()
    expect(activeElement.id).toBe(dayId)
    expect([this.desktopFocusColor, this.mobileFocusColor]).toContain(
      activeElement.computedStyle.outlineColor
    )
    expect(activeElement.computedStyle.outlineWidth).toBe('2px')
  }
}

type ReadOnlyDayState =
  | 'noChildren'
  | 'absentNotEditable'
  | 'termBreak'
  | 'notYetReservable'
  | 'reservationClosed'
  | 'holiday'

class ReservationModal extends Element {
  startDate: DatePicker
  endDate: DatePicker
  dailyAddReservationButton: Element
  dailyAbsentButton: Element
  dailySecondRangeDeleteButton: Element
  dailyScreenReaderMessage: Element
  #repetitionSelect: Select
  #dailyStartTimeInput: TextInput
  #dailyEndTimeInput: TextInput
  #modalSendButton: Element
  #weeklyStartTimeInputs: TextInput[]
  #weeklyEndTimeInputs: TextInput[]
  #weeklyAbsentButtons: Element[]
  #dailyPresentRadio: Radio
  #dailyAbsentRadio: Radio
  #weeklyPresentRadios: Radio[]
  #weeklyAbsentRadios: Radio[]

  constructor(el: Element) {
    super(el)
    this.startDate = new DatePicker(this.findByDataQa('start-date'))
    this.endDate = new DatePicker(this.findByDataQa('end-date'))
    this.#repetitionSelect = new Select(this.findByDataQa('repetition'))
    this.#dailyStartTimeInput = new TextInput(
      this.findByDataQa('daily-time-0-start')
    )
    this.#dailyEndTimeInput = new TextInput(
      this.findByDataQa('daily-time-0-end')
    )
    this.#weeklyStartTimeInputs = [0, 1, 2, 3, 4, 5, 6].map(
      (index) =>
        new TextInput(this.findByDataQa(`weekly-${index}-time-0-start`))
    )
    this.#weeklyEndTimeInputs = [0, 1, 2, 3, 4, 5, 6].map(
      (index) => new TextInput(this.findByDataQa(`weekly-${index}-time-0-end`))
    )
    this.#weeklyAbsentButtons = [0, 1, 2, 3, 4, 5, 6].map(
      (index) =>
        new TextInput(this.findByDataQa(`weekly-${index}-absent-button`))
    )
    this.#dailyPresentRadio = new Radio(
      this.findByDataQa('daily-present-radio')
    )
    this.#dailyAbsentRadio = new Radio(this.findByDataQa('daily-absent-radio'))
    this.#weeklyPresentRadios = [0, 1, 2, 3, 4, 5, 6].map(
      (index) => new Radio(this.findByDataQa(`weekly-${index}-present-radio`))
    )
    this.#weeklyAbsentRadios = [0, 1, 2, 3, 4, 5, 6].map(
      (index) => new Radio(this.findByDataQa(`weekly-${index}-absent-radio`))
    )
    this.#modalSendButton = this.findByDataQa('modal-okBtn')
    this.dailyAddReservationButton = this.findByDataQa('daily-add-res-button')
    this.dailyAbsentButton = this.findByDataQa('daily-absent-button')
    this.dailySecondRangeDeleteButton = this.findByDataQa(
      'daily-time-1-delete-button'
    )
    this.dailyScreenReaderMessage = this.findByDataQa(
      'daily-screen-reader-message'
    )
  }

  irregularStartTimeInput = (date: LocalDate) =>
    new TextInput(
      this.findByDataQa(`irregular-${date.formatIso()}-time-0-start`)
    )

  irregularEndTimeInput = (date: LocalDate) =>
    new TextInput(this.findByDataQa(`irregular-${date.formatIso()}-time-0-end`))

  irregularPresentRadio = (date: LocalDate) =>
    new Radio(this.findByDataQa(`irregular-${date.formatIso()}-present-radio`))

  irregularAbsentRadio = (date: LocalDate) =>
    new Radio(this.findByDataQa(`irregular-${date.formatIso()}-absent-radio`))

  #childCheckbox = (childId: string) =>
    new Checkbox(this.findByDataQa(`child-${childId}`))

  async save() {
    await this.#modalSendButton.click()
  }

  async selectRepetition(repetition: 'DAILY' | 'WEEKLY' | 'IRREGULAR') {
    await this.#repetitionSelect.selectOption({ value: repetition })
  }

  async fillDailyReservationInfo(
    dateRange: FiniteDateRange,
    startTime: string,
    endTime: string,
    childIds?: UUID[],
    totalChildren?: number
  ) {
    if (childIds && totalChildren) {
      await this.deselectChildren(totalChildren)
      for (const childId of childIds) {
        await this.#childCheckbox(childId).click()
      }
    }

    await this.startDate.fill(dateRange.start.format())
    await this.endDate.fill(dateRange.end.format())
    await this.#dailyStartTimeInput.fill(startTime)
    await this.#dailyEndTimeInput.fill(endTime)
    await this.#dailyEndTimeInput.press('Tab')
  }

  async fillDaily2ndReservationInfo(startTime: string, endTime: string) {
    await this.dailyAddReservationButton.click()
    const daily2ndReservationStartTimeInput = new TextInput(
      this.findByDataQa('daily-time-1-start')
    )

    const daily2ndReservationEndTimeInput = new TextInput(
      this.findByDataQa('daily-time-1-end')
    )
    await daily2ndReservationStartTimeInput.fill(startTime)
    await daily2ndReservationEndTimeInput.fill(endTime)
    await daily2ndReservationEndTimeInput.press('Tab')
  }

  async createRepeatingDailyReservation(
    dateRange: FiniteDateRange,
    startTime: string,
    endTime: string,
    childIds?: UUID[],
    totalChildren?: number
  ) {
    await this.fillDailyReservationInfo(
      dateRange,
      startTime,
      endTime,
      childIds,
      totalChildren
    )
    await this.save()
  }

  private async deselectChildren(n: number) {
    for (let i = 0; i < n; i++) {
      await this.findAllByDataQa('relevant-child').nth(i).click()
    }
  }

  async deselectAllChildren() {
    const pills = this.findAllByDataQa('relevant-child')
    for (const el of await pills.elementHandles()) {
      await el.click()
    }
  }

  async selectChild(childId: string) {
    await this.findByDataQa(`child-${childId}`).click()
  }

  async fillWeeklyReservationInfo(
    dateRange: FiniteDateRange,
    weeklyTimes: ({ startTime: string; endTime: string } | { absence: true })[]
  ) {
    await this.startDate.fill(dateRange.start.format())
    await this.endDate.fill(dateRange.end.format())
    await this.selectRepetition('WEEKLY')
    await weeklyTimes.reduce(async (promise, weeklyTime, index) => {
      await promise
      if ('absence' in weeklyTime) {
        await this.#weeklyAbsentButtons[index].click()
      } else {
        await this.#weeklyStartTimeInputs[index].fill(weeklyTime.startTime)
        const end = this.#weeklyEndTimeInputs[index]
        await this.#weeklyEndTimeInputs[index].fill(weeklyTime.endTime)
        await end.press('Tab')
      }
    }, Promise.resolve())
  }

  async createRepeatingWeeklyReservation(
    dateRange: FiniteDateRange,
    weeklyTimes: ({ startTime: string; endTime: string } | { absence: true })[]
  ) {
    await this.fillWeeklyReservationInfo(dateRange, weeklyTimes)
    await this.save()
  }

  async fillIrregularReservationInfo(
    dateRange: FiniteDateRange,
    irregularTimes: { date: LocalDate; startTime: string; endTime: string }[]
  ) {
    await this.startDate.fill(dateRange.start.format())
    await this.endDate.fill(dateRange.end.format())
    await this.selectRepetition('IRREGULAR')

    await irregularTimes.reduce(async (promise, weeklyTime) => {
      await promise
      await this.irregularStartTimeInput(weeklyTime.date).fill(
        weeklyTime.startTime
      )
      const end = this.irregularEndTimeInput(weeklyTime.date)
      await end.fill(weeklyTime.endTime)
      await end.press('Tab')
    }, Promise.resolve())
  }

  async createIrregularReservation(
    dateRange: FiniteDateRange,
    irregularTimes: { date: LocalDate; startTime: string; endTime: string }[]
  ) {
    await this.fillIrregularReservationInfo(dateRange, irregularTimes)
    await this.save()
  }

  async fillDailyHolidayPeriodReservationInfo(
    dateRange: FiniteDateRange,
    dailyPresence: boolean
  ) {
    await this.startDate.fill(dateRange.start.format())
    await this.endDate.fill(dateRange.end.format())
    await this.selectRepetition('DAILY')

    if (dailyPresence) {
      await this.#dailyPresentRadio.check()
    } else {
      await this.#dailyAbsentRadio.check()
    }
  }

  async fillWeeklyHolidayPeriodReservationInfo(
    dateRange: FiniteDateRange,
    weeklyPresences: boolean[]
  ) {
    await this.startDate.fill(dateRange.start.format())
    await this.endDate.fill(dateRange.end.format())
    await this.selectRepetition('WEEKLY')

    await weeklyPresences.reduce(async (promise, presence, index) => {
      await promise
      if (presence) {
        await this.#weeklyPresentRadios[index].check()
      } else {
        await this.#weeklyAbsentRadios[index].check()
      }
    }, Promise.resolve())
  }

  async fillIrregularHolidayPeriodReservationInfo(
    dateRange: FiniteDateRange,
    irregularPresences: { date: LocalDate; present: boolean }[]
  ) {
    await this.startDate.fill(dateRange.start.format())
    await this.endDate.fill(dateRange.end.format())
    await this.selectRepetition('IRREGULAR')

    await irregularPresences.reduce(async (promise, presence) => {
      await promise
      if (presence.present) {
        await this.irregularPresentRadio(presence.date).check()
      } else {
        await this.irregularAbsentRadio(presence.date).check()
      }
    }, Promise.resolve())
  }

  async assertReadOnlyWeeklyDay(dayIndex: number, state: ReadOnlyDayState) {
    await expect(this.findByDataQa(`weekly-${dayIndex}-${state}`)).toBeVisible()
  }

  async assertReadOnlyIrregularDay(date: LocalDate, state: ReadOnlyDayState) {
    await expect(
      this.findByDataQa(`irregular-${date.formatIso()}-${state}`)
    ).toBeVisible()
  }

  async assertSendButtonDisabled(targetValue: boolean) {
    await this.#modalSendButton.assertDisabled(targetValue)
  }

  async assertDailyInputValidationWarningVisible(
    input: 'start' | 'end',
    reservationIndex: 0 | 1
  ) {
    await expect(
      this.findByDataQa(`daily-time-${reservationIndex}-${input}-info`)
    ).toBeVisible()
  }

  async assertWeeklyInputValidationWarningVisible(
    input: 'start' | 'end',
    weekDayIndex: 0 | 1 | 2 | 3 | 4 | 5 | 6,
    reservationIndex: 0 | 1
  ) {
    await expect(
      this.findByDataQa(
        `weekly-${weekDayIndex}-time-${reservationIndex}-${input}-info`
      )
    ).toBeVisible()
  }

  async assertIrregularInputValidationWarningVisible(
    input: 'start' | 'end',
    reservationIndex: 0 | 1,
    date: LocalDate
  ) {
    await expect(
      this.findByDataQa(
        `irregular-${date.formatIso()}-time-${reservationIndex}-${input}-info`
      )
    ).toBeVisible()
  }

  async assertChildrenSelectable(childIds: string[]) {
    for (const childId of childIds) {
      await expect(this.findByDataQa(`child-${childId}`)).toBeVisible()
    }

    await expect(this.findAllByDataQa('relevant-child')).toHaveCount(
      childIds.length
    )
  }

  async assertChildrenChipDisabled(disabled: boolean, childIds: string[]) {
    for (const childId of childIds) {
      const childChip = new SelectionChip(this.findByDataQa(`child-${childId}`))
      await childChip.assertDisabled(disabled)
    }
  }

  async assertHolidayPeriodInfoContent(content: string) {
    await expect(this.findByDataQa('holiday-period-info')).toHaveText(content, {
      useInnerText: true
    })
  }

  async assertIncompletelyAnsweredPeriodsInfoVisible() {
    await expect(
      this.findByDataQa('incompletely-answered-periods-info')
    ).toBeVisible()
  }

  async assertIncompletelyAnsweredPeriodsInfoNotVisible() {
    await expect(
      this.findByDataQa('incompletely-answered-periods-info')
    ).toBeHidden()
  }

  getInfoBox(childId: string) {
    return this.findByDataQa(`child-not-started-infobox-${childId}`)
  }
}

class AbsencesModal {
  startDateInput: TextInput
  endDateInput: TextInput
  modalSendButton: Element
  absenceTypeRequiredError: Element

  constructor(private readonly page: Page) {
    this.startDateInput = new TextInput(page.findByDataQa('start-date'))
    this.endDateInput = new TextInput(page.findByDataQa('end-date'))
    this.modalSendButton = page.findByDataQa('modal-okBtn')
    this.absenceTypeRequiredError = page.findByDataQa(
      'modal-absence-type-required-error'
    )
  }

  #childCheckbox = (childId: string) =>
    new Checkbox(this.page.findByDataQa(`child-${childId}`))

  #absenceChip = (type: string) =>
    new Checkbox(this.page.findByDataQa(`absence-${type}`))

  #notStartedChildInfoBox = (childId: string) =>
    this.page.findByDataQa(`child-not-started-infobox-${childId}`)

  async assertChildrenSelectable(childIds: string[]) {
    for (const childId of childIds) {
      await expect(this.page.findByDataQa(`child-${childId}`)).toBeVisible()
    }

    await expect(this.page.findAllByDataQa('relevant-child')).toHaveCount(
      childIds.length
    )
  }

  async assertChildrenChipDisabled(disabled: boolean, childIds: string[]) {
    for (const childId of childIds) {
      const childChip = new SelectionChip(
        this.page.findByDataQa(`child-${childId}`)
      )
      await childChip.assertDisabled(disabled)
    }
  }

  async markAbsence(
    child: { id: string },
    totalChildren: number,
    dateRange: FiniteDateRange,
    absenceType: 'SICKLEAVE' | 'OTHER_ABSENCE' | 'PLANNED_ABSENCE'
  ) {
    await this.deselectChildren(totalChildren)
    await this.toggleChildren([child])
    await this.selectDates(dateRange)
    await this.selectAbsenceType(absenceType)
    await this.submit()
  }

  async deselectChildren(n: number) {
    for (let i = 0; i < n; i++) {
      await this.page.findAllByDataQa('relevant-child').nth(i).click()
    }
  }

  async toggleChildren(children: { id: string }[]) {
    for (const child of children) {
      await this.#childCheckbox(child.id).click()
    }
  }

  async selectDates(dateRange: FiniteDateRange) {
    await this.startDateInput.fill(dateRange.start.format())
    await this.endDateInput.fill(dateRange.end.format())
    await this.endDateInput.press('Enter')
  }

  async selectAbsenceType(
    absenceType: 'SICKLEAVE' | 'OTHER_ABSENCE' | 'PLANNED_ABSENCE'
  ) {
    await this.getAbsenceChip(absenceType).click()
  }

  getAbsenceChip(
    absenceType: 'SICKLEAVE' | 'OTHER_ABSENCE' | 'PLANNED_ABSENCE'
  ) {
    return this.#absenceChip(absenceType)
  }

  getInfoBox(childId: string) {
    return this.#notStartedChildInfoBox(childId)
  }

  tooManyAbsencesError = (childId: ChildId) =>
    this.page.findByDataQa(`too-many-absences-error-${childId}`)

  async submit() {
    await this.modalSendButton.click()
  }
}

class MonthlySummary extends Element {
  title = this.findByDataQa('monthly-summary-info-title')
  warningElement = this.findByDataQa('monthly-summary-warning')
  textElement = this.findByDataQa('monthly-summary-info-text')
}

class DayView extends Element {
  constructor(
    private readonly page: Page,
    root: Element
  ) {
    super(root)
  }

  #editButton = this.findByDataQa('edit')
  #createAbsenceButton = this.findByDataQa('create-absence')
  childNames = this.findAllByDataQa('child-name')

  #childSection(childId: UUID) {
    return this.findByDataQa(`child-${childId}`)
  }

  async assertNoReservation(childId: UUID) {
    await expect(
      this.#childSection(childId).findByDataQa('no-reservations')
    ).toBeVisible()
  }

  async assertReservations(childId: UUID, reservations: string) {
    const reservationsElement =
      this.#childSection(childId).findByDataQa('reservations')
    await expect(reservationsElement).toHaveText(reservations, {
      useInnerText: true
    })
  }

  async assertReservationNoTimes(childId: UUID) {
    await expect(
      this.#childSection(childId).findByDataQa('reservation-no-times')
    ).toBeVisible()
  }

  async assertReservationNotRequired(childId: UUID) {
    await expect(
      this.#childSection(childId).findByDataQa('reservation-not-required')
    ).toBeVisible()
  }

  async assertNotYetReservable(childId: UUID) {
    await expect(
      this.#childSection(childId).findByDataQa('not-yet-reservable')
    ).toBeVisible()
  }

  async assertAttendances(childId: UUID, attendances: string[]) {
    await expect(
      this.#childSection(childId).findByDataQa('attendances')
    ).toHaveText(attendances.join('\n'), { useInnerText: true })
  }

  getServiceUsageWarning(childId: UUID) {
    return this.#childSection(childId).findByDataQa('service-usage-warning')
  }

  getUsedService(childId: UUID) {
    return this.#childSection(childId).findByDataQa('used-service')
  }

  async assertAbsence(childId: UUID, value: string) {
    await expect(
      this.#childSection(childId).findByDataQa('absence')
    ).toHaveText(value, { useInnerText: true })
  }

  async assertNoActivePlacementsMsgVisible() {
    await expect(this.findByDataQa('no-active-placements-msg')).toBeVisible()
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

  async assertEvent(
    childId: UUID,
    eventId: UUID,
    { title, description }: { title: string; description: string },
    exportable = true
  ) {
    const event = this.#childSection(childId).findByDataQa(`event-${eventId}`)
    await expect(event).toBeVisible()
    await expect(event.findByDataQa('event-title')).toHaveText(title, {
      useInnerText: true
    })
    await expect(event.findByDataQa('event-description')).toHaveText(
      description,
      { useInnerText: true }
    )

    const exportButton = event.findByDataQa(`event-export-button-${eventId}`)
    if (exportable) {
      await expect(exportButton).toBeVisible()
    } else {
      await expect(exportButton).toBeHidden()
    }
  }

  async assertEventNotShown(childId: UUID, eventId: UUID) {
    await expect(
      this.#childSection(childId).findByDataQa(`event-${eventId}`)
    ).toBeHidden()
  }

  async assertDiscussionReservation(
    childId: UUID,
    eventId: UUID,
    eventTimeId: UUID,
    cancellable: boolean,
    {
      title,
      description,
      reservationText
    }: { title: string; description: string; reservationText: string },
    exportable = true
  ) {
    const event = this.#childSection(childId).findByDataQa(`event-${eventId}`)
    await expect(event.findByDataQa('title-text')).toHaveText(title, {
      useInnerText: true
    })
    await expect(event.findByDataQa('event-description')).toHaveText(
      description,
      { useInnerText: true }
    )
    await expect(
      event.findByDataQa(`reservation-time-${eventTimeId}`)
    ).toHaveText(reservationText, { useInnerText: true })
    const cancelButton = new TextInput(
      event.findByDataQa(`reservation-cancel-button-${eventTimeId}`)
    )
    await cancelButton.assertDisabled(!cancellable)

    const exportButton = event.findByDataQa(
      `event-export-button-${eventTimeId}`
    )
    if (exportable) {
      await expect(exportButton).toBeVisible()
    } else {
      await expect(exportButton).toBeHidden()
    }
  }

  async cancelDiscussionReservation(
    childId: UUID,
    eventId: UUID,
    eventTimeId: UUID
  ) {
    const event = this.#childSection(childId).findByDataQa(`event-${eventId}`)
    const cancelButton = new TextInput(
      event.findByDataQa(`reservation-cancel-button-${eventTimeId}`)
    )
    await cancelButton.click()
  }
}

class DayViewEditor extends Element {
  saveButton = this.findByDataQa('save')

  childSection(childId: UUID) {
    return new DayViewEditorChildSection(this.findByDataQa(`child-${childId}`))
  }
}

class DayViewEditorChildSection extends Element {
  absentButton = this.findByDataQa('edit-reservation-absent-button')
  addSecondReservationButton = this.findByDataQa(
    'edit-reservation-add-res-button'
  )
  reservationStart = new TextInput(
    this.findByDataQa('edit-reservation-time-0-start')
  )
  reservationEnd = new TextInput(
    this.findByDataQa('edit-reservation-time-0-end')
  )
  reservation2Start = new TextInput(
    this.findByDataQa('edit-reservation-time-1-start')
  )
  reservation2End = new TextInput(
    this.findByDataQa('edit-reservation-time-1-end')
  )
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
    await expect(this).toBeHidden()
  }

  async markNoHolidays(children: { id: string }[]) {
    for (const child of children) {
      await this.#childHolidaySelect(child.id).selectOption({ index: 0 })
    }
    await this.#modalSendButton.click()
    await expect(this).toBeHidden()
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
      await this.#childHolidaySelect(child.id).assertSelectedOption(option)
    }
  }

  async assertNotEligible(child: { id: string }) {
    await expect(
      this.#childSection(child.id).findByDataQa('not-eligible')
    ).toBeVisible()
  }

  async assertOptions(child: { id: string }, expected: string[]) {
    const select = this.#childHolidaySelect(child.id)
    await expect(select).toBeVisible()
    const actual = await select.allOptions
    expect(actual).toEqual(expected)
  }
}
