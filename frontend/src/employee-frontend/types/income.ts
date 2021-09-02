// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from './index'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

export type IncomeId = 'new' | UUID

export interface PartialIncome {
  effect: IncomeEffect
  data: Partial<{ [K in IncomeType]: IncomeValue }>
  isEntrepreneur: boolean
  worksAtECHA: boolean
  validFrom: LocalDate
  validTo?: LocalDate
  notes: string
}

export interface Income extends PartialIncome {
  id: UUID
  personId: UUID
  total: number
  totalIncome: number
  totalExpenses: number
  updatedAt: Date
  updatedBy: string
  applicationId: UUID | null
  notes: string
}

export const deserializeIncome = (json: JsonOf<Income>): Income => ({
  ...json,
  validFrom: LocalDate.parseIso(json.validFrom),
  validTo: json.validTo ? LocalDate.parseIso(json.validTo) : undefined,
  updatedAt: new Date(json.updatedAt)
})

export type IncomeValue = {
  amount: number
  coefficient: IncomeCoefficient
  monthlyAmount?: number
}

export const incomeCoefficients = [
  'MONTHLY_WITH_HOLIDAY_BONUS',
  'MONTHLY_NO_HOLIDAY_BONUS',
  'BI_WEEKLY_WITH_HOLIDAY_BONUS',
  'BI_WEEKLY_NO_HOLIDAY_BONUS',
  'YEARLY'
] as const

export type IncomeCoefficient = typeof incomeCoefficients[number]

export const incomeEffects = [
  'MAX_FEE_ACCEPTED',
  'INCOMPLETE',
  'INCOME'
] as const

export type IncomeEffect = typeof incomeEffects[number]

export const incomeTypes = [
  'MAIN_INCOME',
  'SHIFT_WORK_ADD_ON',
  'PERKS',
  'SECONDARY_INCOME',
  'PENSION',
  'UNEMPLOYMENT_BENEFITS',
  'SICKNESS_ALLOWANCE',
  'PARENTAL_ALLOWANCE',
  'HOME_CARE_ALLOWANCE',
  'ALIMONY',
  'OTHER_INCOME'
] as const

export const expenseTypes = ['ALL_EXPENSES'] as const

export type IncomeType =
  | typeof incomeTypes[number]
  | typeof expenseTypes[number]

export const incomeSubTypes: IncomeType[] = ['SHIFT_WORK_ADD_ON', 'PERKS']
