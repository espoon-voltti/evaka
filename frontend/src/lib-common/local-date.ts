// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  addBusinessDays,
  addDays,
  addMonths,
  addWeeks,
  addYears,
  differenceInDays,
  differenceInYears,
  endOfWeek,
  format,
  getISODay,
  getISOWeek,
  isAfter,
  isBefore,
  isExists,
  isSameDay,
  isToday,
  isValid,
  isWeekend,
  lastDayOfMonth,
  setMonth,
  startOfMonth,
  startOfWeek,
  subDays,
  subMonths,
  subWeeks,
  subYears
} from 'date-fns'

import { DateFormat, DateFormatWithWeekday, locales } from './date'
import HelsinkiDateTime from './helsinki-date-time'
import LocalTime from './local-time'
import { Ordered } from './ordered'
import { isAutomatedTest } from './utils/helpers'

const isoPattern = /^([0-9]+)-([0-9]+)-([0-9]+)$/
const fiPattern = /^(\d{1,2})\.(\d{1,2})\.(\d{4})$/

type Lang = 'fi' | 'sv' | 'en'
export default class LocalDate implements Ordered<LocalDate> {
  private constructor(
    readonly year: number,
    readonly month: number,
    readonly date: number
  ) {}
  getYear(): number {
    return this.year
  }
  getMonth(): number {
    return this.month
  }
  getDate(): number {
    return this.date
  }
  getIsoDayOfWeek(): number {
    return getISODay(this.toSystemTzDate())
  }
  getIsoWeek(): number {
    return getISOWeek(this.toSystemTzDate())
  }
  withYear(year: number): LocalDate {
    return LocalDate.of(year, this.month, this.date)
  }
  withMonth(month: number): LocalDate {
    return LocalDate.fromSystemTzDate(
      setMonth(this.toSystemTzDate(), month - 1)
    )
  }
  withDate(date: number): LocalDate {
    return LocalDate.of(this.year, this.month, date)
  }
  addYears(years: number): LocalDate {
    return LocalDate.fromSystemTzDate(addYears(this.toSystemTzDate(), years))
  }
  addMonths(months: number): LocalDate {
    return LocalDate.fromSystemTzDate(addMonths(this.toSystemTzDate(), months))
  }
  addWeeks(weeks: number): LocalDate {
    return LocalDate.fromSystemTzDate(addWeeks(this.toSystemTzDate(), weeks))
  }
  addDays(days: number): LocalDate {
    return LocalDate.fromSystemTzDate(addDays(this.toSystemTzDate(), days))
  }
  addBusinessDays(days: number): LocalDate {
    return LocalDate.fromSystemTzDate(
      addBusinessDays(this.toSystemTzDate(), days)
    )
  }
  subYears(years: number): LocalDate {
    return LocalDate.fromSystemTzDate(subYears(this.toSystemTzDate(), years))
  }
  subMonths(months: number): LocalDate {
    return LocalDate.fromSystemTzDate(subMonths(this.toSystemTzDate(), months))
  }
  subWeeks(weeks: number): LocalDate {
    return LocalDate.fromSystemTzDate(subWeeks(this.toSystemTzDate(), weeks))
  }
  subDays(days: number): LocalDate {
    return LocalDate.fromSystemTzDate(subDays(this.toSystemTzDate(), days))
  }
  startOfWeek(): LocalDate {
    return LocalDate.fromSystemTzDate(
      startOfWeek(this.toSystemTzDate(), { weekStartsOn: 1 })
    )
  }
  endOfWeek(): LocalDate {
    return LocalDate.fromSystemTzDate(
      endOfWeek(this.toSystemTzDate(), { weekStartsOn: 1 })
    )
  }
  startOfMonth(): LocalDate {
    return LocalDate.fromSystemTzDate(startOfMonth(this.toSystemTzDate()))
  }
  lastDayOfMonth(): LocalDate {
    return LocalDate.fromSystemTzDate(lastDayOfMonth(this.toSystemTzDate()))
  }
  isBefore(other: LocalDate): boolean {
    return isBefore(this.toSystemTzDate(), other.toSystemTzDate())
  }
  isEqualOrAfter(other: LocalDate): boolean {
    return this.isEqual(other) || this.isAfter(other)
  }
  isEqualOrBefore(other: LocalDate): boolean {
    return this.isEqual(other) || this.isBefore(other)
  }
  isEqual(other: LocalDate): boolean {
    return (
      this.year === other.year &&
      this.month === other.month &&
      this.date === other.date
    )
  }
  isAfter(other: LocalDate): boolean {
    return isAfter(this.toSystemTzDate(), other.toSystemTzDate())
  }
  compareTo(other: LocalDate): number {
    if (this.isBefore(other)) {
      return -1
    }
    if (this.isAfter(other)) {
      return 1
    }
    return 0
  }
  isBetween(minInclusive: LocalDate, maxInclusive: LocalDate): boolean {
    return (
      this.isEqualOrAfter(minInclusive) && this.isEqualOrBefore(maxInclusive)
    )
  }
  isToday(): boolean {
    return isAutomatedTest
      ? isSameDay(
          LocalDate.todayInSystemTz().toSystemTzDate(),
          this.toSystemTzDate()
        )
      : isToday(this.toSystemTzDate())
  }
  isWeekend(): boolean {
    return isWeekend(this.toSystemTzDate())
  }
  differenceInYears(other: LocalDate): number {
    return differenceInYears(this.toSystemTzDate(), other.toSystemTzDate())
  }
  differenceInDays(other: LocalDate): number {
    return differenceInDays(this.toSystemTzDate(), other.toSystemTzDate())
  }
  format<T extends DateFormat | DateFormatWithWeekday>(
    ...[dateFormat, locale]: T extends DateFormatWithWeekday
      ? [DateFormatWithWeekday, Lang]
      : [DateFormat?]
  ): string {
    return format(
      this.toSystemTzDate(),
      dateFormat ?? 'dd.MM.yyyy',
      locale ? { locale: locales[locale] } : undefined
    )
  }
  /**
   * <a href="https://date-fns.org/docs/format">date-fns format()</a>
   */
  formatExotic(pattern: string, lang?: Lang): string {
    return format(this.toSystemTzDate(), pattern, {
      locale: locales[lang ?? 'fi']
    })
  }
  toString(): string {
    return this.formatIso()
  }
  valueOf(): string {
    return this.formatIso()
  }
  formatIso(): string {
    const month = this.month.toString().padStart(2, '0')
    const date = this.date.toString().padStart(2, '0')
    const year = this.year.toString().padStart(4, '0')
    return `${year}-${month}-${date}`
  }
  toJSON(): string {
    return this.formatIso()
  }
  toSystemTzDate(): Date {
    return new Date(this.year, this.month - 1, this.date)
  }
  toHelsinkiDateTime(time: LocalTime): HelsinkiDateTime {
    return HelsinkiDateTime.fromLocal(this, time)
  }

  /**
   * Current date in system (= browser local) timezone.
   */
  static todayInSystemTz(): LocalDate {
    const timestamp =
      (isAutomatedTest && typeof window !== 'undefined'
        ? window.evaka?.mockedTime
        : undefined) ?? new Date()
    return LocalDate.fromSystemTzDate(timestamp)
  }
  /**
   * Current date in Europe/Helsinki timezone.
   */
  static todayInHelsinkiTz(): LocalDate {
    return HelsinkiDateTime.now().toLocalDate()
  }

  static fromSystemTzDate(date: Date): LocalDate {
    const result = LocalDate.tryFromDate(date)
    if (!result) {
      throw new RangeError('Invalid date')
    }
    return result
  }
  static parseFiOrThrow(value: string): LocalDate {
    const date = LocalDate.parseFiOrNull(value)
    if (!date) {
      throw new RangeError(`Invalid date ${value}`)
    }
    return date
  }
  static parseFiOrNull(value: string): LocalDate | null {
    const parts = fiPattern.exec(value)
    if (parts) {
      const date = LocalDate.tryCreate(
        Number(parts[3]),
        Number(parts[2]),
        Number(parts[1])
      )
      if (date) {
        return date
      }
    }
    return null
  }
  static parseIso(value: string): LocalDate {
    const result = LocalDate.tryParseIso(value)
    if (!result) {
      throw new RangeError(`Invalid ISO date ${value}`)
    }
    return result
  }
  static parseNullableIso(value: string | null): LocalDate | null {
    return value !== null ? LocalDate.parseIso(value) : null
  }
  static tryParseIso(value: string): LocalDate | undefined {
    const parts = isoPattern.exec(value)
    if (!parts) return undefined
    return LocalDate.tryCreate(
      Number(parts[1]),
      Number(parts[2]),
      Number(parts[3])
    )
  }
  static of(year: number, month: number, date: number): LocalDate {
    const result = LocalDate.tryCreate(year, month, date)
    if (!result) {
      throw new RangeError(`Invalid date ${year}-${month}-${date}`)
    }
    return result
  }

  /** Generate all dates in [start, end] */
  static range(start: LocalDate, end: LocalDate): LocalDate[] {
    const result: LocalDate[] = []
    for (
      let value = start;
      value.isEqualOrBefore(end);
      value = value.addDays(1)
    ) {
      result.push(value)
    }
    return result
  }

  static tryCreate(
    year: number,
    month: number,
    date: number
  ): LocalDate | undefined {
    return isExists(year, month - 1, date)
      ? new LocalDate(year, month, date)
      : undefined
  }
  private static tryFromDate(date: Date): LocalDate | undefined {
    return isValid(date)
      ? new LocalDate(date.getFullYear(), date.getMonth() + 1, date.getDate())
      : undefined
  }
}
