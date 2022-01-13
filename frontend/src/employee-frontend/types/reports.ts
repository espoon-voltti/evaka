// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  AssistanceActionOption,
  AssistanceMeasure
} from 'lib-common/generated/api-types/assistanceaction'
import { AssistanceBasisOption } from 'lib-common/generated/api-types/assistanceneed'
import { ProviderType } from 'lib-common/generated/api-types/daycare'
import { UnitType } from 'lib-common/generated/api-types/reports'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { UnitProviderType } from 'lib-customizations/types'

export interface InvoiceReportRow {
  areaCode: number
  amountOfInvoices: number
  totalSumCents: number
  amountWithoutSSN: number
  amountWithoutAddress: number
  amountWithZeroPrice: number
}

export interface InvoiceReport {
  totalAmountOfInvoices: number
  totalSumCents: number
  totalAmountWithoutSSN: number
  totalAmountWithoutAddress: number
  totalAmountWithZeroPrice: number
  reportRows: InvoiceReportRow[]
}

export interface FamilyConflictReportRow {
  careAreaName: string
  unitId: UUID
  unitName: string
  id: UUID
  firstName: string | null
  lastName: string | null
  socialSecurityNumber: string | null
  partnerConflictCount: number
  childConflictCount: number
}

interface FamilyContact {
  firstName: string
  lastName: string
  phone: string
  email: string
}

export interface FamilyContactsReportRow {
  id: UUID
  firstName: string
  lastName: string
  ssn: string | null
  group: string | null
  streetAddress: string
  postalCode: string
  postOffice: string
  headOfChild: FamilyContact | null
  guardian1: FamilyContact | null
  guardian2: FamilyContact | null
}

export interface ApplicationsReportRow {
  careAreaName: string
  unitId: UUID
  unitName: string
  unitProviderType: UnitProviderType
  under3Years: number
  over3Years: number
  preschool: number
  club: number
  total: number
}

export interface DecisionsReportRow {
  careAreaName: string
  unitId: string
  unitName: string
  unitProviderType: UnitProviderType
  daycareUnder3: number
  daycareOver3: number
  preschool: number
  preschoolDaycare: number
  preparatory: number
  preparatoryDaycare: number
  club: number
  preference1: number
  preference2: number
  preference3: number
  preferenceNone: number
  total: number
}

export interface PresenceReportRow {
  date: LocalDate
  socialSecurityNumber: string | null
  daycareId: UUID | null
  daycareGroupName: string | null
  present: boolean | null
}

export interface ServiceNeedReportRow {
  careAreaName: string
  unitName: string
  unitType: 'DAYCARE' | 'FAMILY' | 'GROUP_FAMILY' | 'CLUB' | null
  unitProviderType: UnitProviderType
  age: number
  fullDay: number
  partDay: number
  fullWeek: number
  partWeek: number
  shiftCare: number
  missingServiceNeed: number
  total: number
}

export interface MissingHeadOfFamilyReportRow {
  careAreaName: string
  unitId: UUID
  unitName: string
  childId: UUID
  firstName: string | null
  lastName: string | null
  daysWithoutHead: number
}

export interface MissingServiceNeedReportRow {
  careAreaName: string
  unitId: UUID
  unitName: string
  childId: UUID
  firstName: string | null
  lastName: string | null
  daysWithoutServiceNeed: number
}

export interface InvalidServiceNeedReportRow {
  currentUnitId: UUID | null
  currentUnitName: string | null
  childId: UUID
  firstName: string | null
  lastName: string | null
  startDate: LocalDate
  endDate: LocalDate
}

export interface PartnersInDifferentAddressReportRow {
  careAreaName: string
  unitId: UUID
  unitName: string
  personId1: UUID
  firstName1: string | null
  lastName1: string | null
  address1: string | null
  personId2: UUID
  firstName2: string | null
  lastName2: string | null
  address2: string | null
}

export interface ChildrenInDifferentAddressReportRow {
  careAreaName: string
  unitId: UUID
  unitName: string
  parentId: UUID
  firstNameParent: string | null
  lastNameParent: string | null
  addressParent: string | null
  childId: UUID
  firstNameChild: string | null
  lastNameChild: string | null
  addressChild: string | null
}

export interface UnitBasicsAbstractReportRow {
  careAreaName: string
  unitId: UUID
  unitName: string
  unitType: 'DAYCARE' | 'FAMILY' | 'GROUP_FAMILY' | 'CLUB' | null
  unitProviderType: UnitProviderType
}

export interface GroupBasicsAbstractReportRow
  extends UnitBasicsAbstractReportRow {
  groupId: UUID
  groupName: string
}

export interface ChildAgeLanguageReportRow extends UnitBasicsAbstractReportRow {
  fi_0y: number
  fi_1y: number
  fi_2y: number
  fi_3y: number
  fi_4y: number
  fi_5y: number
  fi_6y: number
  fi_7y: number

  sv_0y: number
  sv_1y: number
  sv_2y: number
  sv_3y: number
  sv_4y: number
  sv_5y: number
  sv_6y: number
  sv_7y: number

  other_0y: number
  other_1y: number
  other_2y: number
  other_3y: number
  other_4y: number
  other_5y: number
  other_6y: number
  other_7y: number
}

export interface AssistanceNeedsAndActionsReport {
  actions: AssistanceActionOption[]
  bases: AssistanceBasisOption[]
  rows: AssistanceNeedsAndActionsReportRow[]
}

export interface AssistanceNeedsAndActionsReportRow {
  actionCounts: Record<string, number>
  basisCounts: Record<string, number>
  measureCounts: Record<AssistanceMeasure, number>
  careAreaName: string
  groupId: UUID
  groupName: string
  noActionCount: number
  noBasisCount: number
  otherActionCount: number
  unitId: UUID
  unitName: string
  unitProviderType: ProviderType
  unitType: UnitType
}

export interface OccupancyReportRow {
  unitId: UUID
  unitName: string
  groupName?: string
  occupancies: {
    [date: string]: OccupancyReportDate
  }
}

interface OccupancyReportDate {
  sum: number
  headcount: number
  caretakers: number | null
  percentage: number | null
}

export interface EndedPlacementsReportRow {
  childId: UUID
  firstName: string | null
  lastName: string | null
  ssn: string | null
  placementEnd: LocalDate
  nextPlacementStart: LocalDate | null
}

export interface ReferenceCount {
  table: string
  column: string
  count: number
}

export interface DuplicatePeopleReportRow {
  groupIndex: number
  duplicateNumber: number
  id: UUID
  firstName: string | null
  lastName: string | null
  socialSecurityNumber: string | null
  dateOfBirth: LocalDate
  streetAddress: string | null
  referenceCounts: ReferenceCount[]
}

export interface StartingPlacementsRow {
  childId: UUID
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
  ssn?: string
  placementStart: LocalDate
  careAreaName: string
}

export interface PlacementSketchingRow {
  applicationId: UUID
  areaName: string
  preferredStartDate: LocalDate
  sentDate: LocalDate
  requestedUnitId: UUID
  requestedUnitName: string
  currentUnitName: string | null
  currentUnitId: UUID | null
  childId: UUID
  childFirstName: string
  childLastName: string
  childDob: LocalDate
  childStreetAddr: string
  assistanceNeeded: boolean
  preparatoryEducation: boolean
  siblingBasis: boolean
  connectedDaycare: boolean
  guardianPhoneNumber: string | null
  guardianEmail: string | null
  otherPreferredUnits: string[] | null
}
