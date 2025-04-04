// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Element } from '../../utils/page'

export class DiscussionReservationModal extends Element {
  #modalSendButton: Element

  constructor(el: Element) {
    super(el)
    this.#modalSendButton = this.findByDataQa('modal-okBtn')
  }

  reserveEventTime = async (eventTimeId: string) => {
    const eventTimeRadio = this.findByDataQa(`radio-${eventTimeId}`)
    await eventTimeRadio.click()
    await this.save()
  }

  assertEventTimeNotShown = async (eventTimeId: string) => {
    await this.findByDataQa(`radio-${eventTimeId}`).waitUntilHidden()
  }

  assertEventTimes = async (expectedEventTimeIds: string[]) => {
    await this.findAllByDataQa('discussion-time-duration').assertCount(
      expectedEventTimeIds.length
    )
    for (const et of expectedEventTimeIds) {
      await this.findByDataQa(`radio-${et}`).waitUntilVisible()
    }
  }

  async save() {
    await this.#modalSendButton.click()
  }
}

export class DiscussionSurveyModal extends Element {
  constructor(el: Element) {
    super(el)
  }

  assertChildSurvey = async (
    surveyId: string,
    childId: string,
    title: string
  ) => {
    const childElement = this.findByDataQa(`discussion-child-${childId}`)
    await childElement.waitUntilVisible()

    const surveyElement = childElement.findByDataQa(
      `child-survey-${childId}-${surveyId}`
    )
    await surveyElement.waitUntilVisible()
    await surveyElement
      .findByDataQa(`survey-title-${surveyId}`)
      .assertTextEquals(title)
    await surveyElement
      .findByDataQa('open-survey-reservations-button')
      .waitUntilVisible()
  }

  assertChildReservation = async (
    surveyId: string,
    childId: string,
    title: string,
    reservationId: string,
    reservationText: string,
    isCancellable: boolean,
    isExportable = true
  ) => {
    const childElement = this.findByDataQa(`discussion-child-${childId}`)
    await childElement.waitUntilVisible()

    const surveyElement = childElement.findByDataQa(
      `child-survey-${childId}-${surveyId}`
    )
    await surveyElement.waitUntilVisible()
    await surveyElement
      .findByDataQa(`survey-title-${surveyId}`)
      .assertTextEquals(title)

    await surveyElement
      .findByDataQa(`reservation-content-${reservationId}`)
      .assertTextEquals(reservationText)

    const cancelButton = surveyElement.findByDataQa(`reservation-cancel-button`)
    await cancelButton.waitUntilVisible()
    await cancelButton.assertDisabled(!isCancellable)

    const exportButton = surveyElement.findByDataQa(
      `event-export-button-${reservationId}`
    )
    if (isExportable) {
      await exportButton.waitUntilVisible()
    } else {
      await exportButton.waitUntilHidden()
    }
  }

  showReservationModal = async (surveyId: string, childId: string) => {
    const surveyElement = this.findByDataQa(
      `child-survey-${childId}-${surveyId}`
    )
    await surveyElement.waitUntilVisible()

    const surveyReservationButton = surveyElement.findByDataQa(
      'open-survey-reservations-button'
    )
    await surveyReservationButton.click()
  }

  cancelReservation = async (surveyId: string, childId: string) => {
    const surveyElement = this.findByDataQa(
      `child-survey-${childId}-${surveyId}`
    )
    await surveyElement.waitUntilVisible()

    const cancelButton = surveyElement.findByDataQa(`reservation-cancel-button`)
    await cancelButton.waitUntilVisible()
    await cancelButton.assertDisabled(false)
    await cancelButton.click()
  }
}
