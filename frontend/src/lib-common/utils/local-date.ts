// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

/**
 * Calculate age based on date of birth.
 */
export const getAge = (dateOfBirth: LocalDate): number => {
  return LocalDate.today().differenceInYears(dateOfBirth)
}

/**
 *
 * Groups a list of dates to list of consecutive date ranges
 */
export function groupDatesToRanges(dates: LocalDate[]): FiniteDateRange[] {
  if (dates.length === 0) return []

  const sorted = [...dates].sort()
  const groupedDates: LocalDate[][] = [[sorted[0]]]
  let previousDate: LocalDate = sorted[0]
  for (const date of sorted) {
    if (date.differenceInDays(previousDate) > 1) {
      groupedDates.push([date])
    } else {
      groupedDates[groupedDates.length - 1].push(date)
    }
    previousDate = date
  }

  const dateRanges: FiniteDateRange[] = []
  for (const dateGroup of groupedDates) {
    dateRanges.push(
      new FiniteDateRange(dateGroup[0], dateGroup[dateGroup.length - 1])
    )
  }
  return dateRanges
}
