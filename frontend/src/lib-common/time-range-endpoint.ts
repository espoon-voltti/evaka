// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalTime from './local-time'
import { Ordered } from './ordered'

// eslint-disable-next-line @typescript-eslint/no-namespace
namespace TimeRangeEndpoint {
  export class Start implements Ordered<Start> {
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

    format(pattern = 'HH:mm'): string {
      return this.__inner.format(pattern)
    }

    formatIso(): string {
      return this.__inner.formatIso()
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

  export class End implements Ordered<End> {
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

    format(pattern = 'HH:mm'): string {
      return this.__inner.format(pattern)
    }

    formatIso(): string {
      return this.__inner.formatIso()
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

type TimeRangeEndpoint = TimeRangeEndpoint.Start | TimeRangeEndpoint.End

export default TimeRangeEndpoint
