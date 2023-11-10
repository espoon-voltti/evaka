// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import LocalTime from '../../local-time'
import { AbsenceType } from './daycare'
import { ApplicationStatus } from './application'
import { AssistanceActionOption } from './assistanceaction'
import { AssistanceNeedDecisionStatus } from './assistanceneed'
import { DaycareAssistanceLevel } from './assistance'
import { DecisionType } from './decision'
import { OccupancyValues } from './occupancy'
import { OtherAssistanceMeasureType } from './assistance'
import { PlacementType } from './placement'
import { PreschoolAssistanceLevel } from './assistance'
import { ProviderType } from './daycare'
import { ServiceNeedOption } from './application'
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
  unitProviderType: ProviderType
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedDecisionsReportRow
*/
export interface AssistanceNeedDecisionsReportRow {
  careAreaName: string
  childName: string
  decisionMade: LocalDate | null
  id: UUID
  isOpened: boolean | null
  preschool: boolean
  sentForDecision: LocalDate
  status: AssistanceNeedDecisionStatus
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReport
*/
export interface AssistanceNeedsAndActionsReport {
  actions: AssistanceActionOption[]
  rows: AssistanceNeedsAndActionsReportRow[]
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportByChild
*/
export interface AssistanceNeedsAndActionsReportByChild {
  actions: AssistanceActionOption[]
  rows: AssistanceNeedsAndActionsReportRowByChild[]
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow
*/
export interface AssistanceNeedsAndActionsReportRow {
  actionCounts: Record<string, number>
  careAreaName: string
  daycareAssistanceCounts: Record<DaycareAssistanceLevel, number>
  groupId: UUID
  groupName: string
  noActionCount: number
  otherActionCount: number
  otherAssistanceMeasureCounts: Record<OtherAssistanceMeasureType, number>
  preschoolAssistanceCounts: Record<PreschoolAssistanceLevel, number>
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRowByChild
*/
export interface AssistanceNeedsAndActionsReportRowByChild {
  actions: string[]
  careAreaName: string
  childFirstName: string
  childId: UUID
  childLastName: string
  daycareAssistanceCounts: Record<DaycareAssistanceLevel, number>
  groupId: UUID
  groupName: string
  otherAction: string
  otherAssistanceMeasureCounts: Record<OtherAssistanceMeasureType, number>
  preschoolAssistanceCounts: Record<PreschoolAssistanceLevel, number>
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.AttendanceReservationReportByChildRow
*/
export interface AttendanceReservationReportByChildRow {
  absenceId: UUID | null
  absenceType: AbsenceType | null
  childFirstName: string
  childId: UUID
  childLastName: string
  date: LocalDate
  groupId: UUID | null
  groupName: string | null
  isBackupCare: boolean
  reservationEndTime: LocalTime | null
  reservationId: UUID | null
  reservationStartTime: LocalTime | null
}

/**
* Generated from fi.espoo.evaka.reports.AttendanceReservationReportRow
*/
export interface AttendanceReservationReportRow {
  capacityFactor: number
  childCount: number
  childCountOver3: number
  childCountUnder3: number
  dateTime: HelsinkiDateTime
  groupId: UUID | null
  groupName: string | null
  staffCountRequired: number
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
  unitProviderType: ProviderType
  unitType: UnitType
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
  email: string | null
  firstName: string
  id: UUID
  lastName: string
  phone: string
}

/**
* Generated from fi.espoo.evaka.reports.DecisionsReportRow
*/
export interface DecisionsReportRow {
  careAreaName: string
  club: number
  connectedDaycareOnly: number
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
  providerType: ProviderType
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
  groupName: string | null
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
* Generated from fi.espoo.evaka.reports.FamilyDaycareMealReport.FamilyDaycareMealAreaResult
*/
export interface FamilyDaycareMealAreaResult {
  areaId: UUID
  areaName: string
  breakfastCount: number
  daycareResults: FamilyDaycareMealDaycareResult[]
  lunchCount: number
  snackCount: number
}

/**
* Generated from fi.espoo.evaka.reports.FamilyDaycareMealReport.FamilyDaycareMealChildResult
*/
export interface FamilyDaycareMealChildResult {
  breakfastCount: number
  childId: UUID
  firstName: string
  lastName: string
  lunchCount: number
  snackCount: number
}

/**
* Generated from fi.espoo.evaka.reports.FamilyDaycareMealReport.FamilyDaycareMealDaycareResult
*/
export interface FamilyDaycareMealDaycareResult {
  breakfastCount: number
  childResults: FamilyDaycareMealChildResult[]
  daycareId: UUID
  daycareName: string
  lunchCount: number
  snackCount: number
}

/**
* Generated from fi.espoo.evaka.reports.FamilyDaycareMealReport.FamilyDaycareMealReportResult
*/
export interface FamilyDaycareMealReportResult {
  areaResults: FamilyDaycareMealAreaResult[]
  breakfastCount: number
  lunchCount: number
  snackCount: number
}

/**
* Generated from fi.espoo.evaka.reports.FuturePreschoolersReportRow
*/
export interface FuturePreschoolersReportRow {
  childAddress: string
  childFirstName: string
  childLastName: string
  childPostOffice: string
  childPostalCode: string
  childSsn: string | null
  guardian1Address: string | null
  guardian1Email: string | null
  guardian1FirstName: string | null
  guardian1LastName: string | null
  guardian1Phone: string | null
  guardian1PostOffice: string | null
  guardian1PostalCode: string | null
  guardian2Address: string | null
  guardian2Email: string | null
  guardian2FirstName: string | null
  guardian2LastName: string | null
  guardian2Phone: string | null
  guardian2PostOffice: string | null
  guardian2PostalCode: string | null
  id: UUID
  languageEmphasisGroup: string | null
  shiftCare: boolean
  twoYearPreschool: boolean
  unitAddress: string
  unitArea: string | null
  unitName: string
  unitPostOffice: string
  unitPostalCode: string
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
* Generated from fi.espoo.evaka.reports.ManualDuplicationReportController.ManualDuplicationReportRow
*/
export interface ManualDuplicationReportRow {
  applicationId: UUID
  childFirstName: string
  childId: UUID
  childLastName: string
  connectedDaycareId: UUID
  connectedDaycareName: string
  connectedDecisionType: DecisionType
  connectedEndDate: LocalDate
  connectedSnoName: string | null
  connectedStartDate: LocalDate
  dateOfBirth: LocalDate
  preschoolDaycareId: UUID
  preschoolDaycareName: string
  preschoolDecisionType: DecisionType
  preschoolEndDate: LocalDate
  preschoolStartDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.reports.ManualDuplicationReportViewMode
*/
export type ManualDuplicationReportViewMode =
  | 'DUPLICATED'
  | 'NONDUPLICATED'

/**
* Generated from fi.espoo.evaka.reports.MissingHeadOfFamilyReportRow
*/
export interface MissingHeadOfFamilyReportRow {
  childId: UUID
  firstName: string
  lastName: string
  rangesWithoutHead: FiniteDateRange[]
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
* Generated from fi.espoo.evaka.reports.OccupancyGroupReportResultRow
*/
export interface OccupancyGroupReportResultRow {
  areaId: UUID
  areaName: string
  groupId: UUID
  groupName: string
  occupancies: Record<string, OccupancyValues>
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.OccupancyUnitReportResultRow
*/
export interface OccupancyUnitReportResultRow {
  areaId: UUID
  areaName: string
  occupancies: Record<string, OccupancyValues>
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
* Generated from fi.espoo.evaka.reports.PlacementCountReportController.PlacementCountAreaResult
*/
export interface PlacementCountAreaResult {
  areaId: UUID
  areaName: string
  calculatedPlacements: number
  daycareResults: PlacementCountDaycareResult[]
  placementCount: number
  placementCount3vAndOver: number
  placementCountUnder3v: number
}

/**
* Generated from fi.espoo.evaka.reports.PlacementCountReportController.PlacementCountDaycareResult
*/
export interface PlacementCountDaycareResult {
  calculatedPlacements: number
  daycareId: UUID
  daycareName: string
  placementCount: number
  placementCount3vAndOver: number
  placementCountUnder3v: number
}

/**
* Generated from fi.espoo.evaka.reports.PlacementCountReportController.PlacementCountReportResult
*/
export interface PlacementCountReportResult {
  areaResults: PlacementCountAreaResult[]
  calculatedPlacements: number
  placementCount: number
  placementCount3vAndOver: number
  placementCountUnder3v: number
}

/**
* Generated from fi.espoo.evaka.reports.PlacementGuaranteeReportRow
*/
export interface PlacementGuaranteeReportRow {
  areaId: UUID
  areaName: string
  childFirstName: string
  childId: UUID
  childLastName: string
  placementEndDate: LocalDate
  placementStartDate: LocalDate
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.PlacementSketchingReportRow
*/
export interface PlacementSketchingReportRow {
  applicationId: UUID
  applicationStatus: ApplicationStatus
  areaName: string
  assistanceNeeded: boolean | null
  childCorrectedCity: string
  childCorrectedPostalCode: string
  childCorrectedStreetAddress: string
  childDob: LocalDate
  childFirstName: string
  childId: UUID
  childLastName: string
  childMovingDate: LocalDate | null
  childPostalCode: string | null
  childStreetAddr: string | null
  connectedDaycare: boolean | null
  currentUnitId: UUID | null
  currentUnitName: string | null
  guardianEmail: string | null
  guardianPhoneNumber: string | null
  hasAdditionalInfo: boolean
  otherPreferredUnits: string[]
  preferredStartDate: LocalDate
  preparatoryEducation: boolean | null
  requestedUnitId: UUID
  requestedUnitName: string
  sentDate: LocalDate
  serviceNeedOption: ServiceNeedOption | null
  siblingBasis: boolean | null
}

/**
* Generated from fi.espoo.evaka.reports.PreschoolGroupsReportRow
*/
export interface PreschoolGroupsReportRow {
  address: string
  amongSchool: boolean | null
  groupName: string
  groupSize: number
  id: UUID
  languageEmphasis: boolean | null
  postOffice: string
  postalCode: string
  shiftCare: boolean
  unitName: string
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
  isHoliday: boolean
  isWeekday: boolean
  language: string | null
  lastName: string
  partDay: boolean
  partWeek: boolean
  placementType: PlacementType
  postOffice: string
  postalCode: string
  realizedCapacity: number
  shiftCare: boolean
  staffDimensioning: number
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
* Generated from fi.espoo.evaka.reports.Report
*/
export type Report =
  | 'APPLICATIONS'
  | 'ASSISTANCE_NEED_DECISIONS'
  | 'ASSISTANCE_NEEDS_AND_ACTIONS'
  | 'ASSISTANCE_NEEDS_AND_ACTIONS_BY_CHILD'
  | 'ATTENDANCE_RESERVATION'
  | 'CHILD_AGE_LANGUAGE'
  | 'CHILDREN_IN_DIFFERENT_ADDRESS'
  | 'DECISIONS'
  | 'DUPLICATE_PEOPLE'
  | 'ENDED_PLACEMENTS'
  | 'FAMILY_CONFLICT'
  | 'FAMILY_DAYCARE_MEAL_REPORT'
  | 'INVOICE'
  | 'MANUAL_DUPLICATION'
  | 'MISSING_HEAD_OF_FAMILY'
  | 'MISSING_SERVICE_NEED'
  | 'OCCUPANCY'
  | 'PARTNERS_IN_DIFFERENT_ADDRESS'
  | 'PLACEMENT_COUNT'
  | 'PLACEMENT_GUARANTEE'
  | 'PLACEMENT_SKETCHING'
  | 'PRESENCE'
  | 'RAW'
  | 'SERVICE_NEED'
  | 'SERVICE_VOUCHER_VALUE'
  | 'SEXTET'
  | 'STARTING_PLACEMENTS'
  | 'UNITS'
  | 'VARDA_ERRORS'
  | 'FUTURE_PRESCHOOLERS'

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
  unitProviderType: ProviderType
  unitType: UnitType
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherReport
*/
export interface ServiceVoucherReport {
  locked: LocalDate | null
  rows: ServiceVoucherValueUnitAggregate[]
}

/**
* Generated from fi.espoo.evaka.reports.ServiceVoucherValueReportController.ServiceVoucherUnitReport
*/
export interface ServiceVoucherUnitReport {
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
  assistanceNeedCoefficient: number
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
  serviceVoucherFinalCoPayment: number
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
* Generated from fi.espoo.evaka.reports.UnitsReportRow
*/
export interface UnitsReportRow {
  address: string
  careAreaName: string
  careTypeCentre: boolean
  careTypeClub: boolean
  careTypeFamily: boolean
  careTypeGroupFamily: boolean
  careTypePreparatoryEducation: boolean
  careTypePreschool: boolean
  clubApply: boolean
  costCenter: string
  daycareApply: boolean
  id: UUID
  invoicedByMunicipality: boolean
  name: string
  preschoolApply: boolean
  providerType: ProviderType
  unitManagerName: string
  unitManagerPhone: string
  uploadChildrenToVarda: boolean
  uploadToKoski: boolean
  uploadToVarda: boolean
}

/**
* Generated from fi.espoo.evaka.reports.VardaErrorReportRow
*/
export interface VardaErrorReportRow {
  childId: UUID
  created: HelsinkiDateTime
  errors: string[]
  resetTimeStamp: HelsinkiDateTime | null
  serviceNeedEndDate: string
  serviceNeedId: UUID
  serviceNeedOptionName: string
  serviceNeedStartDate: string
  updated: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.reports.VoucherReportRowType
*/
export type VoucherReportRowType =
  | 'ORIGINAL'
  | 'REFUND'
  | 'CORRECTION'
