// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateTimeRange from './date-time-range'
import HelsinkiDateTime from './helsinki-date-time'
import type { JsonOf } from './json'
import type LocalDate from './local-date'
import { maxOf, minOf } from './ordered'

/** A datetime range with an inclusive start and an exclusive end */
export default class FiniteDateTimeRange {
  constructor(
    readonly start: HelsinkiDateTime,
    readonly end: HelsinkiDateTime
  ) {
    if (end.isBefore(start)) {
      throw new Error(
        `Attempting to initialize invalid datetime range with start: ${start.formatIso()}, end: ${end.formatIso()}`
      )
    }
  }

  /** Returns undefined if the range would be empty */
  static tryCreate(
    start: HelsinkiDateTime,
    end: HelsinkiDateTime
  ): FiniteDateTimeRange | undefined {
    return start.isBefore(end) ? new FiniteDateTimeRange(start, end) : undefined
  }

  /** Returns the range from the start of the date to the start of the next date */
  static ofDate(date: LocalDate): FiniteDateTimeRange {
    return new FiniteDateTimeRange(
      date.startOfDay(),
      date.addDays(1).startOfDay()
    )
  }

  asDateTimeRange(): DateTimeRange {
    return new DateTimeRange(this.start, this.end)
  }

  overlaps(other: FiniteDateTimeRange | DateTimeRange): boolean {
    return this.asDateTimeRange().overlaps(other)
  }

  includes(value: HelsinkiDateTime): boolean {
    return this.start.isEqualOrBefore(value) && value.isBefore(this.end)
  }

  contains(other: FiniteDateTimeRange | DateTimeRange): boolean {
    return (
      this.start.isEqualOrBefore(other.start) &&
      other.end !== null &&
      other.end.isEqualOrBefore(this.end)
    )
  }

  /** Returns undefined if the ranges do not overlap */
  intersection(
    other: FiniteDateTimeRange | DateTimeRange
  ): FiniteDateTimeRange | undefined {
    return FiniteDateTimeRange.tryCreate(
      maxOf(this.start, other.start),
      other.end === null ? this.end : minOf(this.end, other.end)
    )
  }

  durationInMinutes(): number {
    return (this.end.timestamp - this.start.timestamp) / 60_000
  }

  static parseJson(json: JsonOf<FiniteDateTimeRange>): FiniteDateTimeRange {
    return new FiniteDateTimeRange(
      HelsinkiDateTime.parseIso(json.start),
      HelsinkiDateTime.parseIso(json.end)
    )
  }

  toJSON(): JsonOf<FiniteDateTimeRange> {
    return { start: this.start.formatIso(), end: this.end.formatIso() }
  }
}
