// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format, isToday } from 'date-fns'

export const DATE_FORMAT_DATE = 'dd.MM.yyyy'
export const DATE_FORMAT_DATE_TIME = 'dd.MM.yyyy HH:mm'
export const DATE_FORMAT_NO_YEAR = 'dd.MM.'
export const DATE_FORMAT_SHORT_NO_YEAR = 'd.M.'
export const DATE_FORMAT_ISO = 'yyyy-MM-dd'
export const DATE_FORMAT_TIME_ONLY = 'HH:mm'

export function formatDate(
  date: Date | null | undefined,
  dateFormat = DATE_FORMAT_DATE
): string {
  return date ? format(date, dateFormat) : ''
}

export function formatTime(date: Date | null | undefined): string {
  return formatDate(date, DATE_FORMAT_TIME_ONLY)
}

// matches 24h format with mandatory leading zeros "23:59" and "00:09"
const timeRegex = /^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$/

export function isValidTime(time: string): boolean {
  return timeRegex.test(time)
}

export function formatDateOrTime(date: Date): string {
  return format(
    date,
    isToday(date) ? DATE_FORMAT_TIME_ONLY : DATE_FORMAT_SHORT_NO_YEAR
  )
}
