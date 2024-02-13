// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from '../date-range'
import { DailyServiceTimesValue } from '../generated/api-types/dailyservicetimes'
import { JsonOf } from '../json'
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

export function parseDailyServiceTimes(
  times: JsonOf<DailyServiceTimesValue>
): DailyServiceTimesValue {
  switch (times.type) {
    case 'REGULAR':
      return {
        type: 'REGULAR',
        regularTimes: TimeRange.parseJson(times.regularTimes),
        validityPeriod: DateRange.parseJson(times.validityPeriod)
      }
    case 'IRREGULAR':
      return {
        type: 'IRREGULAR',
        monday: times.monday ? TimeRange.parseJson(times.monday) : null,
        tuesday: times.tuesday ? TimeRange.parseJson(times.tuesday) : null,
        wednesday: times.wednesday
          ? TimeRange.parseJson(times.wednesday)
          : null,
        thursday: times.thursday ? TimeRange.parseJson(times.thursday) : null,
        friday: times.friday ? TimeRange.parseJson(times.friday) : null,
        saturday: times.saturday ? TimeRange.parseJson(times.saturday) : null,
        sunday: times.sunday ? TimeRange.parseJson(times.sunday) : null,
        validityPeriod: DateRange.parseJson(times.validityPeriod)
      }
    case 'VARIABLE_TIME':
      return {
        type: 'VARIABLE_TIME',
        validityPeriod: DateRange.parseJson(times.validityPeriod)
      }
  }
}
