// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from './helsinki-date-time'
import { Ordered } from './ordered'

export default class LocalHm implements Ordered<LocalHm> {
  constructor(
    public readonly hour: number,
    public readonly minute: number
  ) {
    if (hour < 0 || hour > 23) {
      throw new RangeError('Invalid hour')
    }
    if (minute < 0 || minute > 59) {
      throw new RangeError('Invalid minute')
    }
  }

  isBefore(other: LocalHm): boolean {
    return (
      this.hour < other.hour ||
      (this.hour === other.hour && this.minute < other.minute)
    )
  }

  isEqualOrBefore(other: LocalHm): boolean {
    return (
      this.hour < other.hour ||
      (this.hour === other.hour && this.minute <= other.minute)
    )
  }

  isEqual(other: LocalHm): boolean {
    return this.hour === other.hour && this.minute === other.minute
  }

  isEqualOrAfter(other: LocalHm): boolean {
    return (
      this.hour > other.hour ||
      (this.hour === other.hour && this.minute >= other.minute)
    )
  }

  isAfter(other: LocalHm): boolean {
    return (
      this.hour > other.hour ||
      (this.hour === other.hour && this.minute > other.minute)
    )
  }

  format(): string {
    return `${this.hour.toString().padStart(2, '0')}:${this.minute.toString().padStart(2, '0')}`
  }

  toString(): string {
    return this.format()
  }

  valueOf(): string {
    return this.format()
  }

  toJSON(): string {
    return this.format()
  }

  // A dummy method to make LocalHm distinct from LocalTime with respect to structural typing
  __thisIsLocalHm(): boolean {
    return true
  }

  /**
   * Current date in Europe/Helsinki timezone.
   */
  static nowInHelsinkiTz(): LocalHm {
    const now = HelsinkiDateTime.now()
    return new LocalHm(now.hour, now.minute)
  }

  static parse(value: string): LocalHm {
    const parts = value.split(':')
    if (parts.length !== 2)
      throw new RangeError(`Invalid minute resolution time ${value}`)
    const hour = Number(parts[0])
    const minute = Number(parts[1])
    return new LocalHm(hour, minute)
  }

  static parseOrNull(value: string): LocalHm | null {
    try {
      return LocalHm.parse(value)
    } catch (e: unknown) {
      if (e instanceof RangeError) return null
      throw e
    }
  }
}
