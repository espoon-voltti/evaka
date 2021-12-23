// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import DateRange from '../../date-range'
import LocalDate from '../../local-date'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonData.Basic
*/
export interface Basic {
  dateOfBirth: LocalDate
  firstName: string
  id: UUID
  lastName: string
  ssn: string | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.CreateRetroactiveFeeDecisionsBody
*/
export interface CreateRetroactiveFeeDecisionsBody {
  from: LocalDate
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonData.Detailed
*/
export interface Detailed {
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  email: string | null
  firstName: string
  forceManualFeeDecisions: boolean
  id: UUID
  invoiceRecipientName: string
  invoicingPostOffice: string
  invoicingPostalCode: string
  invoicingStreetAddress: string
  language: string | null
  lastName: string
  phone: string
  postOffice: string
  postalCode: string
  residenceCode: string
  restrictedDetailsEnabled: boolean
  ssn: string | null
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeAlteration
*/
export interface FeeAlteration {
  amount: number
  id: UUID | null
  isAbsolute: boolean
  notes: string
  personId: UUID
  type: Type
  updatedAt: Date | null
  updatedBy: UUID | null
  validFrom: LocalDate
  validTo: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionSortParam
*/
export type FeeDecisionSortParam = 
  | 'HEAD_OF_FAMILY'
  | 'VALIDITY'
  | 'NUMBER'
  | 'CREATED'
  | 'SENT'
  | 'STATUS'
  | 'FINAL_PRICE'

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
*/
export type FeeDecisionStatus = 
  | 'DRAFT'
  | 'WAITING_FOR_SENDING'
  | 'WAITING_FOR_MANUAL_SENDING'
  | 'SENT'
  | 'ANNULLED'

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
*/
export interface FeeDecisionSummary {
  approvedAt: Date | null
  children: Basic[]
  created: Date
  decisionNumber: number | null
  finalPrice: number
  headOfFamily: Basic
  id: UUID
  sentAt: Date | null
  status: FeeDecisionStatus
  validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionType
*/
export type FeeDecisionType = 
  | 'NORMAL'
  | 'RELIEF_REJECTED'
  | 'RELIEF_PARTLY_ACCEPTED'
  | 'RELIEF_ACCEPTED'

/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeDecisionTypeRequest
*/
export interface FeeDecisionTypeRequest {
  type: FeeDecisionType
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeThresholds
*/
export interface FeeThresholds {
  incomeMultiplier2: number
  incomeMultiplier3: number
  incomeMultiplier4: number
  incomeMultiplier5: number
  incomeMultiplier6: number
  incomeThresholdIncrease6Plus: number
  maxFee: number
  maxIncomeThreshold2: number
  maxIncomeThreshold3: number
  maxIncomeThreshold4: number
  maxIncomeThreshold5: number
  maxIncomeThreshold6: number
  minFee: number
  minIncomeThreshold2: number
  minIncomeThreshold3: number
  minIncomeThreshold4: number
  minIncomeThreshold5: number
  minIncomeThreshold6: number
  siblingDiscount2: number
  siblingDiscount2Plus: number
  validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeThresholdsWithId
*/
export interface FeeThresholdsWithId {
  id: UUID
  thresholds: FeeThresholds
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.GenerateDecisionsBody
*/
export interface GenerateDecisionsBody {
  starting: string
  targetHeads: UUID[]
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.IncomeEffect
*/
export type IncomeEffect = 
  | 'MAX_FEE_ACCEPTED'
  | 'INCOMPLETE'
  | 'INCOME'
  | 'NOT_AVAILABLE'

/**
* Generated from fi.espoo.evaka.invoicing.domain.Invoice
*/
export interface Invoice {
  agreementType: number
  dueDate: LocalDate
  headOfFamily: JustId
  id: UUID
  invoiceDate: LocalDate
  number: number | null
  periodEnd: LocalDate
  periodStart: LocalDate
  rows: InvoiceRow[]
  sentAt: Date | null
  sentBy: UUID | null
  status: InvoiceStatus
}

/**
* Generated from fi.espoo.evaka.invoicing.service.InvoiceCodes
*/
export interface InvoiceCodes {
  agreementTypes: number[]
  costCenters: string[]
  products: Product[]
  subCostCenters: string[]
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceDetailed
*/
export interface InvoiceDetailed {
  account: number
  agreementType: number
  dueDate: LocalDate
  headOfFamily: Detailed
  id: UUID
  invoiceDate: LocalDate
  number: number | null
  periodEnd: LocalDate
  periodStart: LocalDate
  rows: InvoiceRowDetailed[]
  sentAt: Date | null
  sentBy: UUID | null
  status: InvoiceStatus
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoicePayload
*/
export interface InvoicePayload {
  areas: string[]
  dueDate: LocalDate | null
  from: LocalDate
  invoiceDate: LocalDate | null
  to: LocalDate
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceRow
*/
export interface InvoiceRow {
  amount: number
  child: WithDateOfBirth
  costCenter: string
  description: string
  id: UUID | null
  periodEnd: LocalDate
  periodStart: LocalDate
  product: Product
  subCostCenter: string | null
  unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
*/
export interface InvoiceRowDetailed {
  amount: number
  child: Detailed
  costCenter: string
  description: string
  id: UUID
  periodEnd: LocalDate
  periodStart: LocalDate
  product: Product
  subCostCenter: string | null
  unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceRowSummary
*/
export interface InvoiceRowSummary {
  amount: number
  child: Basic
  id: UUID
  unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceSortParam
*/
export type InvoiceSortParam = 
  | 'HEAD_OF_FAMILY'
  | 'CHILDREN'
  | 'START'
  | 'END'
  | 'SUM'
  | 'STATUS'
  | 'CREATED_AT'

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceStatus
*/
export type InvoiceStatus = 
  | 'DRAFT'
  | 'WAITING_FOR_SENDING'
  | 'SENT'
  | 'CANCELED'

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceSummary
*/
export interface InvoiceSummary {
  account: number
  createdAt: Date | null
  headOfFamily: Detailed
  id: UUID
  periodEnd: LocalDate
  periodStart: LocalDate
  rows: InvoiceRowSummary[]
  sentAt: Date | null
  sentBy: UUID | null
  status: InvoiceStatus
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonData.JustId
*/
export interface JustId {
  id: UUID
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.Product
*/
export type Product = 
  | 'DAYCARE'
  | 'DAYCARE_DISCOUNT'
  | 'DAYCARE_INCREASE'
  | 'PRESCHOOL_WITH_DAYCARE'
  | 'PRESCHOOL_WITH_DAYCARE_DISCOUNT'
  | 'PRESCHOOL_WITH_DAYCARE_INCREASE'
  | 'TEMPORARY_CARE'
  | 'SCHOOL_SHIFT_CARE'
  | 'SICK_LEAVE_100'
  | 'SICK_LEAVE_50'
  | 'ABSENCE'
  | 'FREE_OF_CHARGE'

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchFeeDecisionRequest
*/
export interface SearchFeeDecisionRequest {
  area: string | null
  distinctions: string | null
  endDate: string | null
  financeDecisionHandlerId: UUID | null
  page: number
  pageSize: number
  searchByStartDate: boolean
  searchTerms: string | null
  sortBy: FeeDecisionSortParam | null
  sortDirection: SortDirection | null
  startDate: string | null
  status: string | null
  unit: string | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchInvoicesRequest
*/
export interface SearchInvoicesRequest {
  area: string | null
  distinctions: string | null
  page: number
  pageSize: number
  periodEnd: string | null
  periodStart: string | null
  searchTerms: string | null
  sortBy: InvoiceSortParam | null
  sortDirection: SortDirection | null
  status: string | null
  unit: UUID | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchVoucherValueDecisionRequest
*/
export interface SearchVoucherValueDecisionRequest {
  area: string | null
  endDate: string | null
  financeDecisionHandlerId: UUID | null
  page: number
  pageSize: number
  searchByStartDate: boolean
  searchTerms: string | null
  sortBy: VoucherValueDecisionSortParam | null
  sortDirection: SortDirection | null
  startDate: string | null
  status: string | null
  unit: string | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SortDirection
*/
export type SortDirection = 
  | 'ASC'
  | 'DESC'

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeAlteration.Type
*/
export type Type = 
  | 'DISCOUNT'
  | 'INCREASE'
  | 'RELIEF'

/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionSortParam
*/
export type VoucherValueDecisionSortParam = 
  | 'HEAD_OF_FAMILY'
  | 'STATUS'

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
*/
export type VoucherValueDecisionStatus = 
  | 'DRAFT'
  | 'WAITING_FOR_SENDING'
  | 'WAITING_FOR_MANUAL_SENDING'
  | 'SENT'
  | 'ANNULLED'

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
*/
export interface VoucherValueDecisionSummary {
  approvedAt: Date | null
  child: Basic
  created: Date
  decisionNumber: number | null
  finalCoPayment: number
  headOfFamily: Basic
  id: UUID
  sentAt: Date | null
  status: VoucherValueDecisionStatus
  validFrom: LocalDate
  validTo: LocalDate | null
  voucherValue: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionType
*/
export type VoucherValueDecisionType = 
  | 'NORMAL'
  | 'RELIEF_REJECTED'
  | 'RELIEF_PARTLY_ACCEPTED'
  | 'RELIEF_ACCEPTED'

/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionTypeRequest
*/
export interface VoucherValueDecisionTypeRequest {
  type: VoucherValueDecisionType
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonData.WithDateOfBirth
*/
export interface WithDateOfBirth {
  dateOfBirth: LocalDate
  id: UUID
}
