// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateTimeRange from './finite-date-time-range'
import HelsinkiDateTime from './helsinki-date-time'
import type { JsonOf } from './json'
import { maxOf, minOf } from './ordered'

/**
 * A datetime range with an inclusive start and an exclusive end. A null end means no end.
 */
export default class DateTimeRange {
  constructor(
    readonly start: HelsinkiDateTime,
    readonly end: HelsinkiDateTime | null
  ) {
    if (end?.isBefore(start)) {
      throw new Error(
        `Attempting to initialize invalid datetime range with start: ${start.formatIso()}, end: ${end.formatIso()}`
      )
    }
  }

  static parseJson(json: JsonOf<DateTimeRange>): DateTimeRange {
    return new DateTimeRange(
      HelsinkiDateTime.parseIso(json.start),
      HelsinkiDateTime.parseNullableIso(json.end)
    )
  }

  toJSON(): JsonOf<DateTimeRange> {
    return { start: this.start.formatIso(), end: this.end?.formatIso() ?? null }
  }

  overlaps(other: DateTimeRange | FiniteDateTimeRange): boolean {
    return (
      (other.end === null || this.start.isBefore(other.end)) &&
      (this.end === null || other.start.isBefore(this.end))
    )
  }

  includes(value: HelsinkiDateTime): boolean {
    return (
      this.start.isEqualOrBefore(value) &&
      (this.end === null || value.isBefore(this.end))
    )
  }

  contains(other: DateTimeRange | FiniteDateTimeRange): boolean {
    return (
      this.start.isEqualOrBefore(other.start) &&
      (this.end === null ||
        (other.end !== null && other.end.isEqualOrBefore(this.end)))
    )
  }

  /** Returns undefined if the ranges do not overlap */
  intersection(other: FiniteDateTimeRange): FiniteDateTimeRange | undefined
  intersection(other: DateTimeRange): DateTimeRange | undefined
  intersection(
    other: DateTimeRange | FiniteDateTimeRange
  ): DateTimeRange | FiniteDateTimeRange | undefined {
    if (other instanceof FiniteDateTimeRange) return other.intersection(this)
    const start = maxOf(this.start, other.start)
    const end =
      this.end === null
        ? other.end
        : other.end === null
          ? this.end
          : minOf(this.end, other.end)
    return end === null || start.isBefore(end)
      ? new DateTimeRange(start, end)
      : undefined
  }
}
