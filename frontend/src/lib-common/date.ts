// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Locale } from 'date-fns'
import { isToday } from 'date-fns'
import { formatInTimeZone } from 'date-fns-tz'
import { fi, sv, enGB } from 'date-fns/locale'

export const locales: { fi: Locale; sv: Locale; en: Locale } = {
  fi,
  sv,
  en: enGB
}

export const DATE_FORMAT_DATE_TIME = 'dd.MM.yyyy HH:mm'
const DATE_FORMAT_TIME_ONLY = 'HH:mm'

type DateWithoutLeadingZeros = 'd.M.yyyy'
type DateWithLeadingZeros = 'dd.MM.yyyy'
type FullDate = DateWithLeadingZeros | DateWithoutLeadingZeros

type DateWithoutYear = 'dd.MM.'
type DateWithoutYearShort = 'd.M.'

export type DateFormat = FullDate | DateWithoutYear | DateWithoutYearShort

type Weekday = 'EEEEEE' // ma, ti.. må, ti.. Mo, Tu
export type DateFormatWithWeekday = Weekday | `${Weekday} ${DateFormat}`

type Time = typeof DATE_FORMAT_TIME_ONLY
type DateTimeFormat = `${FullDate} ${Time}`

type FormatWithoutWeekday = DateFormat | DateTimeFormat
type AllowedDateFormat = FormatWithoutWeekday | DateFormatWithWeekday

export function formatDate<T extends AllowedDateFormat>(
  date: Date | null | undefined,
  ...[dateFormat, locale]: T extends DateFormatWithWeekday
    ? [DateFormatWithWeekday, keyof typeof locales]
    : [FormatWithoutWeekday?]
): string {
  return date
    ? formatInTimeZone(
        date,
        'Europe/Helsinki',
        dateFormat ?? 'dd.MM.yyyy',
        locale ? { locale: locales[locale] } : undefined
      )
    : ''
}

export function formatTime(date: Date | null | undefined): string {
  return date
    ? formatInTimeZone(date, 'Europe/Helsinki', DATE_FORMAT_TIME_ONLY)
    : ''
}

// matches 24h format with mandatory leading zeros "23:59" and "00:09"
const timeRegex = /^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$/

export function isValidTime(time: string): boolean {
  return timeRegex.test(time)
}

export function formatDateOrTime(date: Date): string {
  return isToday(date) ? formatTime(date) : formatDate(date, 'dd.MM.')
}
