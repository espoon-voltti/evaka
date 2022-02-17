// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier */

import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import { AbsenceType } from './daycare'
import { AssistanceActionOption } from './assistanceaction'
import { AssistanceBasisOption } from './assistanceneed'
import { AssistanceMeasure } from './assistanceaction'
import { PlacementType } from './placement'
import { ProviderType } from './daycare'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.reports.ApplicationsReportRow
*/
export interface ApplicationsReportRow {
  careAreaName: string
  club: number
  over3Years: number
  preschool: number
  total: number
  under3Years: number
  unitId: UUID
  unitName: string
  unitProviderType: string
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReport
*/
export interface AssistanceNeedsAndActionsReport {
  actions: AssistanceActionOption[]
  bases: AssistanceBasisOption[]
  rows: AssistanceNeedsAndActionsReportRow[]
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow
*/
export interface AssistanceNeedsAndActionsReportRow {
  actionCounts: Record<string, number>
  basisCounts: Record<string, number>
  careAreaName: string
  groupId: UUID
  groupName: string
  measureCounts: Record<AssistanceMeasure, number>
  noActionCount: number
  noBasisCount: number
  otherActionCount: number
  unitId: UUID
  unitName: string
  unitProviderType: ProviderType
  unitType: UnitType
}

/**
* Generated from fi.espoo.evaka.reports.ChildAgeLanguageReportRow
*/
export interface ChildAgeLanguageReportRow {
  careAreaName: string
  fi_0y: number
  fi_1y: number
  fi_2y: number
  fi_3y: number
  fi_4y: number
  fi_5y: number
  fi_6y: number
  fi_7y: number
  other_0y: number
  other_1y: number
  other_2y: number
  other_3y: number
  other_4y: number
  other_5y: number
  other_6y: number
  other_7y: number
  sv_0y: number
  sv_1y: number
  sv_2y: number
  sv_3y: number
  sv_4y: number
  sv_5y: number
  sv_6y: number
  sv_7y: number
  unitId: UUID
  unitName: string
  unitProviderType: string
  unitType: UnitType | null
}

/**
* Generated from fi.espoo.evaka.reports.ChildrenInDifferentAddressReportRow
*/
export interface ChildrenInDifferentAddressReportRow {
  addressChild: string
  addressParent: string
  careAreaName: string
  childId: UUID
  firstNameChild: string | null
  firstNameParent: string | null
  lastNameChild: string | null
  lastNameParent: string | null
  parentId: UUID
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.Contact
*/
export interface Contact {
  email: string
  firstName: string
  lastName: string
  phone: string
}

/**
* Generated from fi.espoo.evaka.reports.DecisionsReportRow
*/
export interface DecisionsReportRow {
  careAreaName: string
  club: number
  daycareOver3: number
  daycareUnder3: number
  preference1: number
  preference2: number
  preference3: number
  preferenceNone: number
  preparatory: number
  preparatoryDaycare: number
  preschool: number
  preschoolDaycare: number
  providerType: string
  total: number
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.DuplicatePeopleReportRow
*/
export interface DuplicatePeopleReportRow {
  dateOfBirth: LocalDate
  duplicateNumber: number
  firstName: string | null
  groupIndex: number
  id: UUID
  lastName: string | null
  referenceCounts: ReferenceCount[]
  socialSecurityNumber: string | null
  streetAddress: string | null
}

/**
* Generated from fi.espoo.evaka.reports.EndedPlacementsReportRow
*/
export interface EndedPlacementsReportRow {
  childId: UUID
  firstName: string | null
  lastName: string | null
  nextPlacementStart: LocalDate | null
  placementEnd: LocalDate
  ssn: string | null
}

/**
* Generated from fi.espoo.evaka.reports.FamilyConflictReportRow
*/
export interface FamilyConflictReportRow {
  careAreaName: string
  childConflictCount: number
  firstName: string | null
  id: UUID
  lastName: string | null
  partnerConflictCount: number
  socialSecurityNumber: string | null
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.FamilyContactReportRow
*/
export interface FamilyContactReportRow {
  firstName: string
  group: string | null
  guardian1: Contact | null
  guardian2: Contact | null
  headOfChild: Contact | null
  id: UUID
  lastName: string
  postOffice: string
  postalCode: string
  ssn: string | null
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.reports.InvoiceReport
*/
export interface InvoiceReport {
  reportRows: InvoiceReportRow[]
  totalAmountOfInvoices: number
  totalAmountWithZeroPrice: number
  totalAmountWithoutAddress: number
  totalAmountWithoutSSN: number
  totalSumCents: number
}

/**
* Generated from fi.espoo.evaka.reports.InvoiceReportRow
*/
export interface InvoiceReportRow {
  amountOfInvoices: number
  amountWithZeroPrice: number
  amountWithoutAddress: number
  amountWithoutSSN: number
  areaCode: number | null
  totalSumCents: number
}

/**
* Generated from fi.espoo.evaka.reports.MissingHeadOfFamilyReportRow
*/
export interface MissingHeadOfFamilyReportRow {
  careAreaName: string
  childId: UUID
  daysWithoutHead: number
  firstName: string | null
  lastName: string | null
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.MissingServiceNeedReportRow
*/
export interface MissingServiceNeedReportRow {
  careAreaName: string
  childId: UUID
  daysWithoutServiceNeed: number
  firstName: string | null
  lastName: string | null
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.PartnersInDifferentAddressReportRow
*/
export interface PartnersInDifferentAddressReportRow {
  address1: string
  address2: string
  careAreaName: string
  firstName1: string | null
  firstName2: string | null
  lastName1: string | null
  lastName2: string | null
  personId1: UUID
  personId2: UUID
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.PlacementSketchingReportRow
*/
export interface PlacementSketchingReportRow {
  applicationId: UUID | null
  areaName: string
  assistanceNeeded: boolean | null
  childDob: string | null
  childFirstName: string | null
  childId: UUID
  childLastName: string | null
  childStreetAddr: string | null
  connectedDaycare: boolean | null
  currentUnitId: UUID | null
  currentUnitName: string | null
  guardianEmail: string | null
  guardianPhoneNumber: string | null
  otherPreferredUnits: string[]
  preferredStartDate: LocalDate
  preparatoryEducation: boolean | null
  requestedUnitId: UUID
  requestedUnitName: string
  sentDate: LocalDate
  siblingBasis: boolean | null
}

/**
* Generated from fi.espoo.evaka.reports.PresenceReportRow
*/
export interface PresenceReportRow {
  date: LocalDate
  daycareGroupName: string | null
  daycareId: UUID | null
  present: boolean | null
  socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.reports.RawReportRow
*/
export interface RawReportRow {
  absenceFree: AbsenceType | null
  absencePaid: AbsenceType | null
  age: number
  backupGroupId: UUID | null
  backupUnitId: UUID | null
  capacity: number
  capacityFactor: number
  careArea: string
  caretakersPlanned: number | null
  caretakersRealized: number | null
  childId: UUID
  costCenter: string | null
  dateOfBirth: LocalDate
  day: LocalDate
  daycareGroupId: UUID | null
  firstName: string
  groupName: string | null
  hasAssistanceNeed: boolean
  hasServiceNeed: boolean
  hoursPerWeek: number
  language: string | null
  lastName: string
  partDay: boolean
  partWeek: boolean
  placementType: PlacementType
  postOffice: string
  shiftCare: boolean
  unitId: UUID
  unitName: string
  unitProviderType: ProviderType
  unitType: UnitType | null
}

/**
* Generated from fi.espoo.evaka.reports.ReferenceCount
*/
export interface ReferenceCount {
  column: string
  count: number
  table: string
}

/**
* Generated from fi.espoo.evaka.reports.ServiceNeedReportRow
*/
export interface ServiceNeedReportRow {
  age: number
  careAreaName: string
  fullDay: number
  fullWeek: number
  missingServiceNeed: number
  partDay: number
  partWeek: number
  shiftCare: number
  total: number
  unitName: string
  unitProviderType: string
  unitType: UnitType | null
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueReportController.ServiceVoucherReport
*/
export interface ServiceVoucherReport {
  locked: LocalDate | null
  rows: ServiceVoucherValueUnitAggregate[]
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueReportController.ServiceVoucherUnitReport
*/
export interface ServiceVoucherUnitReport {
  assistanceNeedCapacityFactorEnabled: boolean
  locked: LocalDate | null
  rows: ServiceVoucherValueRow[]
  voucherTotal: number
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueRow
*/
export interface ServiceVoucherValueRow {
  areaId: UUID
  areaName: string
  assistanceNeedCapacityFactor: number
  childDateOfBirth: LocalDate
  childFirstName: string
  childGroupName: string | null
  childId: UUID
  childLastName: string
  isNew: boolean
  numberOfDays: number
  realizedAmount: number
  realizedPeriod: FiniteDateRange
  serviceNeedDescription: string
  serviceVoucherCoPayment: number
  serviceVoucherDecisionId: UUID
  serviceVoucherValue: number
  type: VoucherReportRowType
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueUnitAggregate
*/
export interface ServiceVoucherValueUnitAggregate {
  childCount: number
  monthlyPaymentSum: number
  unit: UnitData
}

/**
* Generated from fi.espoo.evaka.reports.SextetReportRow
*/
export interface SextetReportRow {
  attendanceDays: number
  placementType: PlacementType
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.StartingPlacementsRow
*/
export interface StartingPlacementsRow {
  careAreaName: string
  childId: UUID
  dateOfBirth: LocalDate
  firstName: string
  lastName: string
  placementStart: LocalDate
  ssn: string | null
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueUnitAggregate.UnitData
*/
export interface UnitData {
  areaId: UUID
  areaName: string
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.reports.UnitType
*/
export type UnitType = 
  | 'DAYCARE'
  | 'FAMILY'
  | 'GROUP_FAMILY'
  | 'CLUB'

/**
* Generated from fi.espoo.evaka.reports.VardaErrorReportRow
*/
export interface VardaErrorReportRow {
  childId: UUID
  created: Date
  errors: string[]
  resetTimeStamp: Date | null
  serviceNeedEndDate: string
  serviceNeedId: UUID
  serviceNeedOptionName: string
  serviceNeedStartDate: string
  updated: Date
}

/**
* Generated from fi.espoo.evaka.reports.VoucherReportRowType
*/
export type VoucherReportRowType = 
  | 'ORIGINAL'
  | 'REFUND'
  | 'CORRECTION'
