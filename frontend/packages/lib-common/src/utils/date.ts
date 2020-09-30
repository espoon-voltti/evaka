// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { isBefore, isValid, toDate, parseISO } from 'date-fns'
import { formatToTimeZone } from 'date-fns-timezone'
import { FormModel } from '@evaka/lib-common/src/types'
import {
  DATE_FORMAT_TZ,
  TIMEZONE_HELSINKI
} from '@evaka/lib-common/src/constants'

/**
 * Converts a string object into Date object.
 * @param date string object representing a date / null / undefined.
 * Returns a Date or null, depeding on "date".
 */
export const isValidDate = (date: string | null | undefined): boolean => {
  return date ? isValid(new Date(date)) : false
}

const parseChildBirthdayFromSSN = (form: FormModel): Date => {
  if (!form.child.socialSecurityNumber) {
    throw new Error('Missing social security number for child in application!')
  }

  const ssn = form.child.socialSecurityNumber
  const d = ssn.slice(0, 2)
  const m = ssn.slice(2, 4)
  const y = ssn.slice(4, 6)
  const decades: Record<string, string> = {
    '+': '18',
    '-': '19',
    A: '20'
  }
  const c = decades[ssn.slice(6, 7)]
  return new Date(`${c}${y}-${m}-${d}`)
}

export const parseChildBirthday = (form: FormModel): Date => {
  const birthday: Date = new Date(form.child.dateOfBirth || NaN)
  return isValid(birthday) ? birthday : parseChildBirthdayFromSSN(form)
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
