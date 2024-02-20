// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { JsonOf } from '../../json'
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


export function deserializeJsonEntrepreneur(json: JsonOf<Entrepreneur>): Entrepreneur {
  return {
    ...json,
    selfEmployed: (json.selfEmployed != null) ? deserializeJsonSelfEmployed(json.selfEmployed) : null,
    startOfEntrepreneurship: LocalDate.parseIso(json.startOfEntrepreneurship)
  }
}


export function deserializeJsonEstimatedIncome(json: JsonOf<EstimatedIncome>): EstimatedIncome {
  return {
    ...json,
    incomeEndDate: (json.incomeEndDate != null) ? LocalDate.parseIso(json.incomeEndDate) : null,
    incomeStartDate: LocalDate.parseIso(json.incomeStartDate)
  }
}



export function deserializeJsonIncomeStatementChildIncome(json: JsonOf<IncomeStatement.ChildIncome>): IncomeStatement.ChildIncome {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate),
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}

export function deserializeJsonIncomeStatementHighestFee(json: JsonOf<IncomeStatement.HighestFee>): IncomeStatement.HighestFee {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate),
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}

export function deserializeJsonIncomeStatementIncome(json: JsonOf<IncomeStatement.Income>): IncomeStatement.Income {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    entrepreneur: (json.entrepreneur != null) ? deserializeJsonEntrepreneur(json.entrepreneur) : null,
    startDate: LocalDate.parseIso(json.startDate),
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}
export function deserializeJsonIncomeStatement(json: JsonOf<IncomeStatement>): IncomeStatement {
  switch (json.type) {
    case 'CHILD_INCOME': return deserializeJsonIncomeStatementChildIncome(json)
    case 'HIGHEST_FEE': return deserializeJsonIncomeStatementHighestFee(json)
    case 'INCOME': return deserializeJsonIncomeStatementIncome(json)
    default: return json
  }
}


export function deserializeJsonIncomeStatementAwaitingHandler(json: JsonOf<IncomeStatementAwaitingHandler>): IncomeStatementAwaitingHandler {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    startDate: LocalDate.parseIso(json.startDate)
  }
}



export function deserializeJsonIncomeStatementBodyChildIncome(json: JsonOf<IncomeStatementBody.ChildIncome>): IncomeStatementBody.ChildIncome {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}

export function deserializeJsonIncomeStatementBodyHighestFee(json: JsonOf<IncomeStatementBody.HighestFee>): IncomeStatementBody.HighestFee {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}

export function deserializeJsonIncomeStatementBodyIncome(json: JsonOf<IncomeStatementBody.Income>): IncomeStatementBody.Income {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    entrepreneur: (json.entrepreneur != null) ? deserializeJsonEntrepreneur(json.entrepreneur) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}
export function deserializeJsonIncomeStatementBody(json: JsonOf<IncomeStatementBody>): IncomeStatementBody {
  switch (json.type) {
    case 'CHILD_INCOME': return deserializeJsonIncomeStatementBodyChildIncome(json)
    case 'HIGHEST_FEE': return deserializeJsonIncomeStatementBodyHighestFee(json)
    case 'INCOME': return deserializeJsonIncomeStatementBodyIncome(json)
    default: return json
  }
}


export function deserializeJsonPagedIncomeStatements(json: JsonOf<PagedIncomeStatements>): PagedIncomeStatements {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonIncomeStatement(e))
  }
}


export function deserializeJsonPagedIncomeStatementsAwaitingHandler(json: JsonOf<PagedIncomeStatementsAwaitingHandler>): PagedIncomeStatementsAwaitingHandler {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonIncomeStatementAwaitingHandler(e))
  }
}


export function deserializeJsonSearchIncomeStatementsRequest(json: JsonOf<SearchIncomeStatementsRequest>): SearchIncomeStatementsRequest {
  return {
    ...json,
    placementValidDate: (json.placementValidDate != null) ? LocalDate.parseIso(json.placementValidDate) : null,
    sentEndDate: (json.sentEndDate != null) ? LocalDate.parseIso(json.sentEndDate) : null,
    sentStartDate: (json.sentStartDate != null) ? LocalDate.parseIso(json.sentStartDate) : null
  }
}


export function deserializeJsonSelfEmployed(json: JsonOf<SelfEmployed>): SelfEmployed {
  return {
    ...json,
    estimatedIncome: (json.estimatedIncome != null) ? deserializeJsonEstimatedIncome(json.estimatedIncome) : null
  }
}
