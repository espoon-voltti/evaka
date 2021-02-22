// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format } from 'date-fns'
import { useState, useEffect } from 'react'

export const DATE_FORMAT_DATE_DEFAULT = 'dd.MM.yyyy'
export const DATE_FORMAT_DATE_TIME_DEFAULT = 'dd.MM.yyyy HH:mm'

export function formatDate(
  date: Date | null | undefined,
  dateFormat = DATE_FORMAT_DATE_TIME_DEFAULT
): string {
  return date ? format(date, dateFormat) : ''
}

export const useDebounce = <T>(value: T, delay: number): T => {
  const [debouncedValue, setDebouncedValue] = useState(value)

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedValue(value)
    }, delay)

    return () => {
      clearTimeout(handler)
    }
  }, [value])

  return debouncedValue
}
