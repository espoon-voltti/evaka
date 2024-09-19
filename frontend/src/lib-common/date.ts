// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Locale } from 'date-fns'
import { enGB, fi, sv } from 'date-fns/locale'

import HelsinkiDateTime from './helsinki-date-time'

export const locales: { fi: Locale; sv: Locale; en: Locale } = {
  fi,
  sv,
  en: enGB
}

type DateWithoutLeadingZeros = 'd.M.yyyy'
type DateWithLeadingZeros = 'dd.MM.yyyy'
type FullDate = DateWithLeadingZeros | DateWithoutLeadingZeros

type DateWithoutYear = 'dd.MM.'
type DateWithoutYearShort = 'd.M.'
type MonthAndYear = 'MM/yyyy'

export type DateFormat =
  | FullDate
  | DateWithoutYear
  | DateWithoutYearShort
  | MonthAndYear

type Weekday = 'EEEEEE' // ma, ti.. må, ti.. Mo, Tu
export type DateFormatWithWeekday = Weekday | `${Weekday} ${DateFormat}`

// matches 24h format with mandatory leading zeros "23:59" and "00:09"
const timeRegex = /^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$/

export function isValidTime(time: string): boolean {
  return timeRegex.test(time)
}

export function formatDateOrTime(timestamp: HelsinkiDateTime): string {
  return timestamp.toLocalDate().isToday()
    ? timestamp.toLocalTime().format()
    : timestamp.toLocalDate().format('dd.MM.')
}
