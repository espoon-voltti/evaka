// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

import { waitUntilEqual } from '../../../utils'
import { Page } from '../../../utils/page'

import { UnitCalendarEventsSection } from './unit'

/** Common elements and actions for both month and week calendar pages */
export class UnitCalendarPageBase {
  constructor(protected readonly page: Page) {}

  async waitUntilLoaded() {
    await this.page
      .find('[data-qa="unit-attendances"][data-isloading="false"]')
      .waitUntilVisible()
  }

  monthModeButton = this.page.findByDataQa('choose-calendar-mode-month')
  weekModeButton = this.page.findByDataQa('choose-calendar-mode-week')
  nextWeekButton = this.page.findByDataQa('next-week')
  previousWeekButton = this.page.findByDataQa('previous-week')

  calendarEventsSection = new UnitCalendarEventsSection(this.page)

  private async getSelectedDateRange() {
    const rawRange = await this.page
      .find('[data-qa-date-range]')
      .getAttribute('data-qa-date-range')

    if (!rawRange) throw Error('Week range cannot be found')

    const [start, end] = rawRange
      .replace(/^\[/, '')
      .replace(/\]$/, '')
      .split(', ')
    return new FiniteDateRange(
      LocalDate.parseIso(start),
      LocalDate.parseIso(end)
    )
  }

  async changeWeekToDate(date: LocalDate) {
    for (let i = 0; i < 50; i++) {
      const currentRange = await this.getSelectedDateRange()
      if (currentRange.includes(date)) return

      await (
        currentRange.start.isBefore(date)
          ? this.nextWeekButton
          : this.previousWeekButton
      ).click()
      await this.waitForWeekLoaded()
    }
    throw Error(`Unable to seek to date ${date.formatIso()}`)
  }

  async waitForWeekLoaded() {
    await this.page
      .find('[data-qa="staff-attendances-status"][data-isloading="false"]')
      .waitUntilVisible()
  }

  async assertDateRange(expectedRange: FiniteDateRange) {
    await waitUntilEqual(() => this.getSelectedDateRange(), expectedRange)
  }
}
