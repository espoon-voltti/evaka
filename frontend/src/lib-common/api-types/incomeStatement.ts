// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Attachment } from 'lib-common/api-types/attachment'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { IncomeSource, OtherIncome } from '../generated/enums'
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

interface Base {
  id: UUID
  startDate: LocalDate
  endDate: LocalDate | null
  created: Date
  updated: Date
  handled: boolean
  handlerNote: string
}

export interface HighestFee extends Base {
  type: 'HIGHEST_FEE'
}

export interface ChildIncome extends Base {
  type: 'CHILD_INCOME'
  otherInfo: string
  attachments: IncomeStatementAttachment[]
}

export interface Income extends Base {
  type: 'INCOME'
  gross: Gross | null
  entrepreneur: Entrepreneur | null
  student: boolean
  alimonyPayer: boolean
  otherInfo: string
  attachments: IncomeStatementAttachment[]
}

export interface Gross {
  incomeSource: IncomeSource
  estimatedMonthlyIncome: number | null
  otherIncome: OtherIncome[]
  otherIncomeInfo: string
}

export interface Entrepreneur {
  fullTime: boolean
  startOfEntrepreneurship: LocalDate
  spouseWorksInCompany: boolean
  startupGrant: boolean
  checkupConsent: boolean
  selfEmployed: SelfEmployed | null
  limitedCompany: LimitedCompany | null
  partnership: boolean
  lightEntrepreneur: boolean
  accountant: Accountant | null
}

export interface SelfEmployed {
  attachments: boolean
  estimatedIncome: EstimatedIncome | null
}

export interface EstimatedIncome {
  estimatedMonthlyIncome: number
  incomeStartDate: LocalDate
  incomeEndDate: LocalDate | null
}

export interface LimitedCompany {
  incomeSource: IncomeSource
}

export interface Accountant {
  name: string
  email: string
  address: string
  phone: string
}

export interface IncomeStatementAttachment extends Attachment {
  uploadedByEmployee: boolean
}

export type IncomeStatement = HighestFee | Income | ChildIncome
type IncomeStatementType = IncomeStatement['type']

export interface IncomeStatementAwaitingHandler {
  id: UUID
  startDate: LocalDate
  created: Date
  type: IncomeStatementType
  personId: UUID
  personName: string
  primaryCareArea: string | null
}

type IncomeJson = JsonOf<Income>
type EntrepreneurJson = Exclude<IncomeJson['entrepreneur'], null>
type SelfEmployedJson = Exclude<EntrepreneurJson['selfEmployed'], null>
type EstimatedIncomeJson = Exclude<SelfEmployedJson['estimatedIncome'], null>

export function deserializeIncomeStatement(
  data: JsonOf<IncomeStatement>
): IncomeStatement {
  const startDate = LocalDate.parseIso(data.startDate)
  const endDate = data.endDate ? LocalDate.parseIso(data.endDate) : null
  const created = new Date(data.created)
  const updated = new Date(data.updated)
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
  entrepreneur: EntrepreneurJson | null
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
  selfEmployed: SelfEmployedJson | null
): SelfEmployed | null {
  if (!selfEmployed) return null
  return {
    ...selfEmployed,
    estimatedIncome: deserializeEstimatedIncome(selfEmployed.estimatedIncome)
  }
}

function deserializeEstimatedIncome(
  estimatedIncome: EstimatedIncomeJson | null
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
  created: new Date(data.created)
})
