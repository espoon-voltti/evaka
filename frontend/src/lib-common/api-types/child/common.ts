// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export interface TimeRange {
  start: string
  end: string
}

export type DailyServiceTimesType = 'REGULAR' | 'IRREGULAR' | 'VARIABLE_TIME'

export interface RegularDailyServiceTimes {
  type: 'REGULAR'
  regularTimes: TimeRange
}

export interface IrregularDailyServiceTimes {
  type: 'IRREGULAR'
  monday: TimeRange | null
  tuesday: TimeRange | null
  wednesday: TimeRange | null
  thursday: TimeRange | null
  friday: TimeRange | null
}

export interface VariableDailyServiceTimes {
  type: 'VARIABLE_TIME'
  variableTimes: boolean | null
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
