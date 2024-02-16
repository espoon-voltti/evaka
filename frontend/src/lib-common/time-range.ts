// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from './json'
import LocalTime from './local-time'

// eslint-disable-next-line @typescript-eslint/no-namespace
export namespace TimeRangeEndpoint {
  export class Start {
    isStart = true
    isEnd = false

    constructor(public __inner: LocalTime) {}

    compareTo(other: TimeRangeEndpoint): number {
      if (other.isStart) {
        return this.__inner.compareTo(other.__inner)
      } else if (other.isEnd) {
        if (
          this.__inner.isEqual(LocalTime.MIDNIGHT) ||
          other.__inner.isEqual(LocalTime.MIDNIGHT)
        ) {
          return -1
        }
        return this.__inner.compareTo(other.__inner)
      } else {
        throw new Error('Unknown TimeRangeEndpoint type')
      }
    }

    asStart(): Start {
      return this
    }

    asEnd(): End {
      if (this.__inner.isEqual(LocalTime.MIDNIGHT))
        throw new Error('Cannot convert midnight start to end')
      return new End(this.__inner)
    }

    /** @deprecated Use the range operations of TimeRange instead */
    asLocalTime(): LocalTime {
      return this.__inner
    }

    isBefore(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) < 0
    }

    isEqualOrBefore(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) <= 0
    }

    isEqual(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) === 0
    }

    isEqualOrAfter(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) >= 0
    }

    isAfter(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) > 0
    }
  }

  export class End {
    isStart = false
    isEnd = true

    constructor(public __inner: LocalTime) {}

    asStart(): Start {
      if (this.__inner.isEqual(LocalTime.MIDNIGHT))
        throw new Error('Cannot convert midnight end to start')
      return new Start(this.__inner)
    }

    asEnd(): End {
      return this
    }

    /** @deprecated Use the range operations of TimeRange instead */
    asLocalTime(): LocalTime {
      if (this.__inner.isEqual(LocalTime.MIDNIGHT)) {
        throw new Error('Cannot convert midnight end to local time')
      }
      return this.__inner
    }

    compareTo(other: TimeRangeEndpoint): number {
      if (other.isStart) {
        if (
          this.__inner.isEqual(LocalTime.MIDNIGHT) ||
          other.__inner.isEqual(LocalTime.MIDNIGHT)
        ) {
          return 1
        }
        return this.__inner.compareTo(other.__inner)
      } else if (other.isEnd) {
        if (
          this.__inner.isEqual(LocalTime.MIDNIGHT) &&
          other.__inner.isEqual(LocalTime.MIDNIGHT)
        ) {
          return 0
        }
        if (this.__inner.isEqual(LocalTime.MIDNIGHT)) {
          return 1
        }
        if (other.__inner.isEqual(LocalTime.MIDNIGHT)) {
          return -1
        }
        return this.__inner.compareTo(other.__inner)
      } else {
        throw new Error('Unknown TimeRangeEndpoint type')
      }
    }

    isBefore(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) < 0
    }

    isEqualOrBefore(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) <= 0
    }

    isEqual(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) === 0
    }

    isEqualOrAfter(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) >= 0
    }

    isAfter(other: TimeRangeEndpoint): boolean {
      return this.compareTo(other) > 0
    }
  }
}

export type TimeRangeEndpoint = TimeRangeEndpoint.Start | TimeRangeEndpoint.End

function minOf(a: TimeRangeEndpoint, b: TimeRangeEndpoint): TimeRangeEndpoint {
  return a.isBefore(b) ? a : b
}

function maxOf(a: TimeRangeEndpoint, b: TimeRangeEndpoint): TimeRangeEndpoint {
  return a.isAfter(b) ? a : b
}

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
