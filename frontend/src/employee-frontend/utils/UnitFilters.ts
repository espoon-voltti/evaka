// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '@evaka/lib-common/local-date'
import { UnitFiltersType } from '../types/unit'

export type FilterTimePeriod = '1 day' | '3 months' | '6 months' | '1 year'

export class UnitFilters implements UnitFiltersType {
  private readonly _startDate: LocalDate
  private readonly _endDate: LocalDate
  private readonly _period: FilterTimePeriod

  constructor(startDate: LocalDate, period: FilterTimePeriod) {
    this._startDate = startDate
    this._period = period
    switch (period) {
      case '1 day':
        this._endDate = startDate.addDays(0)
        break
      case '3 months':
        this._endDate = startDate.addMonths(3)
        break
      case '6 months':
        this._endDate = startDate.addMonths(6)
        break
      case '1 year':
        this._endDate = startDate.addYears(1)
        break
    }
  }

  withStartDate(date: LocalDate) {
    return new UnitFilters(date, this._period)
  }

  withPeriod(period: FilterTimePeriod) {
    return new UnitFilters(this._startDate, period)
  }

  get startDate(): LocalDate {
    return this._startDate
  }

  get endDate(): LocalDate {
    return this._endDate
  }

  get period(): FilterTimePeriod {
    return this._period
  }
}
