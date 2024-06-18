// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import {
  Element,
  Modal,
  Page,
  Select,
  TextInput,
  TreeDropdown
} from '../../../utils/page'

export interface DiscussionSurveyListItem {
  id: string
  status: string
  title: string
}

export interface TestEventTime {
  startTime: string
  endTime: string
}

export class DiscussionSurveyListPage {
  createSurveyButton: Element
  surveyList: Element
  constructor(protected readonly page: Page) {
    this.createSurveyButton = page.findByDataQa(
      'create-discussion-survey-button'
    )
    this.surveyList = page.findByDataQa('discussion-survey-list')
  }

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="discussion-survey-ist"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async openDiscussionSurvey(surveyId: string) {
    const surveyItem = this.surveyList.findByDataQa(`survey-${surveyId}`)
    await surveyItem.click()
    return new DiscussionSurveyReadView(this.page)
  }
  async openNewDiscussionSurveyEditor() {
    await this.createSurveyButton.click()
    return new CreateDiscussionSurveyEditor(this.page)
  }

  async assertDiscussionSurveyInList(expectedSurvey: DiscussionSurveyListItem) {
    const surveyItem = this.surveyList.findByDataQa(
      `survey-${expectedSurvey.id}`
    )
    await surveyItem
      .findByDataQa('survey-title')
      .assertTextEquals(expectedSurvey.title)
    await surveyItem
      .findByDataQa('survey-status')
      .assertTextEquals(expectedSurvey.status)
  }
  async assertDiscussionSurveyNotInList(surveyId: string) {
    await this.surveyList.findByDataQa(`survey-${surveyId}`).waitUntilHidden()
  }
}

export class CreateDiscussionSurveyEditor {
  submitSurveyButton: Element
  titleInput: TextInput
  descriptionInput: TextInput
  attendeeSelect: TreeDropdown
  newTimesCalendar: Element
  constructor(protected readonly page: Page) {
    this.submitSurveyButton = page.findByDataQa('survey-editor-submit-button')
    this.titleInput = new TextInput(page.findByDataQa('survey-title-input'))
    this.descriptionInput = new TextInput(
      page.findByDataQa('survey-description-input')
    )
    this.attendeeSelect = new TreeDropdown(
      page.findByDataQa('survey-attendees-select')
    )
    this.newTimesCalendar = page.findByDataQa('survey-times-calendar')
  }

  async waitUntilLoaded() {
    await this.submitSurveyButton.waitUntilVisible()
  }

  async addEventTime(date: LocalDate, index: number, eventTime: TestEventTime) {
    const calendarDay = this.newTimesCalendar.findByDataQa(
      `times-calendar-day-${date.formatIso()}`
    )
    await calendarDay
      .findByDataQa(`${date.formatIso()}-add-time-button`)
      .click()
    const dayInputs = calendarDay.findAllByDataQa(`time-input-container`)

    const timeEditor = new CalendarDayEventTimeEditor(dayInputs.nth(index))

    await timeEditor.startTimeInput.fill(eventTime.startTime)
    await timeEditor.endTimeInput.fill(eventTime.endTime)
  }

  async submit() {
    await this.submitSurveyButton.click()
  }
}

export class DiscussionSurveyReadView {
  editSurveyButton: Element
  deleteSurveyButton: Element
  surveyTimesCalendar: Element
  constructor(protected readonly page: Page) {
    this.editSurveyButton = page.findByDataQa('survey-edit-button')
    this.deleteSurveyButton = page.findByDataQa('survey-delete-button')
    this.surveyTimesCalendar = page.findByDataQa('reservation-calendar')
  }

  async waitUntilLoaded() {
    await this.page
      .findByDataQa('survey-reservation-calendar-title')
      .waitUntilVisible()
  }

  async addEventTimeForDay(date: LocalDate, eventTime: TestEventTime) {
    const calendarDay = this.surveyTimesCalendar.findByDataQa(
      `times-calendar-day-${date.formatIso()}`
    )
    await calendarDay.findByDataQa('add-time-button').click()
    const dayEditor = new ReservationDayEventTimeEditor(
      calendarDay.findByDataQa(`time-input-container`)
    )

    await dayEditor.waitUntilVisible()
    await dayEditor.startTimeInput.fill(eventTime.startTime)
    await dayEditor.endTimeInput.fill(eventTime.endTime)
    await dayEditor.submitButton.click()
  }

  async openReservationModal(index: number, date: LocalDate) {
    const calendarDay = this.surveyTimesCalendar.findByDataQa(
      `times-calendar-day-${date.formatIso()}`
    )
    const reservationRows = calendarDay.findAllByDataQa('reservation-row')
    await reservationRows
      .nth(index)
      .findByDataQa('reserve-event-time-button')
      .click()
    return new ReserveModal(this.page.findByDataQa('reservation-modal'))
  }

  async deleteSurvey() {
    await this.deleteSurveyButton.click()
    return new Modal(
      this.page.findByDataQa('deletion-confirm-modal').findByDataQa('modal')
    )
  }

  async openSurveyEditor() {
    await this.editSurveyButton.click()
    return new DiscussionSurveyEditor(this.page)
  }

  async assertSurveyTitle(expectedTitle: string) {
    await this.page.findByDataQa('survey-title').assertTextEquals(expectedTitle)
  }

  async assertSurveyDescription(expectedTitle: string) {
    await this.page
      .findByDataQa('survey-description')
      .assertTextEquals(expectedTitle)
  }

  async assertUnreservedAttendeeExists(attendeeId: string) {
    await this.page
      .findByDataQa('unreserved-attendees')
      .findByDataQa(`attendee-${attendeeId}`)
      .waitUntilVisible()
  }

  async assertReservedAttendeeExists(attendeeId: string) {
    await this.page
      .findByDataQa('reserved-attendees')
      .findByDataQa(`attendee-${attendeeId}`)
      .waitUntilVisible()
  }

  async assertEventTimeExists(
    index: number,
    date: LocalDate,
    eventTime: TestEventTime
  ) {
    const calendarDay = this.surveyTimesCalendar.findByDataQa(
      `times-calendar-day-${date.formatIso()}`
    )
    const eventTimeRanges = calendarDay.findAllByDataQa('event-time-range')

    await eventTimeRanges
      .nth(index)
      .assertTextEquals(`${eventTime.startTime} – ${eventTime.endTime}`)
  }

  async assertNoTimesExist(date: LocalDate) {
    const calendarDay = this.surveyTimesCalendar.findByDataQa(
      `times-calendar-day-${date.formatIso()}`
    )
    const eventTimeRanges = calendarDay.findAllByDataQa('event-time-range')
    await eventTimeRanges.nth(0).waitUntilHidden()
  }

  async assertReservationExists(date: LocalDate, index: number, name: string) {
    const calendarDay = this.surveyTimesCalendar.findByDataQa(
      `times-calendar-day-${date.formatIso()}`
    )
    const reservationRows = calendarDay.findAllByDataQa('reservation-row')
    await reservationRows
      .nth(index)
      .findByDataQa('reserve-event-time-button')
      .assertTextEquals(name)
  }
}

export class DiscussionSurveyEditor {
  titleInput: TextInput
  descriptionInput: TextInput
  attendeeInput: TreeDropdown
  submitButton: Element
  constructor(protected readonly page: Page) {
    this.titleInput = new TextInput(page.findByDataQa('survey-title-input'))
    this.descriptionInput = new TextInput(
      page.findByDataQa('survey-description-input')
    )
    this.attendeeInput = new TreeDropdown(
      page.findByDataQa('survey-attendees-select')
    )
    this.submitButton = page.findByDataQa('survey-editor-submit-button')
  }

  async waitUntilLoaded() {
    await this.submitButton.waitUntilVisible()
  }

  async submit() {
    await this.submitButton.click()
  }
}

export class CalendarDayEventTimeEditor extends Element {
  startTimeInput = new TextInput(this.findByDataQa('event-time-start-input'))
  endTimeInput = new TextInput(this.findByDataQa('event-time-end-input'))
}

export class ReservationDayEventTimeEditor extends CalendarDayEventTimeEditor {
  submitButton = this.findByDataQa('event-time-submit')
  deleteButton = this.findByDataQa('event-time-delete')
}

export class ReserveModal extends Element {
  submitButton = this.findByDataQa('submit-reservation-button')
  deleteButton = this.findByDataQa('delete-reservation-button')

  addReservationSelectButton = this.findByDataQa(
    'add-reservation-select-button'
  )
  reservationSelect = new Select(this.findByDataQa('reservee-select'))

  async reserveEventTimeForChild(reserveeName: string) {
    await this.addReservationSelectButton.click()
    await this.reservationSelect.selectOption(reserveeName)
    await this.submitButton.click()
  }

  async deleteReservedEventTime() {
    await this.deleteButton.click()
    const confirmDeleteModal = new Modal(
      this.findByDataQa('confirm-delete-modal').findByDataQa('modal')
    )
    await confirmDeleteModal.submit()
  }

  async deleteEventTime() {
    await this.deleteButton.click()
  }
}
