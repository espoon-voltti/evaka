// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '~types/index'
import LocalDate from '@evaka/lib-common/src/local-date'
import { AbsenceType } from 'types/absence'

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
  unitProviderType: string
  under3Years: number
  over3Years: number
  preschool: number
  club: number
  total: number
}

export interface RawReportRow {
  day: LocalDate
  childId: UUID
  dateOfBirth: LocalDate
  age: number
  language: string | null
  placementType:
    | 'CLUB'
    | 'DAYCARE'
    | 'DAYCARE_PART_TIME'
    | 'PRESCHOOL'
    | 'PRESCHOOL_DAYCARE'
  unitId: UUID
  unitName: string
  careArea: string
  unitType: 'DAYCARE' | 'FAMILY' | 'GROUP_FAMILY' | 'CLUB' | null
  unitProviderType: 'MUNICIPAL' | 'PURCHASED' | 'PRIVATE' | 'MUNICIPAL_SCHOOL'
  daycareGroupId: UUID | null
  groupName: string | null
  caretakersPlanned: number | null
  caretakersRealized: number | null
  backupUnitId: UUID | null
  backupGroupId: UUID | null
  hasServiceNeed: boolean
  partDay: boolean | null
  partWeek: boolean | null
  shiftCare: boolean | null
  preparatory: boolean | null
  hoursPerWeek: number | null
  hasAssistanceNeed: boolean
  capacityFactor: number
  capacity: number
  absencePaid: AbsenceType | null
  absenceFree: AbsenceType | null
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
  unitProviderType: 'MUNICIPAL' | 'PURCHASED' | 'PRIVATE' | 'MUNICIPAL_SCHOOL'
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
  unitProviderType: 'MUNICIPAL' | 'PURCHASED' | 'PRIVATE' | 'MUNICIPAL_SCHOOL'
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

export interface AssistanceNeedsReportRow extends GroupBasicsAbstractReportRow {
  autism: number
  developmentalDisability1: number
  developmentalDisability2: number
  focusChallenge: number
  linguisticChallenge: number
  developmentMonitoring: number
  developmentMonitoringPending: number
  multiDisability: number
  longTermCondition: number
  regulationSkillChallenge: number
  disability: number
  other: number
  none: number
}

export interface AssistanceActionsReportRow
  extends GroupBasicsAbstractReportRow {
  assistanceServiceChild: number
  assistanceServiceUnit: number
  smallerGroup: number
  specialGroup: number
  pervasiveVeoSupport: number
  resourcePerson: number
  ratioDecrease: number
  periodicalVeoSupport: number
  other: number
  none: number
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

export interface DuplicatePeopleReportRow {
  groupIndex: number
  duplicateNumber: number
  id: UUID
  firstName: string | null
  lastName: string | null
  socialSecurityNumber: string | null
  dateOfBirth: LocalDate
  streetAddress: string | null

  applicationsGuardian: number
  feeDecisionsHead: number
  feeDecisionsPartner: number
  fridgeChildren: number
  fridgePartners: number
  incomes: number
  invoices: number

  absences: number
  applicationsChild: number
  assistanceNeeds: number
  assistanceActions: number
  backups: number
  feeAlterations: number
  feeDecisionParts: number
  fridgeParents: number
  invoiceRows: number
  placements: number
  serviceNeeds: number
}

export interface StartingPlacementsRow {
  childId: UUID
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
  ssn?: string
  placementStart: LocalDate
}

export interface VoucherServiceProviderRow {
  careAreaName: string
  unitId: UUID
  unitName: string
  voucherChildCount: number
  voucherSum: number
  startDate: LocalDate
  endDate: LocalDate
  unitVoucherReportUri: string
}
