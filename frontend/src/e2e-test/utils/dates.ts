// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { addWeeks, format, isWeekend, startOfWeek } from 'date-fns'

/**
 * If the given date is a weekend day, changes the date part of the Date object to be the next monday.
 */
export function roundToBusinessDay(date: Date): Date {
  if (!isWeekend(date)) return date
  return addWeeks(startOfWeek(date, { weekStartsOn: 1 }), 1)
}

export function formatISODateString(date: string): string {
  return format(new Date(date), 'dd.MM.yyyy')
}
