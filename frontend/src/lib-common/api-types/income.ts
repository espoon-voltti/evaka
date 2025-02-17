// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// Note: these are currently customized in forks so cannot be generated
export const incomeCoefficients = [
  'MONTHLY_WITH_HOLIDAY_BONUS',
  'MONTHLY_NO_HOLIDAY_BONUS',
  'BI_WEEKLY_WITH_HOLIDAY_BONUS',
  'BI_WEEKLY_NO_HOLIDAY_BONUS',
  'DAILY_ALLOWANCE_21_5',
  'DAILY_ALLOWANCE_25',
  'YEARLY'
] as const

export type IncomeCoefficient = (typeof incomeCoefficients)[number]
