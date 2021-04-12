export interface TimeRange {
  start: string
  end: string
}

export interface RegularDailyServiceTimes {
  regular: true
  regularTimes: TimeRange
}

export interface IrregularDailyServiceTimes {
  regular: false
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
  return times.regular
}

export function isIrregular(
  times: DailyServiceTimes
): times is IrregularDailyServiceTimes {
  return !times.regular
}
