// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from './json'
import LocalTime from './local-time'
import { maxOf, minOf } from './ordered'
import TimeRange from './time-range'
import TimeRangeEndpoint from './time-range-endpoint'

export default class TimeInterval {
  start: TimeRangeEndpoint.Start
  end: TimeRangeEndpoint.End | null

  constructor(start: LocalTime, end: LocalTime | null)
  constructor(start: TimeRangeEndpoint.Start, end: TimeRangeEndpoint.End | null)
  constructor(
    start: LocalTime | TimeRangeEndpoint.Start,
    end: LocalTime | TimeRangeEndpoint.End | null
  ) {
    if (start instanceof LocalTime) {
      start = new TimeRangeEndpoint.Start(start)
    }
    if (end instanceof LocalTime) {
      end = new TimeRangeEndpoint.End(end)
    }
    this.start = start
    this.end = end

    if (end !== null && start.isEqualOrAfter(end)) {
      throw new Error('TimeInterval start must be before end')
    }
  }

  isEqual(other: TimeInterval): boolean {
    return (
      this.start.isEqual(other.start) &&
      ((this.end === null && other.end === null) ||
        (this.end !== null &&
          other.end !== null &&
          this.end.isEqual(other.end)))
    )
  }

  intersection(other: TimeInterval): TimeInterval | undefined
  intersection(other: TimeRange): TimeInterval | undefined
  intersection(other: TimeInterval | TimeRange): TimeInterval | undefined {
    const start = maxOf(this.start, other.start).asStart()
    const end =
      this.end === null
        ? other.end
        : other.end === null
          ? this.end
          : minOf(this.end, other.end)
    try {
      return new TimeInterval(start, end)
    } catch (e) {
      return undefined
    }
  }

  overlaps(other: TimeInterval): boolean
  overlaps(other: TimeRange): boolean
  overlaps(other: TimeInterval | TimeRange): boolean {
    return (
      (other.end === null || this.start.isBefore(other.end)) &&
      (this.end === null || other.start.isBefore(this.end))
    )
  }

  asTimeRange(): TimeRange | undefined {
    if (!this.end) return undefined
    return new TimeRange(this.start, this.end)
  }

  static tryCreate(
    start: LocalTime,
    end: LocalTime | null
  ): TimeInterval | undefined {
    try {
      return new TimeInterval(start, end)
    } catch (e) {
      return undefined
    }
  }

  formatStart(): string {
    return this.start.__inner.format()
  }

  formatEnd(): string {
    return this.end ? this.end.__inner.format() : ''
  }

  format(): string {
    return `${this.formatStart()}–${this.formatEnd()}`
  }

  static parseJson(json: JsonOf<TimeInterval>): TimeInterval {
    return new TimeInterval(
      LocalTime.parseIso(json.start),
      json.end ? LocalTime.parseIso(json.end) : null
    )
  }

  toJSON(): JsonOf<TimeInterval> {
    return {
      start: this.start.__inner.formatIso(),
      end: this.end ? this.end.__inner.formatIso() : null
    }
  }

  __thisIsTimeInterval(): void {
    // This method makes TypeScript understand that this type is separate from TimeRange
  }
}
