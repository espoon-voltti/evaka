import DateRange from 'lib-common/date-range'

export const familySizes = ['2', '3', '4', '5', '6'] as const
export type FamilySize = typeof familySizes[number]

export interface FeeThresholds {
  id: string
  validDuring: DateRange
  maxFee: number
  minFee: number
  minIncomeThreshold2: number
  minIncomeThreshold3: number
  minIncomeThreshold4: number
  minIncomeThreshold5: number
  minIncomeThreshold6: number
  maxIncomeThreshold2: number
  maxIncomeThreshold3: number
  maxIncomeThreshold4: number
  maxIncomeThreshold5: number
  maxIncomeThreshold6: number
  incomeMultiplier2: number
  incomeMultiplier3: number
  incomeMultiplier4: number
  incomeMultiplier5: number
  incomeMultiplier6: number
  incomeThresholdIncrease6Plus: number
  siblingDiscount2: number
  siblingDiscount2Plus: number
}
