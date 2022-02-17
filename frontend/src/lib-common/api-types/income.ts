// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

export const incomeCoefficients = [
  'MONTHLY_WITH_HOLIDAY_BONUS',
  'MONTHLY_NO_HOLIDAY_BONUS',
  'BI_WEEKLY_WITH_HOLIDAY_BONUS',
  'BI_WEEKLY_NO_HOLIDAY_BONUS',
  'DAILY_ALLOWANCE_21_5',
  'DAILY_ALLOWANCE_25',
  'YEARLY'
] as const

export type IncomeCoefficient = typeof incomeCoefficients[number]

export const incomeEffects = [
  'MAX_FEE_ACCEPTED',
  'INCOMPLETE',
  'INCOME'
] as const

export type IncomeEffect = typeof incomeEffects[number]

export type IncomeValue = {
  amount: number
  coefficient: IncomeCoefficient
  monthlyAmount: number
}

export interface DecisionIncome {
  effect: IncomeEffect
  data: Record<string, number>
  totalIncome: number
  totalExpenses: number
  total: number
  worksAtECHA: boolean
  validFrom: LocalDate | null
  validTo: LocalDate | null
}
