import DateRange from 'lib-common/date-range'

export interface Pricing {
  validDuring: DateRange
  multiplier: number
  maxThresholdDifference: number
  minThreshold2: number
  minThreshold3: number
  minThreshold4: number
  minThreshold5: number
  minThreshold6: number
  thresholdIncrease6Plus: number
}
