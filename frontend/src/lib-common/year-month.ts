// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from './local-date'

export default class YearMonth {
  constructor(
    readonly year: number,
    readonly month: number
  ) {
    if (month < 1 || month > 12) {
      throw new Error('Month must be between 1 and 12')
    }
  }

  formatIso(): string {
    return `${this.year.toString().padStart(4, '0')}-${this.month.toString().padStart(2, '0')}`
  }

  format(): string {
    return `${this.month.toString().padStart(2, '0')}/${this.year.toString().padStart(4, '0')}`
  }

  atDay(day: number): LocalDate {
    return LocalDate.of(this.year, this.month, day)
  }

  atEndOfMonth(): LocalDate {
    return LocalDate.of(this.year, this.month, 1).addMonths(1).subDays(1)
  }

  addMonths(months: number): YearMonth {
    const totalMonths = this.year * 12 + this.month - 1
    const newTotalMonths = totalMonths + months
    return new YearMonth(
      Math.floor(newTotalMonths / 12),
      (newTotalMonths % 12) + 1
    )
  }

  subMonths(months: number): YearMonth {
    return this.addMonths(-months)
  }

  addYears(years: number): YearMonth {
    return new YearMonth(this.year + years, this.month)
  }

  subYears(years: number): YearMonth {
    return this.addYears(-years)
  }

  toJSON(): string {
    return this.formatIso()
  }

  static parseIso(iso: string): YearMonth {
    const match = new RegExp(/^(\d{4})-(\d{2})$/).exec(iso)
    if (!match) {
      throw new Error('Invalid ISO year-month')
    }
    return new YearMonth(parseInt(match[1]), parseInt(match[2]))
  }

  static ofDate(date: LocalDate): YearMonth {
    return new YearMonth(date.year, date.month)
  }

  static todayInHelsinkiTz(): YearMonth {
    const today = LocalDate.todayInHelsinkiTz()
    return new YearMonth(today.year, today.month)
  }
}
