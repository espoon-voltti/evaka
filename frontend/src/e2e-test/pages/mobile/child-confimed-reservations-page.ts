// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from '../../utils/page'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export type ReservationChildDetails = {
  firstName: string,
  lastName: string,
  preferredName?: string
}
export default class ConfirmedDayReservationPage {
  constructor(private readonly page: Page) { }

  dayItems = this.page.findAllByDataQa('day-item')

  dayRow = (date: LocalDate) => this.page.findByDataQa(`day-item-${date.formatIso()}`)

  async assertDayExists(date: LocalDate) {
    await this.dayRow(date).waitUntilVisible()
  }

  async assertDayDoesNotExist(date: LocalDate) {
    await this.dayRow(date).waitUntilHidden()
  }

  async assertDailyCounts(date: LocalDate, presentCount: string, presentCalc: string, absentCount: string) {
    const day = this.dayRow(date)
    await day
      .findByDataQa('present-total').assertTextEquals(presentCount)
    await day
      .findByDataQa('present-calc').assertTextEquals(`(${presentCalc})`)
    await day
      .findByDataQa('absent-total').assertTextEquals(`${absentCount}`)
  }

  async openDayItem(date: LocalDate) {
    await this.dayRow(date)
      .findByDataQa('open-day-button')
      .click()
  }

  async assertChildItemExists(date: LocalDate, childId: UUID) {
    await this.dayRow(date).findByDataQa(`child-item-${childId}`).waitUntilVisible()
  }

  async assertChildDetails(date: LocalDate, childId: UUID, reservationTexts: string[], childDetails: ReservationChildDetails) {
    const childItem = this.dayRow(date).findByDataQa(`child-item-${childId}`)
    const childPreferredName = childDetails.preferredName ? ` (${childDetails.preferredName})` : ''
    await childItem
      .findByDataQa('child-name').assertTextEquals(`${childDetails.firstName} ${childDetails.lastName}${childPreferredName}`)
    
    for (const [rt, index] of reservationTexts) {
      await childItem.findByDataQa(`reservation-content-${index}`).assertTextEquals(rt)
    }
  }
}
