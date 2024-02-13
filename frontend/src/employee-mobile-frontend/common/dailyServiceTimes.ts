// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  isIrregular,
  isRegular,
  isVariableTime
} from 'lib-common/api-types/daily-service-times'
import { DailyServiceTimesValue } from 'lib-common/generated/api-types/dailyservicetimes'
import LocalDate from 'lib-common/local-date'
import TimeRange from 'lib-common/time-range'
import { mockNow } from 'lib-common/utils/helpers'

const dayNames = [
  'monday',
  'tuesday',
  'wednesday',
  'thursday',
  'friday'
] as const

type DayName = (typeof dayNames)[number]

function getToday(): DayName | undefined {
  // Sunday is 0
  const dayIndex = ((mockNow() ?? new Date()).getDay() + 6) % 7
  return dayNames[dayIndex]
}

export function getServiceTimeRangeOrNullForDate(
  times: DailyServiceTimesValue | null,
  date: LocalDate
): TimeRange | null {
  if (times != null) {
    if (isRegular(times)) return times.regularTimes
    if (isIrregular(times)) {
      const dayIndex = date.getIsoDayOfWeek()
      const dayName = dayNames[dayIndex - 1]
      return times[dayName]
    }
  }

  return null
}

export function getTodaysServiceTimes(
  times: DailyServiceTimesValue | null
): TimeRange | 'not_today' | 'not_set' | 'variable_times' {
  if (times === null) return 'not_set'

  if (isRegular(times)) return times.regularTimes

  if (isVariableTime(times)) return 'variable_times'

  if (isIrregular(times)) {
    const today = getToday()
    if (!today) return 'not_today'

    return times[today] ?? 'not_today'
  }

  return 'not_set'
}
