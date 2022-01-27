// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format, isToday } from 'date-fns'

export const DATE_FORMAT_DATE_TIME = 'dd.MM.yyyy HH:mm'

type DateWithoutLeadingZeros = 'd.M.yyyy'
type DateWithLeadingZeros = 'dd.MM.yyyy'

type Time = 'HH:mm'
type FullDate = DateWithLeadingZeros | DateWithoutLeadingZeros

type DateWithoutYear = 'dd.MM.'
type DateWithoutYearShort = 'd.M.'

export type DateFormat = FullDate | DateWithoutYear | DateWithoutYearShort
type DateTimeFormat = `${FullDate} ${Time}`

type AllowedDateFormat = DateFormat | DateTimeFormat

export function formatDate(
  date: Date | null | undefined,
  dateFormat: AllowedDateFormat = 'dd.MM.yyyy'
): string {
  return date ? format(date, dateFormat) : ''
}

export function formatTime(date: Date | null | undefined): string {
  return date ? format(date, 'HH:mm') : ''
}

// matches 24h format with mandatory leading zeros "23:59" and "00:09"
const timeRegex = /^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$/

export function isValidTime(time: string): boolean {
  return timeRegex.test(time)
}

export function formatDateOrTime(date: Date): string {
  return format(date, isToday(date) ? 'HH:mm' : 'dd.MM.')
}
