// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DailyServiceTimesValue } from '../generated/api-types/dailyservicetimes'
import TimeRange from '../time-range'

export function isRegular(
  times: DailyServiceTimesValue
): times is DailyServiceTimesValue.RegularTimes {
  return times.type === 'REGULAR'
}

export function isIrregular(
  times: DailyServiceTimesValue
): times is DailyServiceTimesValue.IrregularTimes {
  return times.type === 'IRREGULAR'
}

export function isVariableTime(
  times: DailyServiceTimesValue
): times is DailyServiceTimesValue.VariableTimes {
  return times.type === 'VARIABLE_TIME'
}

export function getTimesOnWeekday(
  times: DailyServiceTimesValue.IrregularTimes,
  dayNumber: number
): TimeRange | null {
  switch (dayNumber) {
    case 1:
      return times.monday
    case 2:
      return times.tuesday
    case 3:
      return times.wednesday
    case 4:
      return times.thursday
    case 5:
      return times.friday
    case 6:
      return times.saturday
    case 7:
      return times.sunday
    default:
      return null
  }
}
