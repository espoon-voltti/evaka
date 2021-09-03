// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../../types'

export interface TimeRange {
  start: string
  end: string
}

export interface RegularDailyServiceTimes {
  childId: UUID
  type: 'REGULAR'
  regularTimes: TimeRange
}

export interface IrregularDailyServiceTimes {
  childId: UUID
  type: 'IRREGULAR'
  monday: TimeRange | null
  tuesday: TimeRange | null
  wednesday: TimeRange | null
  thursday: TimeRange | null
  friday: TimeRange | null
}

export interface VariableDailyServiceTimes {
  childId: UUID
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
    default:
      return null
  }
}
