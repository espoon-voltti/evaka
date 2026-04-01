// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import { expect } from '../../playwright'
import type { Page, Element } from '../../utils/page'

export type ReservationChildDetails = {
  firstName: string
  lastName: string
  preferredName?: string
}
export default class ConfirmedDayReservationPage {
  constructor(private readonly page: Page) {}

  dayRow = (date: LocalDate) =>
    this.page.findByDataQa(`day-item-${date.formatIso()}`)

  childItem = (date: LocalDate, childId: UUID): Element =>
    this.dayRow(date).findByDataQa(`child-${childId}`)

  async assertDayExists(date: LocalDate) {
    await expect(this.dayRow(date)).toBeVisible()
  }

  async assertDayDoesNotExist(date: LocalDate) {
    await expect(this.dayRow(date)).toBeHidden()
  }

  async assertDailyCounts(
    date: LocalDate,
    presentCount: string,
    presentCalc: string,
    absentCount: string
  ) {
    const day = this.dayRow(date)
    await expect(day.findByDataQa('present-total')).toHaveText(presentCount)
    await expect(day.findByDataQa('present-calc')).toHaveText(
      `(${presentCalc})`
    )
    await expect(day.findByDataQa('absent-total')).toHaveText(`${absentCount}`)
  }

  async openDayItem(date: LocalDate) {
    await this.dayRow(date).findByDataQa('open-day-button').click()
  }

  async assertChildDetails(
    date: LocalDate,
    childId: UUID,
    reservationTexts: string[],
    childDetails: ReservationChildDetails
  ) {
    const childItem = this.dayRow(date).findByDataQa(`child-${childId}`)
    const childPreferredName = childDetails.preferredName
      ? ` (${childDetails.preferredName})`
      : ''
    await expect(childItem.findByDataQa('child-name')).toHaveText(
      `${childDetails.firstName} ${childDetails.lastName}${childPreferredName}`
    )

    for (const [index, value] of reservationTexts.entries()) {
      await expect(
        childItem.findByDataQa(`reservation-content-${index}`)
      ).toHaveText(value)
    }
  }
}
