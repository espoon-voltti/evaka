// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

export type FilterTimePeriod = '1 day' | '3 months' | '6 months' | '1 year'

export class UnitFilters {
  readonly endDate: LocalDate

  constructor(
    public readonly startDate: LocalDate,
    public readonly period: FilterTimePeriod
  ) {
    switch (period) {
      case '1 day':
        this.endDate = startDate.addDays(0)
        break
      case '3 months':
        this.endDate = startDate.addMonths(3)
        break
      case '6 months':
        this.endDate = startDate.addMonths(6)
        break
      case '1 year':
        this.endDate = startDate.addYears(1)
        break
    }
  }

  withStartDate(date: LocalDate) {
    return new UnitFilters(date, this.period)
  }

  withPeriod(period: FilterTimePeriod) {
    return new UnitFilters(this.startDate, period)
  }
}
