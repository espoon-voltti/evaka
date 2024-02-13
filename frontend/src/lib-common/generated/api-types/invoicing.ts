// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import DateRange from '../../date-range'
import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { Action } from '../action'
import { CareType } from './daycare'
import { JsonOf } from '../../json'
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
* Generated from fi.espoo.evaka.invoicing.domain.DecisionIncome
*/
export interface DecisionIncome {
  data: Record<string, number>
  effect: IncomeEffect
  total: number
  totalExpenses: number
  totalIncome: number
  worksAtECHA: boolean
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.DistinctiveParams
*/
export const feeDecisionDistinctiveParams = [
  'UNCONFIRMED_HOURS',
  'EXTERNAL_CHILD',
  'RETROACTIVE',
  'NO_STARTING_PLACEMENTS',
  'MAX_FEE_ACCEPTED',
  'PRESCHOOL_CLUB'
] as const

export type DistinctiveParams = typeof feeDecisionDistinctiveParams[number]

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
  attachments: FeeAlterationAttachment[]
  id: UUID | null
  isAbsolute: boolean
  notes: string
  personId: UUID
  type: FeeAlterationType
  updatedAt: HelsinkiDateTime | null
  updatedBy: UUID | null
  validFrom: LocalDate
  validTo: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeAlterationAttachment
*/
export interface FeeAlterationAttachment {
  contentType: string
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeAlterationType
*/
export const feeAlterationTypes = [
  'DISCOUNT',
  'INCREASE',
  'RELIEF'
] as const

export type FeeAlterationType = typeof feeAlterationTypes[number]

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
*/
export interface FeeAlterationWithEffect {
  amount: number
  effect: number
  isAbsolute: boolean
  type: FeeAlterationType
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.FeeAlterationController.FeeAlterationWithPermittedActions
*/
export interface FeeAlterationWithPermittedActions {
  data: FeeAlteration
  permittedActions: Action.FeeAlteration[]
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecision
*/
export interface FeeDecision {
  approvedAt: HelsinkiDateTime | null
  approvedById: UUID | null
  children: FeeDecisionChild[]
  created: HelsinkiDateTime
  decisionHandlerId: UUID | null
  decisionNumber: number | null
  decisionType: FeeDecisionType
  difference: FeeDecisionDifference[]
  documentKey: string | null
  familySize: number
  feeThresholds: FeeDecisionThresholds
  headOfFamilyId: UUID
  headOfFamilyIncome: DecisionIncome | null
  id: UUID
  partnerId: UUID | null
  partnerIncome: DecisionIncome | null
  sentAt: HelsinkiDateTime | null
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
  childIncome: DecisionIncome | null
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
  childIncome: DecisionIncome | null
  fee: number
  feeAlterations: FeeAlterationWithEffect[]
  finalFee: number
  placementType: PlacementType
  placementUnit: UnitData
  serviceNeedDescriptionFi: string
  serviceNeedDescriptionSv: string
  serviceNeedFeeCoefficient: number
  serviceNeedMissing: boolean
  serviceNeedOptionId: UUID | null
  siblingDiscount: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionDetailed
*/
export interface FeeDecisionDetailed {
  approvedAt: HelsinkiDateTime | null
  approvedBy: EmployeeWithName | null
  children: FeeDecisionChildDetailed[]
  created: HelsinkiDateTime
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
  isRetroactive: boolean
  partner: PersonDetailed | null
  partnerIncome: DecisionIncome | null
  partnerIsCodebtor: boolean | null
  requiresManualSending: boolean
  sentAt: HelsinkiDateTime | null
  status: FeeDecisionStatus
  totalFee: number
  totalIncome: number | null
  validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionDifference
*/
export const feeDecisionDifferences = [
  'GUARDIANS',
  'CHILDREN',
  'INCOME',
  'PLACEMENT',
  'SERVICE_NEED',
  'SIBLING_DISCOUNT',
  'FEE_ALTERATIONS',
  'FAMILY_SIZE',
  'FEE_THRESHOLDS'
] as const

export type FeeDecisionDifference = typeof feeDecisionDifferences[number]

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
  contractDaysPerMonth: number | null
  descriptionFi: string
  descriptionSv: string
  feeCoefficient: number
  missing: boolean
  optionId: UUID | null
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
  | 'IGNORED'
  | 'WAITING_FOR_SENDING'
  | 'WAITING_FOR_MANUAL_SENDING'
  | 'SENT'
  | 'ANNULLED'

/**
* Generated from fi.espoo.evaka.invoicing.domain.FeeDecisionSummary
*/
export interface FeeDecisionSummary {
  annullingDecision: boolean
  approvedAt: HelsinkiDateTime | null
  children: PersonBasic[]
  created: HelsinkiDateTime
  decisionNumber: number | null
  difference: FeeDecisionDifference[]
  finalPrice: number
  headOfFamily: PersonBasic
  id: UUID
  sentAt: HelsinkiDateTime | null
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
* Generated from fi.espoo.evaka.invoicing.domain.FinanceDecisionType
*/
export type FinanceDecisionType =
  | 'FEE_DECISION'
  | 'VOUCHER_VALUE_DECISION'

/**
* Generated from fi.espoo.evaka.invoicing.controller.GenerateDecisionsBody
*/
export interface GenerateDecisionsBody {
  starting: string
  targetHeads: (UUID | null)[]
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.IncomeCoefficient
*/
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

/**
* Generated from fi.espoo.evaka.invoicing.domain.IncomeEffect
*/
export type IncomeEffect =
  | 'MAX_FEE_ACCEPTED'
  | 'INCOMPLETE'
  | 'INCOME'
  | 'NOT_AVAILABLE'

/**
* Generated from fi.espoo.evaka.invoicing.service.IncomeNotification
*/
export interface IncomeNotification {
  created: HelsinkiDateTime
  notificationType: IncomeNotificationType
  receiverId: UUID
}

/**
* Generated from fi.espoo.evaka.invoicing.service.IncomeNotificationType
*/
export type IncomeNotificationType =
  | 'INITIAL_EMAIL'
  | 'REMINDER_EMAIL'
  | 'EXPIRED_EMAIL'

/**
* Generated from fi.espoo.evaka.invoicing.domain.IncomeType
*/
export interface IncomeType {
  isSubType: boolean
  multiplier: number
  nameFi: string
  withCoefficient: boolean
}

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
  sentAt: HelsinkiDateTime | null
  sentBy: UUID | null
  status: InvoiceStatus
  totalPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.service.InvoiceCodes
*/
export interface InvoiceCodes {
  products: ProductWithName[]
  units: InvoiceDaycare[]
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrection
*/
export interface InvoiceCorrection {
  amount: number
  childId: UUID
  description: string
  headOfFamilyId: UUID
  id: UUID
  invoiceId: UUID | null
  invoiceStatus: InvoiceStatus | null
  note: string
  period: FiniteDateRange
  product: string
  unitId: UUID
  unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrectionsController.InvoiceCorrectionWithPermittedActions
*/
export interface InvoiceCorrectionWithPermittedActions {
  data: InvoiceCorrection
  permittedActions: Action.InvoiceCorrection[]
}

/**
* Generated from fi.espoo.evaka.invoicing.service.InvoiceDaycare
*/
export interface InvoiceDaycare {
  costCenter: string | null
  id: UUID
  name: string
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
  sentAt: HelsinkiDateTime | null
  sentBy: UUID | null
  status: InvoiceStatus
  totalPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.InvoiceDetailedResponse
*/
export interface InvoiceDetailedResponse {
  data: InvoiceDetailed
  permittedActions: Action.Invoice[]
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
  child: UUID
  correctionId: UUID | null
  description: string
  id: UUID | null
  periodEnd: LocalDate
  periodStart: LocalDate
  price: number
  product: string
  unitId: UUID
  unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.InvoiceRowDetailed
*/
export interface InvoiceRowDetailed {
  amount: number
  child: PersonDetailed
  correctionId: UUID | null
  costCenter: string
  daycareType: CareType[]
  description: string
  id: UUID
  note: string | null
  periodEnd: LocalDate
  periodStart: LocalDate
  price: number
  product: string
  savedCostCenter: string | null
  subCostCenter: string | null
  unitId: UUID
  unitName: string
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
  createdAt: HelsinkiDateTime | null
  headOfFamily: PersonDetailed
  id: UUID
  periodEnd: LocalDate
  periodStart: LocalDate
  rows: InvoiceRowSummary[]
  sentAt: HelsinkiDateTime | null
  sentBy: UUID | null
  status: InvoiceStatus
  totalPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.InvoiceSummaryResponse
*/
export interface InvoiceSummaryResponse {
  data: InvoiceSummary
  permittedActions: Action.Invoice[]
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.NewInvoiceCorrection
*/
export interface NewInvoiceCorrection {
  amount: number
  childId: UUID
  description: string
  headOfFamilyId: UUID
  note: string
  period: FiniteDateRange
  product: string
  unitId: UUID
  unitPrice: number
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceCorrectionsController.NoteUpdateBody
*/
export interface NoteUpdateBody {
  note: string
}

/**
* Generated from fi.espoo.evaka.invoicing.data.PagedFeeDecisionSummaries
*/
export interface PagedFeeDecisionSummaries {
  data: FeeDecisionSummary[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.InvoiceController.PagedInvoiceSummaryResponses
*/
export interface PagedInvoiceSummaryResponses {
  data: InvoiceSummaryResponse[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.invoicing.data.PagedPayments
*/
export interface PagedPayments {
  data: Payment[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.invoicing.data.PagedVoucherValueDecisionSummaries
*/
export interface PagedVoucherValueDecisionSummaries {
  data: VoucherValueDecisionSummary[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.Payment
*/
export interface Payment {
  amount: number
  created: HelsinkiDateTime
  dueDate: LocalDate | null
  id: UUID
  number: number | null
  paymentDate: LocalDate | null
  period: DateRange
  sentAt: HelsinkiDateTime | null
  sentBy: UUID | null
  status: PaymentStatus
  unit: PaymentUnit
  updated: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentDistinctiveParams
*/
export type PaymentDistinctiveParams =
  | 'MISSING_PAYMENT_DETAILS'

/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentSortParam
*/
export type PaymentSortParam =
  | 'UNIT'
  | 'PERIOD'
  | 'CREATED'
  | 'NUMBER'
  | 'AMOUNT'

/**
* Generated from fi.espoo.evaka.invoicing.domain.PaymentStatus
*/
export type PaymentStatus =
  | 'DRAFT'
  | 'SENT'

/**
* Generated from fi.espoo.evaka.invoicing.domain.PaymentUnit
*/
export interface PaymentUnit {
  businessId: string | null
  careType: CareType[]
  iban: string | null
  id: UUID
  name: string
  providerId: string | null
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
* Generated from fi.espoo.evaka.invoicing.service.ProductWithName
*/
export interface ProductWithName {
  key: string
  nameFi: string
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchFeeDecisionRequest
*/
export interface SearchFeeDecisionRequest {
  area: string[] | null
  difference: FeeDecisionDifference[] | null
  distinctions: DistinctiveParams[] | null
  endDate: LocalDate | null
  financeDecisionHandlerId: UUID | null
  page: number
  pageSize: number
  searchByStartDate: boolean
  searchTerms: string | null
  sortBy: FeeDecisionSortParam | null
  sortDirection: SortDirection | null
  startDate: LocalDate | null
  statuses: FeeDecisionStatus[] | null
  unit: UUID | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchInvoicesRequest
*/
export interface SearchInvoicesRequest {
  area: string[] | null
  distinctions: InvoiceDistinctiveParams[] | null
  page: number
  pageSize: number
  periodEnd: LocalDate | null
  periodStart: LocalDate | null
  searchTerms: string | null
  sortBy: InvoiceSortParam | null
  sortDirection: SortDirection | null
  status: InvoiceStatus[] | null
  unit: UUID | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchPaymentsRequest
*/
export interface SearchPaymentsRequest {
  area: string[]
  distinctions: PaymentDistinctiveParams[]
  page: number
  pageSize: number
  paymentDateEnd: LocalDate | null
  paymentDateStart: LocalDate | null
  searchTerms: string
  sortBy: PaymentSortParam
  sortDirection: SortDirection
  status: PaymentStatus
  unit: UUID | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SearchVoucherValueDecisionRequest
*/
export interface SearchVoucherValueDecisionRequest {
  area: string[] | null
  difference: VoucherValueDecisionDifference[] | null
  distinctions: VoucherValueDecisionDistinctiveParams[] | null
  endDate: LocalDate | null
  financeDecisionHandlerId: UUID | null
  page: number
  pageSize: number
  searchByStartDate: boolean
  searchTerms: string | null
  sortBy: VoucherValueDecisionSortParam | null
  sortDirection: SortDirection | null
  startDate: LocalDate | null
  statuses: VoucherValueDecisionStatus[]
  unit: UUID | null
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.PaymentController.SendPaymentsRequest
*/
export interface SendPaymentsRequest {
  dueDate: LocalDate
  paymentDate: LocalDate
  paymentIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.SortDirection
*/
export type SortDirection =
  | 'ASC'
  | 'DESC'

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
  approvedAt: HelsinkiDateTime | null
  approvedById: UUID | null
  assistanceNeedCoefficient: number
  baseCoPayment: number
  baseValue: number
  child: ChildWithDateOfBirth
  childIncome: DecisionIncome | null
  coPayment: number
  created: HelsinkiDateTime
  decisionHandler: UUID | null
  decisionNumber: number | null
  decisionType: VoucherValueDecisionType
  difference: VoucherValueDecisionDifference[]
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
  placement: VoucherValueDecisionPlacement | null
  sentAt: HelsinkiDateTime | null
  serviceNeed: VoucherValueDecisionServiceNeed | null
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
  approvedAt: HelsinkiDateTime | null
  approvedBy: EmployeeWithName | null
  assistanceNeedCoefficient: number
  baseCoPayment: number
  baseValue: number
  child: PersonDetailed
  childAge: number
  childIncome: DecisionIncome | null
  coPayment: number
  created: HelsinkiDateTime
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
  isRetroactive: boolean
  partner: PersonDetailed | null
  partnerIncome: DecisionIncome | null
  partnerIsCodebtor: boolean | null
  placement: VoucherValueDecisionPlacementDetailed
  requiresManualSending: boolean
  sentAt: HelsinkiDateTime | null
  serviceNeed: VoucherValueDecisionServiceNeed
  siblingDiscount: number
  status: VoucherValueDecisionStatus
  totalIncome: number | null
  validFrom: LocalDate
  validTo: LocalDate | null
  voucherValue: number
}

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDifference
*/
export const voucherValueDecisionDifferences = [
  'GUARDIANS',
  'INCOME',
  'FAMILY_SIZE',
  'PLACEMENT',
  'SERVICE_NEED',
  'SIBLING_DISCOUNT',
  'CO_PAYMENT',
  'FEE_ALTERATIONS',
  'FINAL_CO_PAYMENT',
  'BASE_VALUE',
  'VOUCHER_VALUE',
  'FEE_THRESHOLDS'
] as const

export type VoucherValueDecisionDifference = typeof voucherValueDecisionDifferences[number]

/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionDistinctiveParams
*/
export const voucherValueDecisionDistinctiveParams = [
  'UNCONFIRMED_HOURS',
  'EXTERNAL_CHILD',
  'RETROACTIVE',
  'NO_STARTING_PLACEMENTS',
  'MAX_FEE_ACCEPTED'
] as const

export type VoucherValueDecisionDistinctiveParams = typeof voucherValueDecisionDistinctiveParams[number]

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
  missing: boolean
  voucherValueCoefficient: number
  voucherValueDescriptionFi: string
  voucherValueDescriptionSv: string
}

/**
* Generated from fi.espoo.evaka.invoicing.controller.VoucherValueDecisionSortParam
*/
export type VoucherValueDecisionSortParam =
  | 'HEAD_OF_FAMILY'
  | 'CHILD'
  | 'VALIDITY'
  | 'VOUCHER_VALUE'
  | 'FINAL_CO_PAYMENT'
  | 'NUMBER'
  | 'CREATED'
  | 'SENT'
  | 'STATUS'

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
*/
export type VoucherValueDecisionStatus =
  | 'DRAFT'
  | 'IGNORED'
  | 'WAITING_FOR_SENDING'
  | 'WAITING_FOR_MANUAL_SENDING'
  | 'SENT'
  | 'ANNULLED'

/**
* Generated from fi.espoo.evaka.invoicing.domain.VoucherValueDecisionSummary
*/
export interface VoucherValueDecisionSummary {
  annullingDecision: boolean
  approvedAt: HelsinkiDateTime | null
  child: PersonBasic
  created: HelsinkiDateTime
  decisionNumber: number | null
  difference: VoucherValueDecisionDifference[]
  finalCoPayment: number
  headOfFamily: PersonBasic
  id: UUID
  sentAt: HelsinkiDateTime | null
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


export function deserializeJsonChildWithDateOfBirth(json: JsonOf<ChildWithDateOfBirth>): ChildWithDateOfBirth {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}


export function deserializeJsonCreateRetroactiveFeeDecisionsBody(json: JsonOf<CreateRetroactiveFeeDecisionsBody>): CreateRetroactiveFeeDecisionsBody {
  return {
    ...json,
    from: LocalDate.parseIso(json.from)
  }
}


export function deserializeJsonFeeAlteration(json: JsonOf<FeeAlteration>): FeeAlteration {
  return {
    ...json,
    updatedAt: (json.updatedAt != null) ? HelsinkiDateTime.parseIso(json.updatedAt) : null,
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonFeeAlterationWithPermittedActions(json: JsonOf<FeeAlterationWithPermittedActions>): FeeAlterationWithPermittedActions {
  return {
    ...json,
    data: deserializeJsonFeeAlteration(json.data)
  }
}


export function deserializeJsonFeeDecision(json: JsonOf<FeeDecision>): FeeDecision {
  return {
    ...json,
    approvedAt: (json.approvedAt != null) ? HelsinkiDateTime.parseIso(json.approvedAt) : null,
    children: json.children.map(e => deserializeJsonFeeDecisionChild(e)),
    created: HelsinkiDateTime.parseIso(json.created),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    validDuring: DateRange.parseJson(json.validDuring),
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonFeeDecisionChild(json: JsonOf<FeeDecisionChild>): FeeDecisionChild {
  return {
    ...json,
    child: deserializeJsonChildWithDateOfBirth(json.child)
  }
}


export function deserializeJsonFeeDecisionChildDetailed(json: JsonOf<FeeDecisionChildDetailed>): FeeDecisionChildDetailed {
  return {
    ...json,
    child: deserializeJsonPersonDetailed(json.child)
  }
}


export function deserializeJsonFeeDecisionDetailed(json: JsonOf<FeeDecisionDetailed>): FeeDecisionDetailed {
  return {
    ...json,
    approvedAt: (json.approvedAt != null) ? HelsinkiDateTime.parseIso(json.approvedAt) : null,
    children: json.children.map(e => deserializeJsonFeeDecisionChildDetailed(e)),
    created: HelsinkiDateTime.parseIso(json.created),
    headOfFamily: deserializeJsonPersonDetailed(json.headOfFamily),
    partner: (json.partner != null) ? deserializeJsonPersonDetailed(json.partner) : null,
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    validDuring: DateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonFeeDecisionSummary(json: JsonOf<FeeDecisionSummary>): FeeDecisionSummary {
  return {
    ...json,
    approvedAt: (json.approvedAt != null) ? HelsinkiDateTime.parseIso(json.approvedAt) : null,
    children: json.children.map(e => deserializeJsonPersonBasic(e)),
    created: HelsinkiDateTime.parseIso(json.created),
    headOfFamily: deserializeJsonPersonBasic(json.headOfFamily),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    validDuring: DateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonFeeThresholds(json: JsonOf<FeeThresholds>): FeeThresholds {
  return {
    ...json,
    validDuring: DateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonFeeThresholdsWithId(json: JsonOf<FeeThresholdsWithId>): FeeThresholdsWithId {
  return {
    ...json,
    thresholds: deserializeJsonFeeThresholds(json.thresholds)
  }
}


export function deserializeJsonIncomeNotification(json: JsonOf<IncomeNotification>): IncomeNotification {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created)
  }
}


export function deserializeJsonInvoice(json: JsonOf<Invoice>): Invoice {
  return {
    ...json,
    dueDate: LocalDate.parseIso(json.dueDate),
    invoiceDate: LocalDate.parseIso(json.invoiceDate),
    periodEnd: LocalDate.parseIso(json.periodEnd),
    periodStart: LocalDate.parseIso(json.periodStart),
    rows: json.rows.map(e => deserializeJsonInvoiceRow(e)),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null
  }
}


export function deserializeJsonInvoiceCorrection(json: JsonOf<InvoiceCorrection>): InvoiceCorrection {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period)
  }
}


export function deserializeJsonInvoiceCorrectionWithPermittedActions(json: JsonOf<InvoiceCorrectionWithPermittedActions>): InvoiceCorrectionWithPermittedActions {
  return {
    ...json,
    data: deserializeJsonInvoiceCorrection(json.data)
  }
}


export function deserializeJsonInvoiceDetailed(json: JsonOf<InvoiceDetailed>): InvoiceDetailed {
  return {
    ...json,
    codebtor: (json.codebtor != null) ? deserializeJsonPersonDetailed(json.codebtor) : null,
    dueDate: LocalDate.parseIso(json.dueDate),
    headOfFamily: deserializeJsonPersonDetailed(json.headOfFamily),
    invoiceDate: LocalDate.parseIso(json.invoiceDate),
    periodEnd: LocalDate.parseIso(json.periodEnd),
    periodStart: LocalDate.parseIso(json.periodStart),
    rows: json.rows.map(e => deserializeJsonInvoiceRowDetailed(e)),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null
  }
}


export function deserializeJsonInvoiceDetailedResponse(json: JsonOf<InvoiceDetailedResponse>): InvoiceDetailedResponse {
  return {
    ...json,
    data: deserializeJsonInvoiceDetailed(json.data)
  }
}


export function deserializeJsonInvoicePayload(json: JsonOf<InvoicePayload>): InvoicePayload {
  return {
    ...json,
    dueDate: (json.dueDate != null) ? LocalDate.parseIso(json.dueDate) : null,
    from: LocalDate.parseIso(json.from),
    invoiceDate: (json.invoiceDate != null) ? LocalDate.parseIso(json.invoiceDate) : null,
    to: LocalDate.parseIso(json.to)
  }
}


export function deserializeJsonInvoiceRow(json: JsonOf<InvoiceRow>): InvoiceRow {
  return {
    ...json,
    periodEnd: LocalDate.parseIso(json.periodEnd),
    periodStart: LocalDate.parseIso(json.periodStart)
  }
}


export function deserializeJsonInvoiceRowDetailed(json: JsonOf<InvoiceRowDetailed>): InvoiceRowDetailed {
  return {
    ...json,
    child: deserializeJsonPersonDetailed(json.child),
    periodEnd: LocalDate.parseIso(json.periodEnd),
    periodStart: LocalDate.parseIso(json.periodStart)
  }
}


export function deserializeJsonInvoiceRowSummary(json: JsonOf<InvoiceRowSummary>): InvoiceRowSummary {
  return {
    ...json,
    child: deserializeJsonPersonBasic(json.child)
  }
}


export function deserializeJsonInvoiceSummary(json: JsonOf<InvoiceSummary>): InvoiceSummary {
  return {
    ...json,
    codebtor: (json.codebtor != null) ? deserializeJsonPersonDetailed(json.codebtor) : null,
    createdAt: (json.createdAt != null) ? HelsinkiDateTime.parseIso(json.createdAt) : null,
    headOfFamily: deserializeJsonPersonDetailed(json.headOfFamily),
    periodEnd: LocalDate.parseIso(json.periodEnd),
    periodStart: LocalDate.parseIso(json.periodStart),
    rows: json.rows.map(e => deserializeJsonInvoiceRowSummary(e)),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null
  }
}


export function deserializeJsonInvoiceSummaryResponse(json: JsonOf<InvoiceSummaryResponse>): InvoiceSummaryResponse {
  return {
    ...json,
    data: deserializeJsonInvoiceSummary(json.data)
  }
}


export function deserializeJsonNewInvoiceCorrection(json: JsonOf<NewInvoiceCorrection>): NewInvoiceCorrection {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period)
  }
}


export function deserializeJsonPagedFeeDecisionSummaries(json: JsonOf<PagedFeeDecisionSummaries>): PagedFeeDecisionSummaries {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonFeeDecisionSummary(e))
  }
}


export function deserializeJsonPagedInvoiceSummaryResponses(json: JsonOf<PagedInvoiceSummaryResponses>): PagedInvoiceSummaryResponses {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonInvoiceSummaryResponse(e))
  }
}


export function deserializeJsonPagedPayments(json: JsonOf<PagedPayments>): PagedPayments {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonPayment(e))
  }
}


export function deserializeJsonPagedVoucherValueDecisionSummaries(json: JsonOf<PagedVoucherValueDecisionSummaries>): PagedVoucherValueDecisionSummaries {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonVoucherValueDecisionSummary(e))
  }
}


export function deserializeJsonPayment(json: JsonOf<Payment>): Payment {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    dueDate: (json.dueDate != null) ? LocalDate.parseIso(json.dueDate) : null,
    paymentDate: (json.paymentDate != null) ? LocalDate.parseIso(json.paymentDate) : null,
    period: DateRange.parseJson(json.period),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}


export function deserializeJsonPersonBasic(json: JsonOf<PersonBasic>): PersonBasic {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}


export function deserializeJsonPersonDetailed(json: JsonOf<PersonDetailed>): PersonDetailed {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    dateOfDeath: (json.dateOfDeath != null) ? LocalDate.parseIso(json.dateOfDeath) : null
  }
}


export function deserializeJsonSearchFeeDecisionRequest(json: JsonOf<SearchFeeDecisionRequest>): SearchFeeDecisionRequest {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: (json.startDate != null) ? LocalDate.parseIso(json.startDate) : null
  }
}


export function deserializeJsonSearchInvoicesRequest(json: JsonOf<SearchInvoicesRequest>): SearchInvoicesRequest {
  return {
    ...json,
    periodEnd: (json.periodEnd != null) ? LocalDate.parseIso(json.periodEnd) : null,
    periodStart: (json.periodStart != null) ? LocalDate.parseIso(json.periodStart) : null
  }
}


export function deserializeJsonSearchPaymentsRequest(json: JsonOf<SearchPaymentsRequest>): SearchPaymentsRequest {
  return {
    ...json,
    paymentDateEnd: (json.paymentDateEnd != null) ? LocalDate.parseIso(json.paymentDateEnd) : null,
    paymentDateStart: (json.paymentDateStart != null) ? LocalDate.parseIso(json.paymentDateStart) : null
  }
}


export function deserializeJsonSearchVoucherValueDecisionRequest(json: JsonOf<SearchVoucherValueDecisionRequest>): SearchVoucherValueDecisionRequest {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: (json.startDate != null) ? LocalDate.parseIso(json.startDate) : null
  }
}


export function deserializeJsonSendPaymentsRequest(json: JsonOf<SendPaymentsRequest>): SendPaymentsRequest {
  return {
    ...json,
    dueDate: LocalDate.parseIso(json.dueDate),
    paymentDate: LocalDate.parseIso(json.paymentDate)
  }
}


export function deserializeJsonVoucherValueDecision(json: JsonOf<VoucherValueDecision>): VoucherValueDecision {
  return {
    ...json,
    approvedAt: (json.approvedAt != null) ? HelsinkiDateTime.parseIso(json.approvedAt) : null,
    child: deserializeJsonChildWithDateOfBirth(json.child),
    created: HelsinkiDateTime.parseIso(json.created),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonVoucherValueDecisionDetailed(json: JsonOf<VoucherValueDecisionDetailed>): VoucherValueDecisionDetailed {
  return {
    ...json,
    approvedAt: (json.approvedAt != null) ? HelsinkiDateTime.parseIso(json.approvedAt) : null,
    child: deserializeJsonPersonDetailed(json.child),
    created: HelsinkiDateTime.parseIso(json.created),
    headOfFamily: deserializeJsonPersonDetailed(json.headOfFamily),
    partner: (json.partner != null) ? deserializeJsonPersonDetailed(json.partner) : null,
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonVoucherValueDecisionSummary(json: JsonOf<VoucherValueDecisionSummary>): VoucherValueDecisionSummary {
  return {
    ...json,
    approvedAt: (json.approvedAt != null) ? HelsinkiDateTime.parseIso(json.approvedAt) : null,
    child: deserializeJsonPersonBasic(json.child),
    created: HelsinkiDateTime.parseIso(json.created),
    headOfFamily: deserializeJsonPersonBasic(json.headOfFamily),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null,
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}
