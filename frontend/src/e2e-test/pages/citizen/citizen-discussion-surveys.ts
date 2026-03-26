// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { expect } from '../../playwright'
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
    await expect(this.findAllByDataQa('discussion-time-duration')).toHaveCount(
      expectedEventTimeIds.length
    )
    for (const et of expectedEventTimeIds) {
      await expect(this.findByDataQa(`radio-${et}`)).toBeVisible()
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
    await expect(childElement).toBeVisible()

    const surveyElement = childElement.findByDataQa(
      `child-survey-${childId}-${surveyId}`
    )
    await expect(surveyElement).toBeVisible()
    await surveyElement
      .findByDataQa(`survey-title-${surveyId}`)
      .assertTextEquals(title)
    await expect(
      surveyElement.findByDataQa('open-survey-reservations-button')
    ).toBeVisible()
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
    await expect(childElement).toBeVisible()

    const surveyElement = childElement.findByDataQa(
      `child-survey-${childId}-${surveyId}`
    )
    await expect(surveyElement).toBeVisible()
    await surveyElement
      .findByDataQa(`survey-title-${surveyId}`)
      .assertTextEquals(title)

    await surveyElement
      .findByDataQa(`reservation-content-${reservationId}`)
      .assertTextEquals(reservationText)

    const cancelButton = surveyElement.findByDataQa(`reservation-cancel-button`)
    await expect(cancelButton).toBeVisible()
    await cancelButton.assertDisabled(!isCancellable)

    const exportButton = surveyElement.findByDataQa(
      `event-export-button-${reservationId}`
    )
    if (isExportable) {
      await expect(exportButton).toBeVisible()
    } else {
      await exportButton.waitUntilHidden()
    }
  }

  showReservationModal = async (surveyId: string, childId: string) => {
    const surveyElement = this.findByDataQa(
      `child-survey-${childId}-${surveyId}`
    )
    await expect(surveyElement).toBeVisible()

    const surveyReservationButton = surveyElement.findByDataQa(
      'open-survey-reservations-button'
    )
    await surveyReservationButton.click()
  }

  cancelReservation = async (surveyId: string, childId: string) => {
    const surveyElement = this.findByDataQa(
      `child-survey-${childId}-${surveyId}`
    )
    await expect(surveyElement).toBeVisible()

    const cancelButton = surveyElement.findByDataQa(`reservation-cancel-button`)
    await expect(cancelButton).toBeVisible()
    await cancelButton.assertDisabled(false)
    await cancelButton.click()
  }
}
