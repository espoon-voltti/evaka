// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'

import { DateFormat } from './date'
import DateRange from './date-range'
import { JsonOf } from './json'
import LocalDate from './local-date'
import { groupDatesToRanges } from './utils/local-date'
import YearMonth from './year-month'

function maxOf(a: LocalDate, b: LocalDate) {
  return a.isAfter(b) ? a : b
}

function minOf(a: LocalDate, b: LocalDate) {
  return a.isBefore(b) ? a : b
}

interface DeprecatedRange {
  startDate: LocalDate
  endDate: LocalDate
}

export default class FiniteDateRange {
  constructor(
    readonly start: LocalDate,
    readonly end: LocalDate
  ) {
    if (end.isBefore(start)) {
      throw new Error(
        `Attempting to initialize invalid finite date range with start: ${start.formatIso()}, end: ${end.formatIso()}`
      )
    }
  }

  static tryCreate(
    start: LocalDate,
    end: LocalDate
  ): FiniteDateRange | undefined {
    return end.isBefore(start) ? undefined : new FiniteDateRange(start, end)
  }

  static from(objectWithRange: DeprecatedRange) {
    return new FiniteDateRange(
      objectWithRange.startDate,
      objectWithRange.endDate
    )
  }

  withStart(start: LocalDate): FiniteDateRange {
    return new FiniteDateRange(start, this.end)
  }

  withEnd(end: LocalDate): FiniteDateRange {
    return new FiniteDateRange(this.start, end)
  }

  asDateRange(): DateRange {
    return new DateRange(this.start, this.end)
  }

  /**
   * Returns true if this date range fully contains the given date range.
   */
  contains(other: FiniteDateRange | DateRange): boolean {
    if (other.end) {
      return this.start <= other.start && other.end <= this.end
    } else {
      return false
    }
  }

  /**
   * Returns true if this date range includes the given date.
   */
  includes(date: LocalDate): boolean {
    return !this.start.isAfter(date) && !this.end.isBefore(date)
  }

  /**
   * Returns true if this date range overlaps at least partially with the given date range.
   */
  overlaps(other: FiniteDateRange | DateRange): boolean {
    if (other.end) {
      return (
        this.start.isEqualOrBefore(other.end) &&
        other.start.isEqualOrBefore(this.end)
      )
    } else {
      return other.start <= this.end
    }
  }

  leftAdjacentTo(other: FiniteDateRange | DateRange): boolean {
    return this.end.addDays(1).isEqual(other.start)
  }

  rightAdjacentTo(other: FiniteDateRange | DateRange): boolean {
    if (other.end) {
      return other.end.addDays(1).isEqual(this.start)
    } else {
      return false
    }
  }

  adjacentTo(other: FiniteDateRange | DateRange): boolean {
    return this.leftAdjacentTo(other) || this.rightAdjacentTo(other)
  }

  intersection(
    other: FiniteDateRange | DateRange
  ): FiniteDateRange | undefined {
    const start = maxOf(this.start, other.start)
    const end = other.end ? minOf(this.end, other.end) : this.end
    if (start.isAfter(end)) return undefined
    return new FiniteDateRange(start, end)
  }

  complement(other: FiniteDateRange | DateRange): FiniteDateRange[] {
    if (!this.overlaps(other))
      return [new FiniteDateRange(this.start, this.end)]

    return [
      ...(this.start < other.start
        ? [new FiniteDateRange(this.start, other.start.subDays(1))]
        : []),
      ...(other.end !== null && this.end > other.end
        ? [new FiniteDateRange(other.end.addDays(1), this.end)]
        : [])
    ]
  }

  /**
   * Returns an iterable containing all dates included in this date range.
   */
  dates(): Iterable<LocalDate> {
    function* dateIterator(start: LocalDate, end: LocalDate) {
      let current = start
      while (!current.isAfter(end)) {
        yield current
        current = current.addDays(1)
      }
    }

    return dateIterator(this.start, this.end)
  }

  /**
   * Returns the total duration of this date range counted in days.
   */
  durationInDays(): number {
    return this.end.addDays(1).differenceInDays(this.start) // adjust to exclusive range
  }

  format(datePattern?: DateFormat): string {
    return `${this.start.format(datePattern)} - ${this.end.format(datePattern)}`
  }

  formatCompact(whitespace = ''): string {
    const join = (start: string, end: string) =>
      `${start}${whitespace}-${whitespace}${end}`

    if (this.start.getYear() !== this.end.getYear()) {
      return join(this.start.format(), this.end.format())
    }

    if (this.start.getMonth() !== this.end.getMonth()) {
      return join(this.start.format('dd.MM.'), this.end.format())
    }

    if (this.start.getDate() !== this.end.getDate()) {
      return join(this.start.formatExotic('dd.'), this.end.format())
    }

    return this.start.format()
  }

  toString(): string {
    return `[${this.start.formatIso()}, ${this.end.formatIso()}]`
  }

  isEqual(other: FiniteDateRange): boolean {
    return this.start.isEqual(other.start) && this.end.isEqual(other.end)
  }

  getGaps(childRanges: FiniteDateRange[]): FiniteDateRange[] {
    const gapDates = [...this.dates()].filter((date) =>
      childRanges.every((range) => !range.includes(date))
    )
    return groupDatesToRanges(gapDates)
  }

  static parseJson(json: JsonOf<FiniteDateRange>): FiniteDateRange {
    return new FiniteDateRange(
      LocalDate.parseIso(json.start),
      LocalDate.parseIso(json.end)
    )
  }

  static ofMonth(target: YearMonth | LocalDate) {
    if ('date' in target) {
      return new FiniteDateRange(target.withDate(1), target.lastDayOfMonth())
    } else {
      return new FiniteDateRange(target.atDay(1), target.atEndOfMonth())
    }
  }
}

export function mergeDateRanges(ranges: FiniteDateRange[]): FiniteDateRange[] {
  const sorted = sortBy(ranges, ({ start }) => start.formatIso())
  const first = sorted[0]
  if (!first) {
    return []
  }

  const merged = [first]
  for (let i = 1; i < sorted.length; i++) {
    const previous = merged[merged.length - 1]
    const current = sorted[i]
    if (previous.end.addDays(1).isEqualOrAfter(current.start)) {
      merged[merged.length - 1] = new FiniteDateRange(
        previous.start,
        current.end
      )
    } else {
      merged[merged.length] = current
    }
  }

  return merged
}
