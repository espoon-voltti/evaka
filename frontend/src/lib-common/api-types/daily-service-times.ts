// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DailyServiceTimesValue } from 'lib-common/generated/api-types/dailyservicetimes'

import DateRange from '../date-range'
import { TimeRange } from '../generated/api-types/shared'
import { JsonOf } from '../json'
import LocalTime from '../local-time'

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
        regularTimes: parseIsoTimeRange(times.regularTimes),
        validityPeriod: DateRange.parseJson(times.validityPeriod)
      }
    case 'IRREGULAR':
      return {
        type: 'IRREGULAR',
        monday: times.monday ? parseIsoTimeRange(times.monday) : null,
        tuesday: times.tuesday ? parseIsoTimeRange(times.tuesday) : null,
        wednesday: times.wednesday ? parseIsoTimeRange(times.wednesday) : null,
        thursday: times.thursday ? parseIsoTimeRange(times.thursday) : null,
        friday: times.friday ? parseIsoTimeRange(times.friday) : null,
        saturday: times.saturday ? parseIsoTimeRange(times.saturday) : null,
        sunday: times.sunday ? parseIsoTimeRange(times.sunday) : null,
        validityPeriod: DateRange.parseJson(times.validityPeriod)
      }
    case 'VARIABLE_TIME':
      return {
        type: 'VARIABLE_TIME',
        validityPeriod: DateRange.parseJson(times.validityPeriod)
      }
  }
}

export function parseIsoTimeRange(range: JsonOf<TimeRange>): TimeRange {
  return {
    start: LocalTime.parseIso(range.start),
    end: LocalTime.parseIso(range.end)
  }
}
