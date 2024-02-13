// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from './json'
import LocalTime from './local-time'

// eslint-disable-next-line @typescript-eslint/no-namespace
namespace MidnightAwareTime {
  export class Start {
    isStart = true
    isEnd = false

    constructor(public __inner: LocalTime) {}

    compareTo(other: MidnightAwareTime): number {
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
        throw new Error('Unknown MidnightAwareTime type')
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

    isBefore(other: MidnightAwareTime): boolean {
      return this.compareTo(other) < 0
    }

    isEqualOrBefore(other: MidnightAwareTime): boolean {
      return this.compareTo(other) <= 0
    }

    isEqual(other: MidnightAwareTime): boolean {
      return this.compareTo(other) === 0
    }

    isEqualOrAfter(other: MidnightAwareTime): boolean {
      return this.compareTo(other) >= 0
    }

    isAfter(other: MidnightAwareTime): boolean {
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

    compareTo(other: MidnightAwareTime): number {
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
        throw new Error('Unknown MidnightAwareTime type')
      }
    }

    isBefore(other: MidnightAwareTime): boolean {
      return this.compareTo(other) < 0
    }

    isEqualOrBefore(other: MidnightAwareTime): boolean {
      return this.compareTo(other) <= 0
    }

    isEqual(other: MidnightAwareTime): boolean {
      return this.compareTo(other) === 0
    }

    isEqualOrAfter(other: MidnightAwareTime): boolean {
      return this.compareTo(other) >= 0
    }

    isAfter(other: MidnightAwareTime): boolean {
      return this.compareTo(other) > 0
    }
  }
}

type MidnightAwareTime = MidnightAwareTime.Start | MidnightAwareTime.End

function minOf(a: MidnightAwareTime, b: MidnightAwareTime): MidnightAwareTime {
  return a.isBefore(b) ? a : b
}

function maxOf(a: MidnightAwareTime, b: MidnightAwareTime): MidnightAwareTime {
  return a.isAfter(b) ? a : b
}

export default class TimeRange {
  constructor(
    public start: MidnightAwareTime.Start,
    public end: MidnightAwareTime.End
  ) {
    if (start.isEqualOrAfter(end)) {
      throw new Error('TimeRange start must be before end')
    }
  }

  static tryCreate(start: LocalTime, end: LocalTime): TimeRange | undefined {
    try {
      return TimeRange.of(start, end)
    } catch (e) {
      return undefined
    }
  }

  static of(start: LocalTime, end: LocalTime): TimeRange {
    return new TimeRange(
      new MidnightAwareTime.Start(start),
      new MidnightAwareTime.End(end)
    )
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
      return new TimeRange(start, end)
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

  includes(time: LocalTime): boolean {
    const t = new MidnightAwareTime.Start(time)
    return t.isEqualOrAfter(this.start) && t.isBefore(this.end)
  }

  includesStartOf(other: TimeRange): boolean {
    return (
      this.start.isEqualOrBefore(other.start) && other.start.isBefore(this.end)
    )
  }

  includesEndOf(other: TimeRange): boolean {
    return this.start.isBefore(other.end) && other.end.isEqualOrBefore(this.end)
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
    return TimeRange.of(
      LocalTime.parseIso(json.start),
      LocalTime.parseIso(json.end)
    )
  }

  /** HH:mm */
  static parse(json: JsonOf<TimeRange>): TimeRange {
    return TimeRange.of(LocalTime.parse(json.start), LocalTime.parse(json.end))
  }

  toJSON(): JsonOf<TimeRange> {
    return {
      start: this.start.__inner.toJSON(),
      end: this.end.__inner.toJSON()
    }
  }
}
