// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'

export const familySizes = ['2', '3', '4', '5', '6'] as const
export type FamilySize = typeof familySizes[number]

export interface FeeThresholdsWithId {
  id: string
  thresholds: FeeThresholds
}

export interface FeeThresholds {
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
