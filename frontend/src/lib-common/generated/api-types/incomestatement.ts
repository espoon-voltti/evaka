// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { AttachmentId } from './shared'
import { DaycareId } from './shared'
import { IncomeStatementId } from './shared'
import { JsonOf } from '../../json'
import { PersonId } from './shared'
import { ProviderType } from './daycare'
import { SortDirection } from './invoicing'

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
* Generated from fi.espoo.evaka.incomestatement.ChildBasicInfo
*/
export interface ChildBasicInfo {
  firstName: string
  id: PersonId
  lastName: string
}

/**
* Generated from fi.espoo.evaka.incomestatement.Entrepreneur
*/
export interface Entrepreneur {
  accountant: Accountant | null
  businessId: string
  checkupConsent: boolean
  companyName: string
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


export namespace Gross {
  /**
  * Generated from fi.espoo.evaka.incomestatement.Gross.Income
  */
  export interface Income {
    type: 'INCOME'
    estimatedMonthlyIncome: number
    incomeSource: IncomeSource
    otherIncome: OtherIncome[]
    otherIncomeInfo: string
  }

  /**
  * Generated from fi.espoo.evaka.incomestatement.Gross.NoIncome
  */
  export interface NoIncome {
    type: 'NO_INCOME'
    noIncomeDescription: string
  }
}

/**
* Generated from fi.espoo.evaka.incomestatement.Gross
*/
export type Gross = Gross.Income | Gross.NoIncome


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
    attachments: IncomeStatementAttachment[]
    createdAt: HelsinkiDateTime
    endDate: LocalDate | null
    firstName: string
    handledAt: HelsinkiDateTime | null
    handlerNote: string
    id: IncomeStatementId
    lastName: string
    modifiedAt: HelsinkiDateTime
    otherInfo: string
    personId: PersonId
    sentAt: HelsinkiDateTime | null
    startDate: LocalDate
    status: IncomeStatementStatus
  }

  /**
  * Generated from fi.espoo.evaka.incomestatement.IncomeStatement.HighestFee
  */
  export interface HighestFee {
    type: 'HIGHEST_FEE'
    createdAt: HelsinkiDateTime
    endDate: LocalDate | null
    firstName: string
    handledAt: HelsinkiDateTime | null
    handlerNote: string
    id: IncomeStatementId
    lastName: string
    modifiedAt: HelsinkiDateTime
    personId: PersonId
    sentAt: HelsinkiDateTime | null
    startDate: LocalDate
    status: IncomeStatementStatus
  }

  /**
  * Generated from fi.espoo.evaka.incomestatement.IncomeStatement.Income
  */
  export interface Income {
    type: 'INCOME'
    alimonyPayer: boolean
    attachments: IncomeStatementAttachment[]
    createdAt: HelsinkiDateTime
    endDate: LocalDate | null
    entrepreneur: Entrepreneur | null
    firstName: string
    gross: Gross | null
    handledAt: HelsinkiDateTime | null
    handlerNote: string
    id: IncomeStatementId
    lastName: string
    modifiedAt: HelsinkiDateTime
    otherInfo: string
    personId: PersonId
    sentAt: HelsinkiDateTime | null
    startDate: LocalDate
    status: IncomeStatementStatus
    student: boolean
  }
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatement
*/
export type IncomeStatement = IncomeStatement.ChildIncome | IncomeStatement.HighestFee | IncomeStatement.Income


/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementAttachment
*/
export interface IncomeStatementAttachment {
  contentType: string
  id: AttachmentId
  name: string
  type: IncomeStatementAttachmentType | null
  uploadedByEmployee: boolean
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementAttachmentType
*/
export const incomeStatementAttachmentTypes = [
  'PAYSLIP_GROSS',
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
  'OTHER_INCOME',
  'STARTUP_GRANT',
  'PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED',
  'ACCOUNTANT_REPORT_LLC',
  'PAYSLIP_LLC',
  'ACCOUNTANT_REPORT_PARTNERSHIP',
  'PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP',
  'SALARY',
  'PROOF_OF_STUDIES',
  'ALIMONY_PAYOUT',
  'OTHER',
  'CHILD_INCOME'
] as const

export type IncomeStatementAttachmentType = typeof incomeStatementAttachmentTypes[number]

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementAwaitingHandler
*/
export interface IncomeStatementAwaitingHandler {
  handlerNote: string
  id: IncomeStatementId
  incomeEndDate: LocalDate | null
  personFirstName: string
  personId: PersonId
  personLastName: string
  personName: string
  primaryCareArea: string | null
  sentAt: HelsinkiDateTime
  startDate: LocalDate
  type: IncomeStatementType
}


export namespace IncomeStatementBody {
  /**
  * Generated from fi.espoo.evaka.incomestatement.IncomeStatementBody.ChildIncome
  */
  export interface ChildIncome {
    type: 'CHILD_INCOME'
    attachmentIds: AttachmentId[]
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
    attachmentIds: AttachmentId[]
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
  | 'SENT_AT'
  | 'START_DATE'
  | 'INCOME_END_DATE'
  | 'TYPE'
  | 'HANDLER_NOTE'
  | 'PERSON_NAME'

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementStatus
*/
export type IncomeStatementStatus =
  | 'DRAFT'
  | 'SENT'
  | 'HANDLED'

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
* Generated from fi.espoo.evaka.incomestatement.PartnerIncomeStatementStatus
*/
export interface PartnerIncomeStatementStatus {
  hasIncomeStatement: boolean
  name: string
}

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.PartnerIncomeStatementStatusResponse
*/
export interface PartnerIncomeStatementStatusResponse {
  partner: PartnerIncomeStatementStatus | null
}

/**
* Generated from fi.espoo.evaka.incomestatement.SearchIncomeStatementsRequest
*/
export interface SearchIncomeStatementsRequest {
  areas: string[] | null
  page: number
  placementValidDate: LocalDate | null
  providerTypes: ProviderType[] | null
  sentEndDate: LocalDate | null
  sentStartDate: LocalDate | null
  sortBy: IncomeStatementSortParam | null
  sortDirection: SortDirection | null
  unit: DaycareId | null
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

/**
* Generated from fi.espoo.evaka.incomestatement.IncomeStatementControllerCitizen.UpdateSentIncomeStatementBody
*/
export interface UpdateSentIncomeStatementBody {
  attachmentIds: AttachmentId[]
  otherInfo: string
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
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    handledAt: (json.handledAt != null) ? HelsinkiDateTime.parseIso(json.handledAt) : null,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}

export function deserializeJsonIncomeStatementHighestFee(json: JsonOf<IncomeStatement.HighestFee>): IncomeStatement.HighestFee {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    handledAt: (json.handledAt != null) ? HelsinkiDateTime.parseIso(json.handledAt) : null,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}

export function deserializeJsonIncomeStatementIncome(json: JsonOf<IncomeStatement.Income>): IncomeStatement.Income {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    entrepreneur: (json.entrepreneur != null) ? deserializeJsonEntrepreneur(json.entrepreneur) : null,
    handledAt: (json.handledAt != null) ? HelsinkiDateTime.parseIso(json.handledAt) : null,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    startDate: LocalDate.parseIso(json.startDate)
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
    incomeEndDate: (json.incomeEndDate != null) ? LocalDate.parseIso(json.incomeEndDate) : null,
    sentAt: HelsinkiDateTime.parseIso(json.sentAt),
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
