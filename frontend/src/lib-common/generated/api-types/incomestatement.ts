// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { ProviderType } from './daycare'
import { SortDirection } from './invoicing'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.incomestatement.Accountant
*/
export interface Accountant {
  address: string
  email: string
  name: string
  phone: string
}

/**
* Generated from fi.espoo.evaka.incomestatement.Attachment
*/
export interface Attachment {
  contentType: string
  id: UUID
  name: string
  uploadedByEmployee: boolean
}

/**
* Generated from fi.espoo.evaka.incomestatement.ChildBasicInfo
*/
export interface ChildBasicInfo {
  firstName: string
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.incomestatement.Entrepreneur
*/
export interface Entrepreneur {
  accountant: Accountant | null
  checkupConsent: boolean
  fullTime: boolean
  lightEntrepreneur: boolean
  limitedCompany: LimitedCompany | null
  partnership: boolean
  selfEmployed: SelfEmployed | null
  spouseWorksInCompany: boolean
  startOfEntrepreneurship: LocalDate
  startupGrant: boolean
}

/**
* Generated from fi.espoo.evaka.incomestatement.EstimatedIncome
*/
export interface EstimatedIncome {
  estimatedMonthlyIncome: number
  incomeEndDate: LocalDate | null
  incomeStartDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.incomestatement.Gross
*/
export interface Gross {
  estimatedMonthlyIncome: number
  incomeSource: IncomeSource
  otherIncome: OtherIncome[]
  otherIncomeInfo: string
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeSource
*/
export type IncomeSource =
  | 'INCOMES_REGISTER'
  | 'ATTACHMENTS'


export namespace IncomeStatement {
  /**
  * Generated from fi.espoo.evaka.incomestatement.IncomeStatement.ChildIncome
  */
  export interface ChildIncome {
    type: 'CHILD_INCOME'
    attachments: Attachment[]
    created: HelsinkiDateTime
    endDate: LocalDate | null
    firstName: string
    handled: boolean
    handlerNote: string
    id: UUID
    lastName: string
    otherInfo: string
    personId: UUID
    startDate: LocalDate
    updated: HelsinkiDateTime
  }

  /**
  * Generated from fi.espoo.evaka.incomestatement.IncomeStatement.HighestFee
  */
  export interface HighestFee {
    type: 'HIGHEST_FEE'
    created: HelsinkiDateTime
    endDate: LocalDate | null
    firstName: string
    handled: boolean
    handlerNote: string
    id: UUID
    lastName: string
    personId: UUID
    startDate: LocalDate
    updated: HelsinkiDateTime
  }

  /**
  * Generated from fi.espoo.evaka.incomestatement.IncomeStatement.Income
  */
  export interface Income {
    type: 'INCOME'
    alimonyPayer: boolean
    attachments: Attachment[]
    created: HelsinkiDateTime
    endDate: LocalDate | null
    entrepreneur: Entrepreneur | null
    firstName: string
    gross: Gross | null
    handled: boolean
    handlerNote: string
    id: UUID
    lastName: string
    otherInfo: string
    personId: UUID
    startDate: LocalDate
    student: boolean
    updated: HelsinkiDateTime
  }
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatement
*/
export type IncomeStatement = IncomeStatement.ChildIncome | IncomeStatement.HighestFee | IncomeStatement.Income


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementAwaitingHandler
*/
export interface IncomeStatementAwaitingHandler {
  created: HelsinkiDateTime
  handlerNote: string
  id: UUID
  personId: UUID
  personName: string
  primaryCareArea: string | null
  startDate: LocalDate
  type: IncomeStatementType
}


export namespace IncomeStatementBody {
  /**
  * Generated from fi.espoo.evaka.incomestatement.IncomeStatementBody.ChildIncome
  */
  export interface ChildIncome {
    type: 'CHILD_INCOME'
    attachmentIds: UUID[]
    endDate: LocalDate | null
    otherInfo: string
    startDate: LocalDate
  }

  /**
  * Generated from fi.espoo.evaka.incomestatement.IncomeStatementBody.HighestFee
  */
  export interface HighestFee {
    type: 'HIGHEST_FEE'
    endDate: LocalDate | null
    startDate: LocalDate
  }

  /**
  * Generated from fi.espoo.evaka.incomestatement.IncomeStatementBody.Income
  */
  export interface Income {
    type: 'INCOME'
    alimonyPayer: boolean
    attachmentIds: UUID[]
    endDate: LocalDate | null
    entrepreneur: Entrepreneur | null
    gross: Gross | null
    otherInfo: string
    startDate: LocalDate
    student: boolean
  }
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementBody
*/
export type IncomeStatementBody = IncomeStatementBody.ChildIncome | IncomeStatementBody.HighestFee | IncomeStatementBody.Income


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementSortParam
*/
export type IncomeStatementSortParam =
  | 'CREATED'
  | 'START_DATE'

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementType
*/
export type IncomeStatementType =
  | 'HIGHEST_FEE'
  | 'INCOME'
  | 'CHILD_INCOME'

/**
* Generated from fi.espoo.evaka.incomestatement.LimitedCompany
*/
export interface LimitedCompany {
  incomeSource: IncomeSource
}

/**
* Generated from fi.espoo.evaka.incomestatement.OtherIncome
*/
export const otherIncomes = [
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
] as const

export type OtherIncome = typeof otherIncomes[number]

/**
* Generated from fi.espoo.evaka.incomestatement.PagedIncomeStatements
*/
export interface PagedIncomeStatements {
  data: IncomeStatement[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.incomestatement.PagedIncomeStatementsAwaitingHandler
*/
export interface PagedIncomeStatementsAwaitingHandler {
  data: IncomeStatementAwaitingHandler[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.incomestatement.SearchIncomeStatementsRequest
*/
export interface SearchIncomeStatementsRequest {
  areas: string[] | null
  page: number
  pageSize: number
  placementValidDate: LocalDate | null
  providerTypes: ProviderType[] | null
  sentEndDate: LocalDate | null
  sentStartDate: LocalDate | null
  sortBy: IncomeStatementSortParam | null
  sortDirection: SortDirection | null
}

/**
* Generated from fi.espoo.evaka.incomestatement.SelfEmployed
*/
export interface SelfEmployed {
  attachments: boolean
  estimatedIncome: EstimatedIncome | null
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementController.SetIncomeStatementHandledBody
*/
export interface SetIncomeStatementHandledBody {
  handled: boolean
  handlerNote: string
}
