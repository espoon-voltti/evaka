// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isBefore, isValid, toDate, parseISO } from 'date-fns'
import { formatToTimeZone } from 'date-fns-timezone'

export const TIMEZONE_HELSINKI = 'Europe/Helsinki'
export const DATE_FORMAT_TZ = 'YYYY-MM-DD'

/**
 * Converts a string object into Date object.
 * @param date string object representing a date / null / undefined.
 * Returns a Date or null, depeding on "date".
 */
export const isValidDate = (date: string | null | undefined): boolean => {
  return date ? isValid(new Date(date)) : false
}

export const parseDate = (input: string): Date => {
  const parts = input.split('-').map((num) => parseInt(num, 10))
  return new Date(parts[0], parts[1] - 1, parts[2]) // Note: months are 0-based
}

/**
 * Converts a string object into Date object.
 * @param date string object representing a date / null / undefined.
 * Returns a Date or null, depeding on "date".
 */
export const stringToDate = (date: string | null | undefined): Date | null => {
  if (date && isValid(toDate(parseISO(date)))) {
    return toDate(parseISO(date))
  } else {
    return null
  }
}

/**
 * Converts a timestamp into a timestamp in "Europe/Helsinki" time tone
 * @param date A string representing a Date / null.
 * Returns the formatted date string in the given format after converting it to "Europe/Helsinki" time zone or null, depeding on "date".
 */
export const parseTzDate = (date: Date | null): string | null => {
  if (date && isValid(date)) {
    return formatToTimeZone(date, DATE_FORMAT_TZ, {
      timeZone: TIMEZONE_HELSINKI
    })
  } else {
    return null
  }
}

/**
 * Checks if two given dates represents a valid range.
 * @param start start date as Date / null.
 * @param end end date as Date / null.
 * Returns true if valid range, false otherwise.
 */
export const isValidRange = (start: Date | null, end: Date | null): boolean => {
  return start && end && isValid(start) && isValid(end)
    ? isBefore(start, end)
    : false
}

export const isValidRangeStr = (
  startStr: string | null,
  endStr: string | null
): boolean => {
  return isValidRange(stringToDate(startStr), stringToDate(endStr))
}
