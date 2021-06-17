// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format } from 'date-fns'

export const DATE_FORMAT_DATE = 'dd.MM.yyyy'
export const DATE_FORMAT_DATE_TIME = 'dd.MM.yyyy HH:mm'
export const DATE_FORMAT_NO_YEAR = 'dd.MM.'
export const DATE_FORMAT_ISO = 'yyyy-MM-dd'
export const DATE_FORMAT_TIME_ONLY = 'HH:mm'

export function formatDate(
  date: Date | null | undefined,
  dateFormat = DATE_FORMAT_DATE
): string {
  return date ? format(date, dateFormat) : ''
}
