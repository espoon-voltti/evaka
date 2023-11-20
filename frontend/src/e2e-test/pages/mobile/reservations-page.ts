// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { Page, TextInput } from '../../utils/page'

export default class MobileReservationsPage {
  editButton
  confirmButton

  constructor(private readonly page: Page) {
    this.editButton = page.findByDataQa('edit')
    this.confirmButton = page.findByDataQa('confirm')
  }

  assertReservations = async (
    expected: {
      date: LocalDate
      text: string
    }[]
  ) => {
    await Promise.all(
      expected.map(async ({ date, text }) => {
        const reservationDate = this.#findReservationDate(date)
        await reservationDate
          .findByDataQa('reservation-text')
          .assertTextEquals(text)
      })
    )
  }

  fillTime = async (
    date: LocalDate,
    index: number,
    startTime: string,
    endTime: string
  ) => {
    const reservationDate = this.#findReservationDate(date)
    const reservationTime = reservationDate
      .findAllByDataQa('reservation-time')
      .nth(index)
    await new TextInput(
      reservationTime.findByDataQa('reservation-start-time')
    ).fill(startTime)
    await new TextInput(
      reservationTime.findByDataQa('reservation-end-time')
    ).fill(endTime)
  }

  addTime = async (date: LocalDate) => {
    const reservationDate = this.#findReservationDate(date)
    await reservationDate.findByDataQa('reservation-time-add').click()
  }

  removeTime = async (date: LocalDate, index: number) => {
    const reservationDate = this.#findReservationDate(date)
    const reservationTime = reservationDate
      .findAllByDataQa('reservation-time')
      .nth(index)
    await reservationTime.findByDataQa('reservation-time-remove').click()
  }

  removeAbsence = async (date: LocalDate) => {
    const reservationDate = this.#findReservationDate(date)
    await reservationDate.findByDataQa('remove-absence').click()
  }

  #findReservationDate = (date: LocalDate) =>
    this.page.findByDataQa(`reservation-date-${date.formatIso()}`)
}
