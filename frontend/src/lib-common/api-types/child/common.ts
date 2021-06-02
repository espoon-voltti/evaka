// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export interface TimeRange {
  start: string
  end: string
}

export type DailyServiceTimesType = 'REGULAR' | 'IRREGULAR'

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

export type DailyServiceTimes =
  | RegularDailyServiceTimes
  | IrregularDailyServiceTimes

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
