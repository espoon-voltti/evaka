// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DateFormat } from './date'
import FiniteDateRange from './finite-date-range'
import { JsonOf } from './json'
import LocalDate from './local-date'
import { maxOf, minOf } from './ordered'

export type Tense = 'past' | 'present' | 'future'

export default class DateRange {
  constructor(
    readonly start: LocalDate,
    readonly end: LocalDate | null
  ) {
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

  format(datePattern?: DateFormat): string {
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

  overlapsWith(other: DateRange): boolean {
    return (
      (this.end === null || !this.end.isBefore(other.start)) &&
      (other.end === null || !other.end.isBefore(this.start))
    )
  }

  includes(date: LocalDate): boolean {
    if (this.end) {
      return new FiniteDateRange(this.start, this.end).includes(date)
    }

    return !this.start.isAfter(date)
  }

  contains(other: DateRange | FiniteDateRange): boolean {
    if (this.end !== null) {
      if (other.end === null || other.end.isAfter(this.end)) return false
    }
    return !this.start.isAfter(other.start)
  }

  intersection(other: DateRange): DateRange | undefined {
    const start = maxOf(this.start, other.start)
    const end = !this.end || !other.end ? null : minOf(this.end, other.end)
    if (end && start.isAfter(end)) return undefined
    return new DateRange(start, end)
  }

  complement(other: DateRange): DateRange[] {
    if (!this.overlapsWith(other)) return [new DateRange(this.start, this.end)]

    return [
      ...(this.start < other.start
        ? [new DateRange(this.start, other.start.subDays(1))]
        : []),
      ...(other.end !== null && (this.end === null || this.end > other.end)
        ? [new DateRange(other.end.addDays(1), this.end)]
        : [])
    ]
  }

  spanningRange(other: FiniteDateRange | DateRange): DateRange {
    const start = minOf(this.start, other.start)
    const end = this.end && other.end ? maxOf(this.end, other.end) : null
    return new DateRange(start, end)
  }

  tenseAt(date: LocalDate): Tense {
    if (this.start.isAfter(date)) {
      return 'future'
    }
    if (this.end !== null && this.end.isBefore(date)) {
      return 'past'
    }
    return 'present'
  }

  static parseJson(json: JsonOf<DateRange>): DateRange {
    return new DateRange(
      LocalDate.parseIso(json.start),
      json.end ? LocalDate.parseIso(json.end) : null
    )
  }
}
