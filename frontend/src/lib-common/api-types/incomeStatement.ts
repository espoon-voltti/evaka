// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Entrepreneur,
  EstimatedIncome,
  IncomeStatement,
  IncomeStatementAwaitingHandler,
  SelfEmployed
} from 'lib-common/generated/api-types/incomestatement'
import LocalDate from 'lib-common/local-date'

import { OtherIncome } from '../generated/enums'
import HelsinkiDateTime from '../helsinki-date-time'
import { JsonOf } from '../json'

export const otherIncome: OtherIncome[] = [
  'PENSION',
  'ADULT_EDUCATION_ALLOWANCE',
  'SICKNESS_ALLOWANCE',
  'PARENTAL_ALLOWANCE',
  'HOME_CARE_ALLOWANCE',
  'FLEXIBLE_AND_PARTIAL_HOME_CARE_ALLOWANCE',
  'ALIMONY',
  'INTEREST_AND_INVESTMENT_INCOME',
  'RENTAL_INCOME',
  'UNEMPLOYMENT_ALLOWANCE',
  'LABOUR_MARKET_SUBSIDY',
  'ADJUSTED_DAILY_ALLOWANCE',
  'JOB_ALTERNATION_COMPENSATION',
  'REWARD_OR_BONUS',
  'RELATIVE_CARE_SUPPORT',
  'BASIC_INCOME',
  'FOREST_INCOME',
  'FAMILY_CARE_COMPENSATION',
  'REHABILITATION',
  'EDUCATION_ALLOWANCE',
  'GRANT',
  'APPRENTICESHIP_SALARY',
  'ACCIDENT_INSURANCE_COMPENSATION',
  'OTHER_INCOME'
]

export function deserializeIncomeStatement(
  data: JsonOf<IncomeStatement>
): IncomeStatement {
  const startDate = LocalDate.parseIso(data.startDate)
  const endDate = data.endDate ? LocalDate.parseIso(data.endDate) : null
  const created = HelsinkiDateTime.parseIso(data.created)
  const updated = HelsinkiDateTime.parseIso(data.updated)
  switch (data.type) {
    case 'HIGHEST_FEE':
      return { ...data, startDate, endDate, created, updated }
    case 'CHILD_INCOME':
      return { ...data, startDate, endDate, created, updated }
    case 'INCOME':
      return {
        ...data,
        startDate,
        endDate,
        entrepreneur: deserializeEntrepreneur(data.entrepreneur),
        created,
        updated
      }
  }
}

function deserializeEntrepreneur(
  entrepreneur: JsonOf<Entrepreneur> | null
): Entrepreneur | null {
  if (!entrepreneur) return null
  return {
    ...entrepreneur,
    startOfEntrepreneurship: LocalDate.parseIso(
      entrepreneur.startOfEntrepreneurship
    ),
    selfEmployed: deserializeSelfEmployed(entrepreneur.selfEmployed)
  }
}

function deserializeSelfEmployed(
  selfEmployed: JsonOf<SelfEmployed> | null
): SelfEmployed | null {
  if (!selfEmployed) return null
  return {
    ...selfEmployed,
    estimatedIncome: deserializeEstimatedIncome(selfEmployed.estimatedIncome)
  }
}

function deserializeEstimatedIncome(
  estimatedIncome: JsonOf<EstimatedIncome> | null
): EstimatedIncome | null {
  if (!estimatedIncome) return null
  return {
    ...estimatedIncome,
    incomeStartDate: LocalDate.parseIso(estimatedIncome.incomeStartDate),
    incomeEndDate: estimatedIncome.incomeEndDate
      ? LocalDate.parseIso(estimatedIncome.incomeEndDate)
      : null
  }
}

export const deserializeIncomeStatementAwaitingHandler = (
  data: JsonOf<IncomeStatementAwaitingHandler>
): IncomeStatementAwaitingHandler => ({
  ...data,
  startDate: LocalDate.parseIso(data.startDate),
  created: HelsinkiDateTime.parseIso(data.created)
})
