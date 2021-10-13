// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from './index'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

export type IncomeId = 'new' | UUID

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

export type IncomeFields = Partial<Record<string, IncomeValue>>

export interface IncomeBody {
  effect: IncomeEffect
  data: IncomeFields
  isEntrepreneur: boolean
  worksAtECHA: boolean
  validFrom: LocalDate
  validTo?: LocalDate
  notes: string
}

export interface Income extends IncomeBody {
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

export interface IncomeOption {
  value: string
  nameFi: string
  multiplier: number
  withCoefficient: boolean
  isSubType: boolean
}

export const deserializeIncome = (json: JsonOf<Income>): Income => ({
  ...json,
  validFrom: LocalDate.parseIso(json.validFrom),
  validTo: json.validTo ? LocalDate.parseIso(json.validTo) : undefined,
  updatedAt: new Date(json.updatedAt)
})
