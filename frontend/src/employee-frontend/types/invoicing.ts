// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PlacementType } from 'lib-common/api-types/serviceNeed/common'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from '../types'
import { FeeAlterationType } from '../types/fee-alteration'
import { Income, IncomeEffect } from '../types/income'

// Enums

export type FeeDecisionStatus =
  | 'DRAFT'
  | 'WAITING_FOR_SENDING'
  | 'WAITING_FOR_MANUAL_SENDING'
  | 'SENT'
  | 'ANNULLED'

export type DecisionDistinctiveDetails =
  | 'UNCONFIRMED_HOURS'
  | 'EXTERNAL_CHILD'
  | 'RETROACTIVE'

export type VoucherValueDecisionStatus =
  | 'DRAFT'
  | 'WAITING_FOR_SENDING'
  | 'WAITING_FOR_MANUAL_SENDING'
  | 'SENT'
  | 'ANNULLED'

export type InvoiceStatus =
  | 'DRAFT'
  | 'WAITING_FOR_SENDING'
  | 'SENT'
  | 'CANCELED'

export type FeeDecisionPlacementType =
  | 'DAYCARE'
  | 'FIVE_YEARS_OLD_DAYCARE'
  | 'PRESCHOOL_WITH_DAYCARE'
  | 'PREPARATORY_WITH_DAYCARE'

export type ServiceNeed =
  | 'MISSING'
  | 'GTE_35'
  | 'GTE_25'
  | 'GT_25_LT_35'
  | 'GT_15_LT_25'
  | 'LTE_25'
  | 'LTE_15'
  | 'LTE_0'

export type Product =
  | 'DAYCARE'
  | 'DAYCARE_DISCOUNT'
  | 'DAYCARE_INCREASE'
  | 'PRESCHOOL_WITH_DAYCARE'
  | 'PRESCHOOL_WITH_DAYCARE_DISCOUNT'
  | 'PRESCHOOL_WITH_DAYCARE_INCREASE'
  | 'TEMPORARY_CARE'
  | 'SICK_LEAVE_100'
  | 'SICK_LEAVE_50'
  | 'ABSENCE'
  | 'EU_CHEMICAL_AGENCY'
  | 'OTHER_MUNICIPALITY'
  | 'FREE_OF_CHARGE'

export type InvoiceDistinctiveDetails = 'MISSING_ADDRESS'

export type FeeDecisionType =
  | 'NORMAL'
  | 'RELIEF_REJECTED'
  | 'RELIEF_PARTLY_ACCEPTED'
  | 'RELIEF_ACCEPTED'

// Other types and interfaces
// + accompanying deserialization methods

interface Periodic {
  periodStart: LocalDate
  periodEnd: LocalDate
}

export const deserializePeriodic = <E extends Periodic>(json: JsonOf<E>): E => {
  return {
    ...json,
    periodStart: LocalDate.parseIso(json.periodStart),
    periodEnd: LocalDate.parseIso(json.periodEnd)
  } as E
}

interface PersonBasic {
  id: UUID
  dateOfBirth: LocalDate
  firstName: string
  lastName: string
  ssn: string | null
}

export interface PersonDetailed {
  id: UUID
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  firstName: string
  lastName: string
  ssn: string | null
  streetAddress: string | null
  postalCode: string | null
  postOffice: string | null
  residenceCode: string | null
  restrictedDetailsEnabled: boolean
  email: string | null
  phone: string | null
  language: string | null
  invoiceRecipientName: string
  invoicingStreetAddress: string
  invoicingPostalCode: string
  invoicingPostOffice: string
  forceManualFeeDecisions: boolean
}

export const deserializePersonBasic = (
  json: JsonOf<PersonBasic>
): PersonBasic => ({
  ...json,
  dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
})

export const deserializePersonDetailed = (
  json: JsonOf<PersonDetailed>
): PersonDetailed => ({
  ...json,
  dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
  dateOfDeath: LocalDate.parseNullableIso(json.dateOfDeath)
})

export interface Placement {
  unit: UUID
  type: FeeDecisionPlacementType
  serviceNeed: ServiceNeed
}

export interface InvoiceCodes {
  products: Product[]
  agreementTypes: number[]
  subCostCenters: string[]
  costCenters: string[]
}

export interface Unit {
  id: UUID
  name: string
}

export interface UnitDetailed {
  id: UUID
  name: string
  areaId: UUID
  areaName: string
  language: string
}

export interface InvoiceSummary extends Periodic {
  id: UUID
  status: InvoiceStatus
  headOfFamily: PersonDetailed
  rows: Array<{ child: PersonBasic }>
  totalPrice: number
  createdAt: Date | null
}

export interface InvoiceRowDetailed extends Periodic {
  id: UUID | null
  child: PersonDetailed
  amount: number
  unitPrice: number
  product: Product
  costCenter: string
  subCostCenter: string | null
  description: string
  price: number
}

/**
 * TODO: Update /invoices/head-of-family/{uuid} to return InvoiceSummary instead and ditch this type
 */
export interface Invoice extends Periodic {
  id: UUID
  status: InvoiceStatus
  sentAt: Date | null
  totalPrice: number
}

export interface InvoiceDetailed extends Periodic {
  id: UUID
  status: InvoiceStatus
  dueDate: LocalDate
  invoiceDate: LocalDate
  agreementType: number
  headOfFamily: PersonDetailed
  rows: InvoiceRowDetailed[]
  number: string | null
  sentAt: Date | null
  account: number
  totalPrice: number
}

interface FeeDecisionAlteration {
  type: FeeAlterationType
  amount: number
  isAbsolute: boolean
  effect: number
}

export interface FeeDecisionPartDetailed {
  child: PersonDetailed
  placement: Placement
  placementUnit: UnitDetailed
  baseFee: number
  siblingDiscount: number
  fee: number
  feeAlterations: FeeDecisionAlteration[]
  finalFee: number
  serviceNeedMultiplier: number
}

/**
 * TODO: Update /decisions/head-of-family/{uuid} to return FeeDecisionSummary instead and ditch this type
 */
export interface FeeDecision {
  id: UUID
  status: FeeDecisionStatus
  decisionNumber: number | null
  validFrom: LocalDate
  validTo: LocalDate | null
  createdAt: Date
  sentAt: Date | null
  totalFee: number
}

export interface FeeDecisionDetailed {
  id: UUID
  status: FeeDecisionStatus
  decisionNumber: number | null
  decisionType: FeeDecisionType
  validFrom: LocalDate
  validTo: LocalDate | null
  headOfFamily: PersonDetailed
  partner: PersonDetailed | null
  headOfFamilyIncome: Income | null
  partnerIncome: Income | null
  familySize: number
  parts: FeeDecisionPartDetailed[]
  documentKey: string | null
  approvedAt: Date | null
  createdAt: Date
  sentAt: Date | null
  financeDecisionHandlerName: string | null
  approvedBy: { firstName: string; lastName: string } | null
  minThreshold: number
  feePercent: number
  totalFee: number
  incomeEffect: IncomeEffect | 'NOT_AVAILABLE'
  totalIncome: number | null
  requiresManualSending: boolean
  isRetroactive: boolean
}

export interface FeeDecisionSummary {
  id: UUID
  status: FeeDecisionStatus
  decisionNumber: number | null
  validFrom: LocalDate
  validTo: LocalDate | null
  headOfFamily: PersonBasic
  parts: Array<{ child: PersonBasic }>
  approvedAt: Date | null
  createdAt: Date
  sentAt: Date | null
  finalPrice: number
}

export interface VoucherValueDecisionDetailed {
  id: UUID
  status: VoucherValueDecisionStatus
  validFrom: LocalDate
  validTo: LocalDate | null
  decisionNumber: number | null
  headOfFamily: PersonDetailed
  partner: PersonDetailed | null
  headOfFamilyIncome: Income | null
  partnerIncome: Income | null
  familySize: number
  child: PersonDetailed
  placement: VoucherValueDecisionPlacement
  serviceNeed: VoucherValueDecisionServiceNeed
  baseCoPayment: number
  siblingDiscount: number
  coPayment: number
  feeAlterations: FeeDecisionAlteration[]
  finalCoPayment: number
  baseValue: number
  childAge: number
  ageCoefficient: number
  voucherValue: number
  documentKey: string | null
  approvedAt: Date | null
  sentAt: Date | null
  created: Date
  financeDecisionHandlerName: string | null
  minThreshold: number
  feePercent: number
  incomeEffect: IncomeEffect | 'NOT_AVAILABLE'
  totalIncome: number | null
  requiresManualSending: boolean
  isRetroactive: boolean
}

export interface VoucherValueDecisionPlacement {
  unit: UnitDetailed
  type: PlacementType
}

export interface VoucherValueDecisionServiceNeed {
  feeCoefficient: number
  voucherValueCoefficient: number
  feeDescriptionFi: string
  feeDescriptionSv: string
  voucherValueDescriptionFi: string
  voucherValueDescriptionSv: string
}

export interface VoucherValueDecisionSummary {
  id: UUID
  status: VoucherValueDecisionStatus
  validFrom: LocalDate
  validTo: LocalDate | null
  decisionNumber: number | null
  headOfFamily: PersonBasic
  child: PersonBasic
  finalCoPayment: number
  voucherValue: number
  approvedAt: Date | null
  sentAt: Date | null
  created: Date
}
