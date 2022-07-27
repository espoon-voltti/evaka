// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type DateRange from 'lib-common/date-range'

export interface TimeRange {
  start: string
  end: string
}

interface BaseDailyServiceTimes {
  validityPeriod: DateRange
}

export interface RegularDailyServiceTimes extends BaseDailyServiceTimes {
  type: 'REGULAR'
  regularTimes: TimeRange
}

export interface IrregularDailyServiceTimes extends BaseDailyServiceTimes {
  type: 'IRREGULAR'
  monday: TimeRange | null
  tuesday: TimeRange | null
  wednesday: TimeRange | null
  thursday: TimeRange | null
  friday: TimeRange | null
  saturday: TimeRange | null
  sunday: TimeRange | null
}

export interface VariableDailyServiceTimes extends BaseDailyServiceTimes {
  type: 'VARIABLE_TIME'
}

export type DailyServiceTimes =
  | RegularDailyServiceTimes
  | IrregularDailyServiceTimes
  | VariableDailyServiceTimes

export function isRegular(
  times: DailyServiceTimes
): times is RegularDailyServiceTimes {
  return times.type === 'REGULAR'
}

export function isIrregular(
  times: DailyServiceTimes
): times is IrregularDailyServiceTimes {
  return times.type === 'IRREGULAR'
}

export function isVariableTime(
  times: DailyServiceTimes
): times is VariableDailyServiceTimes {
  return times.type === 'VARIABLE_TIME'
}

export function getTimesOnWeekday(
  times: IrregularDailyServiceTimes,
  dayNumber: number
) {
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
