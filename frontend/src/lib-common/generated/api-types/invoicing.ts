// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import DateRange from '../../date-range'
import LocalDate from '../../local-date'
import { DecisionIncome } from '../../api-types/income'
import { PlacementType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.invoicing.domain.ChildWithDateOfBirth
*/
export interface ChildWithDateOfBirth {
  dateOfBirth: LocalDate
  id: UUID
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.CreateRetroactiveFeeDecisionsBody
*/
export interface CreateRetroactiveFeeDecisionsBody {
  from: LocalDate
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.DistinctiveParams
*/
export type DistinctiveParams = 
  | 'UNCONFIRMED_HOURS'
  | 'EXTERNAL_CHILD'
  | 'RETROACTIVE'

/**
* Generated from fi.espoo.evaka.invoicing.domain.EmployeeWithName
*/
export interface EmployeeWithName {
  firstName: string
  id: UUID
  lastName: string
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
* Generated from fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
*/
export interface FeeAlterationWithEffect {
  amount: number
  effect: number
  isAbsolute: boolean
  type: Type
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecision
*/
export interface FeeDecision {
  approvedAt: Date | null
  approvedById: UUID | null
  children: FeeDecisionChild[]
  created: Date
  decisionHandlerId: UUID | null
  decisionNumber: number | null
  decisionType: FeeDecisionType
  documentKey: string | null
  familySize: number
  feeThresholds: FeeDecisionThresholds
  headOfFamilyId: UUID
  headOfFamilyIncome: DecisionIncome | null
  id: UUID
  partnerId: UUID | null
  partnerIncome: DecisionIncome | null
  sentAt: Date | null
  status: FeeDecisionStatus
  totalFee: number
  validDuring: DateRange
  validFrom: LocalDate
  validTo: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionChild
*/
export interface FeeDecisionChild {
  baseFee: number
  child: ChildWithDateOfBirth
  fee: number
  feeAlterations: FeeAlterationWithEffect[]
  finalFee: number
  placement: FeeDecisionPlacement
  serviceNeed: FeeDecisionServiceNeed
  siblingDiscount: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionChildDetailed
*/
export interface FeeDecisionChildDetailed {
  baseFee: number
  child: PersonDetailed
  fee: number
  feeAlterations: FeeAlterationWithEffect[]
  finalFee: number
  placementType: PlacementType
  placementUnit: UnitData
  serviceNeedDescriptionFi: string
  serviceNeedDescriptionSv: string
  serviceNeedFeeCoefficient: number
  serviceNeedMissing: boolean
  siblingDiscount: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
*/
export interface FeeDecisionDetailed {
  approvedAt: Date | null
  approvedBy: EmployeeWithName | null
  children: FeeDecisionChildDetailed[]
  created: Date
  decisionNumber: number | null
  decisionType: FeeDecisionType
  documentKey: string | null
  familySize: number
  feeThresholds: FeeDecisionThresholds
  financeDecisionHandlerFirstName: string | null
  financeDecisionHandlerLastName: string | null
  headOfFamily: PersonDetailed
  headOfFamilyIncome: DecisionIncome | null
  id: UUID
  incomeEffect: IncomeEffect
  isElementaryFamily: boolean | null
  isRetroactive: boolean
  partner: PersonDetailed | null
  partnerIncome: DecisionIncome | null
  requiresManualSending: boolean
  sentAt: Date | null
  status: FeeDecisionStatus
  totalFee: number
  totalIncome: number | null
  validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionPlacement
*/
export interface FeeDecisionPlacement {
  type: PlacementType
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
*/
export interface FeeDecisionServiceNeed {
  descriptionFi: string
  descriptionSv: string
  feeCoefficient: number
  missing: boolean
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
  children: PersonBasic[]
  created: Date
  decisionNumber: number | null
  finalPrice: number
  headOfFamily: PersonBasic
  id: UUID
  sentAt: Date | null
  status: FeeDecisionStatus
  validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionThresholds
*/
export interface FeeDecisionThresholds {
  incomeMultiplier: number
  maxFee: number
  maxIncomeThreshold: number
  minFee: number
  minIncomeThreshold: number
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
  temporaryFee: number
  temporaryFeePartDay: number
  temporaryFeeSibling: number
  temporaryFeeSiblingPartDay: number
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
  areaId: UUID
  codebtor: UUID | null
  dueDate: LocalDate
  headOfFamily: UUID
  id: UUID
  invoiceDate: LocalDate
  number: number | null
  periodEnd: LocalDate
  periodStart: LocalDate
  rows: InvoiceRow[]
  sentAt: Date | null
  sentBy: UUID | null
  status: InvoiceStatus
  totalPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.service.InvoiceCodes
*/
export interface InvoiceCodes {
  costCenters: string[]
  products: Product[]
  subCostCenters: string[]
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceDetailed
*/
export interface InvoiceDetailed {
  account: number
  agreementType: number | null
  areaId: UUID
  codebtor: PersonDetailed | null
  dueDate: LocalDate
  headOfFamily: PersonDetailed
  id: UUID
  invoiceDate: LocalDate
  number: number | null
  periodEnd: LocalDate
  periodStart: LocalDate
  rows: InvoiceRowDetailed[]
  sentAt: Date | null
  sentBy: UUID | null
  status: InvoiceStatus
  totalPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceDistinctiveParams
*/
export type InvoiceDistinctiveParams = 
  | 'MISSING_ADDRESS'

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
  child: ChildWithDateOfBirth
  costCenter: string
  description: string
  id: UUID | null
  periodEnd: LocalDate
  periodStart: LocalDate
  price: number
  product: Product
  subCostCenter: string | null
  unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
*/
export interface InvoiceRowDetailed {
  amount: number
  child: PersonDetailed
  costCenter: string
  description: string
  id: UUID
  periodEnd: LocalDate
  periodStart: LocalDate
  price: number
  product: Product
  subCostCenter: string | null
  unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceRowSummary
*/
export interface InvoiceRowSummary {
  amount: number
  child: PersonBasic
  id: UUID
  price: number
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
  codebtor: PersonDetailed | null
  createdAt: Date | null
  headOfFamily: PersonDetailed
  id: UUID
  periodEnd: LocalDate
  periodStart: LocalDate
  rows: InvoiceRowSummary[]
  sentAt: Date | null
  sentBy: UUID | null
  status: InvoiceStatus
  totalPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonBasic
*/
export interface PersonBasic {
  dateOfBirth: LocalDate
  firstName: string
  id: UUID
  lastName: string
  ssn: string | null
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.PersonDetailed
*/
export interface PersonDetailed {
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
  unit: UUID | null
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
  unit: UUID | null
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
* Generated from fi.espoo.evaka.invoicing.domain.UnitData
*/
export interface UnitData {
  areaId: UUID
  areaName: string
  id: UUID
  language: string
  name: string
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecision
*/
export interface VoucherValueDecision {
  ageCoefficient: number
  approvedAt: Date | null
  approvedById: UUID | null
  baseCoPayment: number
  baseValue: number
  capacityFactor: number
  child: ChildWithDateOfBirth
  coPayment: number
  created: Date
  decisionNumber: number | null
  decisionType: VoucherValueDecisionType
  documentKey: string | null
  familySize: number
  feeAlterations: FeeAlterationWithEffect[]
  feeThresholds: FeeDecisionThresholds
  finalCoPayment: number
  headOfFamilyId: UUID
  headOfFamilyIncome: DecisionIncome | null
  id: UUID
  partnerId: UUID | null
  partnerIncome: DecisionIncome | null
  placement: VoucherValueDecisionPlacement
  sentAt: Date | null
  serviceNeed: VoucherValueDecisionServiceNeed
  siblingDiscount: number
  status: VoucherValueDecisionStatus
  validFrom: LocalDate
  validTo: LocalDate | null
  voucherValue: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
*/
export interface VoucherValueDecisionDetailed {
  ageCoefficient: number
  approvedAt: Date | null
  approvedBy: EmployeeWithName | null
  baseCoPayment: number
  baseValue: number
  capacityFactor: number
  child: PersonDetailed
  childAge: number
  coPayment: number
  created: Date
  decisionNumber: number | null
  decisionType: VoucherValueDecisionType
  documentKey: string | null
  familySize: number
  feeAlterations: FeeAlterationWithEffect[]
  feeThresholds: FeeDecisionThresholds
  finalCoPayment: number
  financeDecisionHandlerFirstName: string | null
  financeDecisionHandlerLastName: string | null
  headOfFamily: PersonDetailed
  headOfFamilyIncome: DecisionIncome | null
  id: UUID
  incomeEffect: IncomeEffect
  isElementaryFamily: boolean | null
  isRetroactive: boolean
  partner: PersonDetailed | null
  partnerIncome: DecisionIncome | null
  placement: VoucherValueDecisionPlacementDetailed
  requiresManualSending: boolean
  sentAt: Date | null
  serviceNeed: VoucherValueDecisionServiceNeed
  siblingDiscount: number
  status: VoucherValueDecisionStatus
  totalIncome: number | null
  validFrom: LocalDate
  validTo: LocalDate | null
  voucherValue: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacement
*/
export interface VoucherValueDecisionPlacement {
  type: PlacementType
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionPlacementDetailed
*/
export interface VoucherValueDecisionPlacementDetailed {
  type: PlacementType
  unit: UnitData
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
*/
export interface VoucherValueDecisionServiceNeed {
  feeCoefficient: number
  feeDescriptionFi: string
  feeDescriptionSv: string
  voucherValueCoefficient: number
  voucherValueDescriptionFi: string
  voucherValueDescriptionSv: string
}

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
  child: PersonBasic
  created: Date
  decisionNumber: number | null
  finalCoPayment: number
  headOfFamily: PersonBasic
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
