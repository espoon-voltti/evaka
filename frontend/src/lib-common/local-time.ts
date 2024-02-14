// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format, parse } from 'date-fns'
import isInteger from 'lodash/isInteger'

import { isValidTime } from './date'
import HelsinkiDateTime from './helsinki-date-time'
import { Ordered } from './ordered'
import { isAutomatedTest } from './utils/helpers'

// ISO local time with nanosecond precision
const isoPattern = /^(\d{2}):(\d{2}):(\d{2})(?:.(\d{9}))?$/

const hourMinutePattern = /^(\d{2}):(\d{2})$/

export default class LocalTime implements Ordered<LocalTime> {
  private constructor(
    readonly hour: number,
    readonly minute: number,
    readonly second: number,
    readonly nanosecond: number
  ) {}

  format(pattern = 'HH:mm'): string {
    const timestamp = new Date(
      1970,
      0,
      1,
      this.hour,
      this.minute,
      this.second,
      nanosToMillis(this.nanosecond)
    )
    return format(timestamp, pattern)
  }

  formatIso(): string {
    const hour = this.hour.toString().padStart(2, '0')
    const minute = this.minute.toString().padStart(2, '0')
    const second = this.second.toString().padStart(2, '0')
    const nanosecond = this.nanosecond.toString().padStart(9, '0')
    return `${hour}:${minute}:${second}${
      this.nanosecond > 0 ? `.${nanosecond}` : ''
    }`
  }

  isBefore(other: LocalTime): boolean {
    return this.formatIso() < other.formatIso()
  }
  isEqualOrBefore(other: LocalTime): boolean {
    return this.isEqual(other) || this.isBefore(other)
  }
  isEqual(other: LocalTime): boolean {
    return (
      this.hour === other.hour &&
      this.minute == other.minute &&
      this.second == other.second &&
      this.nanosecond == other.nanosecond
    )
  }
  isEqualOrAfter(other: LocalTime): boolean {
    return this.isEqual(other) || this.isAfter(other)
  }
  isAfter(other: LocalTime): boolean {
    return this.formatIso() > other.formatIso()
  }
  compareTo(other: LocalTime): number {
    if (this.isBefore(other)) {
      return -1
    }
    if (this.isAfter(other)) {
      return 1
    }
    return 0
  }

  toString(): string {
    return this.formatIso()
  }
  toJSON(): string {
    return this.formatIso()
  }
  valueOf(): string {
    return this.formatIso()
  }

  /**
   * Current time in system (= browser local) timezone.
   */
  static nowInSystemTz(): LocalTime {
    const timestamp =
      (isAutomatedTest && typeof window !== 'undefined'
        ? window.evaka?.mockedTime
        : undefined) ?? new Date()
    return LocalTime.of(
      timestamp.getHours(),
      timestamp.getMinutes(),
      timestamp.getSeconds(),
      millisToNanos(timestamp.getMilliseconds())
    )
  }
  /**
   * Current time in Europe/Helsinki timezone.
   */
  static nowInHelsinkiTz(): LocalTime {
    return HelsinkiDateTime.now().toLocalTime()
  }

  static MIN = LocalTime.of(0, 0, 0, 0)
  static MIDNIGHT = LocalTime.MIN
  static MAX = LocalTime.of(23, 59, 59, 999_999_999)

  static parseIso(text: string): LocalTime {
    const parts =
      isoPattern.exec(text) ??
      // HH:mm for legacy compatibility until our backend sends nanosecond-precision ISO in all cases
      hourMinutePattern.exec(text)
    if (parts) {
      const [, hour, minute, second, nanosecond] =
        parts &&
        parts.map((part) => (part !== undefined ? parseInt(part, 10) : 0))
      const result = LocalTime.tryCreate(hour, minute, second, nanosecond)
      if (result) {
        return result
      }
    }
    throw new RangeError(`Invalid time ${text}`)
  }

  static tryParse(textHHmm: string): LocalTime | undefined {
    return isValidTime(textHHmm)
      ? this.tryParseWithPattern(textHHmm, 'HH:mm')
      : undefined
  }

  static tryParseWithPattern(
    text: string,
    pattern = 'HH:mm'
  ): LocalTime | undefined {
    const timestamp = parse(text, pattern, new Date(1970, 0, 1))
    return LocalTime.tryCreate(
      timestamp.getHours(),
      timestamp.getMinutes(),
      timestamp.getSeconds(),
      millisToNanos(timestamp.getMilliseconds())
    )
  }
  static parse(text: string, pattern = 'HH:mm'): LocalTime {
    const result = LocalTime.tryParseWithPattern(text, pattern)
    if (!result) {
      throw new RangeError(`Invalid time ${text}`)
    }
    return result
  }
  static of(
    hour: number,
    minute: number,
    second = 0,
    nanosecond = 0
  ): LocalTime {
    const result = LocalTime.tryCreate(hour, minute, second, nanosecond)
    if (!result) {
      throw new RangeError(
        `Invalid time ${hour}:${minute}:${second}.${nanosecond}`
      )
    }
    return result
  }

  static tryCreate(
    hour: number,
    minute: number,
    second: number,
    nanosecond: number
  ): LocalTime | undefined {
    if (!(isInteger(hour) && hour >= 0 && hour < 24)) return undefined
    if (!(isInteger(minute) && minute >= 0 && minute < 60)) return undefined
    if (!(isInteger(second) && second >= 0 && second < 60)) return undefined
    if (
      !(isInteger(nanosecond) && nanosecond >= 0 && nanosecond < 1_000_000_000)
    )
      return undefined
    return new LocalTime(hour, minute, second, nanosecond)
  }
}
function millisToNanos(millis: number): number {
  return millis * 1_000_000
}
function nanosToMillis(nanos: number): number {
  return Math.floor(nanos / 1_000_000)
}
