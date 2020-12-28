// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from './local-date'
import { JsonOf } from './json'

export default class DateRange {
  constructor(readonly start: LocalDate, readonly end: LocalDate | null) {
    if (end && end.isBefore(start)) {
      throw new Error(
        `Attempting to initialize invalid date range with start: ${start.formatIso()}, end: ${end.formatIso()}`
      )
    }
  }

  withStart(start: LocalDate): DateRange {
    return new DateRange(start, this.end)
  }

  withEnd(end: LocalDate | null): DateRange {
    return new DateRange(this.start, end)
  }

  format(datePattern?: string): string {
    return `${this.start.format(datePattern)} - ${
      this.end?.format(datePattern) ?? ''
    }`
  }

  toString(): string {
    return `[${this.start.formatIso()}, ${this.end?.formatIso() ?? ''}]`
  }

  isEqual(other: DateRange): boolean {
    if (!this.start.isEqual(other.start)) return false
    if (this.end === null) {
      return other.end === null
    } else if (other.end === null) {
      return false
    } else {
      return this.end.isEqual(other.end)
    }
  }

  static parseJson(json: JsonOf<DateRange>): DateRange {
    return new DateRange(
      LocalDate.parseIso(json.start),
      json.end ? LocalDate.parseIso(json.end) : null
    )
  }
}
