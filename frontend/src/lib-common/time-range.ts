// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from './json'
import LocalTime from './local-time'
import { maxOf, minOf } from './ordered'
import TimeRangeEndpoint from './time-range-endpoint'

export default class TimeRange {
  start: TimeRangeEndpoint.Start
  end: TimeRangeEndpoint.End

  constructor(start: LocalTime, end: LocalTime)
  constructor(start: TimeRangeEndpoint.Start, end: TimeRangeEndpoint.End)
  constructor(
    start: LocalTime | TimeRangeEndpoint.Start,
    end: LocalTime | TimeRangeEndpoint.End
  ) {
    if (start instanceof LocalTime) {
      start = new TimeRangeEndpoint.Start(start)
    }
    if (end instanceof LocalTime) {
      end = new TimeRangeEndpoint.End(end)
    }
    this.start = start
    this.end = end

    if (start.isEqualOrAfter(end)) {
      throw new Error('TimeRange start must be before end')
    }
  }

  static tryCreate(start: LocalTime, end: LocalTime): TimeRange | undefined {
    try {
      return new TimeRange(start, end)
    } catch (e) {
      return undefined
    }
  }

  isEqual(other: TimeRange): boolean {
    return this.start.isEqual(other.start) && this.end.isEqual(other.end)
  }

  contains(other: TimeRange): boolean {
    return (
      this.start.isEqualOrBefore(other.start) &&
      other.end.isEqualOrBefore(this.end)
    )
  }

  intersection(other: TimeRange): TimeRange | undefined {
    const start = maxOf(this.start, other.start)
    const end = minOf(this.end, other.end)
    try {
      return new TimeRange(start.asStart(), end.asEnd())
    } catch (e) {
      return undefined
    }
  }

  static intersection(ranges: TimeRange[]): TimeRange | undefined {
    if (ranges.length === 0) return undefined
    return (ranges as (TimeRange | undefined)[]).reduce(
      (acc: TimeRange | undefined, range) => acc?.intersection(range!)
    )
  }

  includes(time: LocalTime): boolean
  includes(time: TimeRangeEndpoint): boolean
  includes(time: LocalTime | TimeRangeEndpoint): boolean {
    if (time instanceof LocalTime) {
      const t = new TimeRangeEndpoint.Start(time)
      return t.isEqualOrAfter(this.start) && t.isBefore(this.end)
    } else if (time.isStart) {
      return time.isEqualOrAfter(this.start) && time.isBefore(this.end)
    } else if (time.isEnd) {
      return time.isAfter(this.start) && time.isEqualOrBefore(this.end)
    } else {
      throw new Error('Unknown TimeRangeEndpoint type')
    }
  }

  formatStart(): string {
    return this.start.__inner.format()
  }

  formatEnd(): string {
    return this.end.__inner.format()
  }

  format(): string {
    return `${this.formatStart()}–${this.formatEnd()}`
  }

  __thisIsTimeRange(): void {
    // This method makes TypeScript understand that this type is separate from TimeInterval
  }

  /** HH:mm:ss */
  static parseJson(json: JsonOf<TimeRange>): TimeRange {
    return new TimeRange(
      LocalTime.parseIso(json.start),
      LocalTime.parseIso(json.end)
    )
  }

  /** HH:mm */
  static parse(json: JsonOf<TimeRange>): TimeRange {
    return new TimeRange(LocalTime.parse(json.start), LocalTime.parse(json.end))
  }

  toJSON(): JsonOf<TimeRange> {
    return {
      start: this.start.__inner.toJSON(),
      end: this.end.__inner.toJSON()
    }
  }
}
