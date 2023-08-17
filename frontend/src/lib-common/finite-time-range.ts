// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalTime from './local-time'

function maxOf(a: LocalTime, b: LocalTime) {
  return a.isAfter(b) ? a : b
}

function minOf(a: LocalTime, b: LocalTime) {
  return a.isBefore(b) ? a : b
}

export default class FiniteTimeRange {
  constructor(
    readonly start: LocalTime,
    readonly end: LocalTime
  ) {
    if (end.isBefore(start)) {
      throw new Error(
        `Attempting to initialize invalid finite time range with start: ${start.formatIso()}, end: ${end.formatIso()}`
      )
    }
  }

  static tryCreate(
    start: LocalTime,
    end: LocalTime
  ): FiniteTimeRange | undefined {
    return end.isBefore(start) ? undefined : new FiniteTimeRange(start, end)
  }

  withStart(start: LocalTime): FiniteTimeRange {
    return new FiniteTimeRange(start, this.end)
  }

  withEnd(end: LocalTime): FiniteTimeRange {
    return new FiniteTimeRange(this.start, end)
  }

  /**
   * Returns true if this time range fully contains the given time range.
   */
  contains(other: FiniteTimeRange): boolean {
    if (other.end) {
      return this.start <= other.start && other.end <= this.end
    } else {
      return false
    }
  }

  /**
   * Returns true if this time range includes the given time.
   */
  includes(time: LocalTime): boolean {
    return !this.start.isAfter(time) && !this.end.isBefore(time)
  }

  /**
   * Returns true if this time range overlaps at least partially with the given time range.
   */
  overlaps(other: FiniteTimeRange): boolean {
    if (other.end) {
      return (
        this.start.isEqualOrBefore(other.end) &&
        other.start.isEqualOrBefore(this.end)
      )
    } else {
      return other.start <= this.end
    }
  }

  intersection(other: FiniteTimeRange): FiniteTimeRange | undefined {
    const start = maxOf(this.start, other.start)
    const end = minOf(this.end, other.end)
    if (start.isAfter(end)) return undefined
    return new FiniteTimeRange(start, end)
  }

  /**
   * Returns an iterable containing all times included in this time range.
   */
  hours(): Iterable<LocalTime> {
    function* hourIterator(start: LocalTime, end: LocalTime) {
      let current = start
      while (!current.isAfter(end)) {
        yield current
        current = current.addHours(1)
      }
    }

    return hourIterator(this.start, this.end)
  }

  format(pattern = 'HH:mm'): string {
    return `${this.start.format(pattern)} - ${this.end.format(pattern)}`
  }

  toString(): string {
    return `[${this.start.formatIso()}, ${this.end.formatIso()}]`
  }

  isEqual(other: FiniteTimeRange): boolean {
    return this.start.isEqual(other.start) && this.end.isEqual(other.end)
  }

  /* [09:00, 09:59] is before [10:00, 11:00], and [09:00, 10:00] is before and connected */
  isBeforeMaybeConnected(other: FiniteTimeRange): boolean {
    return this.end.isEqualOrBefore(other.start)
  }

  /* [09:01, 10:00] is after of [08:00, 09:00], and [09:00, 10:00] is after and connected */
  isAfterMaybeConnected(other: FiniteTimeRange): boolean {
    return this.start.isEqualOrAfter(other.end)
  }
}
