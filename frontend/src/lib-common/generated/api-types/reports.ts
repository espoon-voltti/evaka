// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import LocalTime from '../../local-time'
import TimeInterval from '../../time-interval'
import TimeRange from '../../time-range'
import { AbsenceType } from './absence'
import { ApplicationStatus } from './application'
import { AssistanceActionOption } from './assistanceaction'
import { AssistanceNeedDecisionStatus } from './assistanceneed'
import { DaycareAssistanceLevel } from './assistance'
import { DecisionType } from './decision'
import { JsonOf } from '../../json'
import { MealType } from './mealintegration'
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
  decisionNumber: number
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
  showAssistanceNeedVoucherCoefficient: boolean
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportByChild
*/
export interface AssistanceNeedsAndActionsReportByChild {
  actions: AssistanceActionOption[]
  rows: AssistanceNeedsAndActionsReportRowByChild[]
  showAssistanceNeedVoucherCoefficient: boolean
}

/**
* Generated from fi.espoo.evaka.reports.AssistanceNeedsAndActionsReportController.AssistanceNeedsAndActionsReportRow
*/
export interface AssistanceNeedsAndActionsReportRow {
  actionCounts: Record<string, number>
  assistanceNeedVoucherCoefficientCount: number
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
  assistanceNeedVoucherCoefficient: number
  careAreaName: string
  childAge: number
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
* Generated from fi.espoo.evaka.reports.AttendanceReservationReportController.AttendanceReservationReportByChildBody
*/
export interface AttendanceReservationReportByChildBody {
  groupIds: UUID[]
  range: FiniteDateRange
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.reports.AttendanceReservationReportByChildGroup
*/
export interface AttendanceReservationReportByChildGroup {
  groupId: UUID | null
  groupName: string | null
  items: AttendanceReservationReportByChildItem[]
}

/**
* Generated from fi.espoo.evaka.reports.AttendanceReservationReportByChildItem
*/
export interface AttendanceReservationReportByChildItem {
  backupCare: boolean
  childFirstName: string
  childId: UUID
  childLastName: string
  date: LocalDate
  fullDayAbsence: boolean
  reservation: TimeRange | null
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
* Generated from fi.espoo.evaka.reports.ChildAttendanceReportRow
*/
export interface ChildAttendanceReportRow {
  attendances: TimeInterval[]
  billableAbsence: AbsenceType | null
  date: LocalDate
  nonbillableAbsence: AbsenceType | null
  reservations: TimeRange[]
}

/**
* Generated from fi.espoo.evaka.reports.ChildPreschoolAbsenceRow
*/
export interface ChildPreschoolAbsenceRow {
  childId: UUID
  firstName: string
  hourlyTypeResults: Record<AbsenceType, number>
  lastName: string
}

/**
* Generated from fi.espoo.evaka.reports.ChildWithName
*/
export interface ChildWithName {
  firstName: string
  id: UUID
  lastName: string
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
* Generated from fi.espoo.evaka.reports.CustomerFeesReport.CustomerFeesReportRow
*/
export interface CustomerFeesReportRow {
  count: number
  feeAmount: number
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
  areaName: string
  childId: UUID
  firstName: string | null
  lastName: string | null
  nextPlacementStart: LocalDate | null
  placementEnd: LocalDate
  ssn: string | null
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.ExceededServiceNeedReportRow
*/
export interface ExceededServiceNeedReportRow {
  childFirstName: string
  childId: UUID
  childLastName: string
  excessHours: number
  groupId: UUID | null
  groupName: string | null
  serviceNeedHoursPerMonth: number
  unitId: UUID
  usedServiceHours: number
}

/**
* Generated from fi.espoo.evaka.reports.ExceededServiceNeedReportUnit
*/
export interface ExceededServiceNeedReportUnit {
  id: UUID
  name: string
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
  childDateOfBirth: LocalDate
  childFirstName: string
  childLanguage: string | null
  childLastName: string
  childPostOffice: string
  childPostalCode: string
  id: UUID
  options: string[]
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.HolidayPeriodAttendanceReportRow
*/
export interface HolidayPeriodAttendanceReportRow {
  absentCount: number
  assistanceChildren: ChildWithName[]
  date: LocalDate
  noResponseChildren: ChildWithName[]
  presentChildren: ChildWithName[]
  presentOccupancyCoefficient: number
  requiredStaff: number
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
* Generated from fi.espoo.evaka.reports.MealReportData
*/
export interface MealReportData {
  date: LocalDate
  meals: MealReportRow[]
  reportName: string
}

/**
* Generated from fi.espoo.evaka.reports.MealReportRow
*/
export interface MealReportRow {
  additionalInfo: string | null
  dietAbbreviation: string | null
  dietId: number | null
  mealCount: number
  mealId: number
  mealTextureId: number | null
  mealTextureName: string | null
  mealType: MealType
}

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
* Generated from fi.espoo.evaka.reports.NonSsnChildrenReportRow
*/
export interface NonSsnChildrenReportRow {
  childId: UUID
  dateOfBirth: LocalDate
  firstName: string
  lastName: string
  lastSentToVarda: HelsinkiDateTime | null
  ophPersonOid: string | null
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
  additionalInfo: string
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
* Generated from fi.espoo.evaka.reports.PreschoolApplicationReportRow
*/
export interface PreschoolApplicationReportRow {
  applicationId: UUID
  applicationUnitId: UUID
  applicationUnitName: string
  childDateOfBirth: LocalDate
  childFirstName: string
  childId: UUID
  childLastName: string
  childPostalCode: string
  childStreetAddress: string
  currentUnitId: UUID | null
  currentUnitName: string | null
  isDaycareAssistanceNeed: boolean
}

/**
* Generated from fi.espoo.evaka.reports.PreschoolUnitsReportRow
*/
export interface PreschoolUnitsReportRow {
  address: string
  id: UUID
  options: string[]
  postOffice: string
  postalCode: string
  unitName: string
  unitSize: number
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
  assistanceNeedVoucherCoefficient: number | null
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
  hasSocialSecurityNumber: boolean
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
  | 'CUSTOMER_FEES'
  | 'DECISIONS'
  | 'DUPLICATE_PEOPLE'
  | 'ENDED_PLACEMENTS'
  | 'EXCEEDED_SERVICE_NEEDS'
  | 'FAMILY_CONFLICT'
  | 'FAMILY_DAYCARE_MEAL_REPORT'
  | 'HOLIDAY_PERIOD_ATTENDANCE'
  | 'INVOICE'
  | 'MANUAL_DUPLICATION'
  | 'MISSING_HEAD_OF_FAMILY'
  | 'MISSING_SERVICE_NEED'
  | 'NON_SSN_CHILDREN'
  | 'OCCUPANCY'
  | 'PARTNERS_IN_DIFFERENT_ADDRESS'
  | 'PLACEMENT_COUNT'
  | 'PLACEMENT_GUARANTEE'
  | 'PLACEMENT_SKETCHING'
  | 'PRESCHOOL_ABSENCES'
  | 'PRESCHOOL_APPLICATIONS'
  | 'PRESENCE'
  | 'RAW'
  | 'SERVICE_NEED'
  | 'SERVICE_VOUCHER_VALUE'
  | 'SEXTET'
  | 'STARTING_PLACEMENTS'
  | 'TITANIA_ERRORS'
  | 'UNITS'
  | 'VARDA_ERRORS'
  | 'FUTURE_PRESCHOOLERS'
  | 'MEALS'

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
* Generated from fi.espoo.evaka.reports.SourceUnitsReportRow
*/
export interface SourceUnitsReportRow {
  address: string
  id: UUID
  postOffice: string
  postalCode: string
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
* Generated from fi.espoo.evaka.reports.TitaniaErrorConflict
*/
export interface TitaniaErrorConflict {
  overlappingShiftBegins: LocalTime
  overlappingShiftEnds: LocalTime
  shiftBegins: LocalTime
  shiftDate: LocalDate
  shiftEnds: LocalTime
}

/**
* Generated from fi.espoo.evaka.reports.TitaniaErrorEmployee
*/
export interface TitaniaErrorEmployee {
  conflictingShifts: TitaniaErrorConflict[]
  employeeName: string
  employeeNumber: string
}

/**
* Generated from fi.espoo.evaka.reports.TitaniaErrorReportRow
*/
export interface TitaniaErrorReportRow {
  requestTime: HelsinkiDateTime
  units: TitaniaErrorUnit[]
}

/**
* Generated from fi.espoo.evaka.reports.TitaniaErrorUnit
*/
export interface TitaniaErrorUnit {
  employees: TitaniaErrorEmployee[]
  unitName: string
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
  ophOrganizerOid: string | null
  ophUnitOid: string | null
  preschoolApply: boolean
  providerType: ProviderType
  unitManagerName: string
  unitManagerPhone: string
  uploadChildrenToVarda: boolean
  uploadToKoski: boolean
  uploadToVarda: boolean
}

/**
* Generated from fi.espoo.evaka.reports.VardaChildErrorReportRow
*/
export interface VardaChildErrorReportRow {
  childId: UUID
  created: HelsinkiDateTime
  errors: string[]
  resetTimeStamp: HelsinkiDateTime | null
  serviceNeedId: UUID | null
  serviceNeedOptionName: string | null
  serviceNeedValidity: FiniteDateRange | null
  updated: HelsinkiDateTime
}

/**
* Generated from fi.espoo.evaka.reports.VardaUnitErrorReportRow
*/
export interface VardaUnitErrorReportRow {
  createdAt: HelsinkiDateTime
  error: string
  erroredAt: HelsinkiDateTime
  unitId: UUID
  unitName: string
}

/**
* Generated from fi.espoo.evaka.reports.VoucherReportRowType
*/
export type VoucherReportRowType =
  | 'REFUND'
  | 'CORRECTION'
  | 'ORIGINAL'


export function deserializeJsonAssistanceNeedDecisionsReportRow(json: JsonOf<AssistanceNeedDecisionsReportRow>): AssistanceNeedDecisionsReportRow {
  return {
    ...json,
    decisionMade: (json.decisionMade != null) ? LocalDate.parseIso(json.decisionMade) : null,
    sentForDecision: LocalDate.parseIso(json.sentForDecision)
  }
}


export function deserializeJsonAttendanceReservationReportByChildBody(json: JsonOf<AttendanceReservationReportByChildBody>): AttendanceReservationReportByChildBody {
  return {
    ...json,
    range: FiniteDateRange.parseJson(json.range)
  }
}


export function deserializeJsonAttendanceReservationReportByChildGroup(json: JsonOf<AttendanceReservationReportByChildGroup>): AttendanceReservationReportByChildGroup {
  return {
    ...json,
    items: json.items.map(e => deserializeJsonAttendanceReservationReportByChildItem(e))
  }
}


export function deserializeJsonAttendanceReservationReportByChildItem(json: JsonOf<AttendanceReservationReportByChildItem>): AttendanceReservationReportByChildItem {
  return {
    ...json,
    date: LocalDate.parseIso(json.date),
    reservation: (json.reservation != null) ? TimeRange.parseJson(json.reservation) : null
  }
}


export function deserializeJsonAttendanceReservationReportRow(json: JsonOf<AttendanceReservationReportRow>): AttendanceReservationReportRow {
  return {
    ...json,
    dateTime: HelsinkiDateTime.parseIso(json.dateTime)
  }
}


export function deserializeJsonChildAttendanceReportRow(json: JsonOf<ChildAttendanceReportRow>): ChildAttendanceReportRow {
  return {
    ...json,
    attendances: json.attendances.map(e => TimeInterval.parseJson(e)),
    date: LocalDate.parseIso(json.date),
    reservations: json.reservations.map(e => TimeRange.parseJson(e))
  }
}


export function deserializeJsonDuplicatePeopleReportRow(json: JsonOf<DuplicatePeopleReportRow>): DuplicatePeopleReportRow {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}


export function deserializeJsonEndedPlacementsReportRow(json: JsonOf<EndedPlacementsReportRow>): EndedPlacementsReportRow {
  return {
    ...json,
    nextPlacementStart: (json.nextPlacementStart != null) ? LocalDate.parseIso(json.nextPlacementStart) : null,
    placementEnd: LocalDate.parseIso(json.placementEnd)
  }
}


export function deserializeJsonFuturePreschoolersReportRow(json: JsonOf<FuturePreschoolersReportRow>): FuturePreschoolersReportRow {
  return {
    ...json,
    childDateOfBirth: LocalDate.parseIso(json.childDateOfBirth)
  }
}


export function deserializeJsonHolidayPeriodAttendanceReportRow(json: JsonOf<HolidayPeriodAttendanceReportRow>): HolidayPeriodAttendanceReportRow {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonManualDuplicationReportRow(json: JsonOf<ManualDuplicationReportRow>): ManualDuplicationReportRow {
  return {
    ...json,
    connectedEndDate: LocalDate.parseIso(json.connectedEndDate),
    connectedStartDate: LocalDate.parseIso(json.connectedStartDate),
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    preschoolEndDate: LocalDate.parseIso(json.preschoolEndDate),
    preschoolStartDate: LocalDate.parseIso(json.preschoolStartDate)
  }
}


export function deserializeJsonMealReportData(json: JsonOf<MealReportData>): MealReportData {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonMissingHeadOfFamilyReportRow(json: JsonOf<MissingHeadOfFamilyReportRow>): MissingHeadOfFamilyReportRow {
  return {
    ...json,
    rangesWithoutHead: json.rangesWithoutHead.map(e => FiniteDateRange.parseJson(e))
  }
}


export function deserializeJsonNonSsnChildrenReportRow(json: JsonOf<NonSsnChildrenReportRow>): NonSsnChildrenReportRow {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    lastSentToVarda: (json.lastSentToVarda != null) ? HelsinkiDateTime.parseIso(json.lastSentToVarda) : null
  }
}


export function deserializeJsonPlacementGuaranteeReportRow(json: JsonOf<PlacementGuaranteeReportRow>): PlacementGuaranteeReportRow {
  return {
    ...json,
    placementEndDate: LocalDate.parseIso(json.placementEndDate),
    placementStartDate: LocalDate.parseIso(json.placementStartDate)
  }
}


export function deserializeJsonPlacementSketchingReportRow(json: JsonOf<PlacementSketchingReportRow>): PlacementSketchingReportRow {
  return {
    ...json,
    childDob: LocalDate.parseIso(json.childDob),
    childMovingDate: (json.childMovingDate != null) ? LocalDate.parseIso(json.childMovingDate) : null,
    preferredStartDate: LocalDate.parseIso(json.preferredStartDate),
    sentDate: LocalDate.parseIso(json.sentDate)
  }
}


export function deserializeJsonPreschoolApplicationReportRow(json: JsonOf<PreschoolApplicationReportRow>): PreschoolApplicationReportRow {
  return {
    ...json,
    childDateOfBirth: LocalDate.parseIso(json.childDateOfBirth)
  }
}


export function deserializeJsonPresenceReportRow(json: JsonOf<PresenceReportRow>): PresenceReportRow {
  return {
    ...json,
    date: LocalDate.parseIso(json.date)
  }
}


export function deserializeJsonRawReportRow(json: JsonOf<RawReportRow>): RawReportRow {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    day: LocalDate.parseIso(json.day)
  }
}


export function deserializeJsonServiceVoucherReport(json: JsonOf<ServiceVoucherReport>): ServiceVoucherReport {
  return {
    ...json,
    locked: (json.locked != null) ? LocalDate.parseIso(json.locked) : null
  }
}


export function deserializeJsonServiceVoucherUnitReport(json: JsonOf<ServiceVoucherUnitReport>): ServiceVoucherUnitReport {
  return {
    ...json,
    locked: (json.locked != null) ? LocalDate.parseIso(json.locked) : null,
    rows: json.rows.map(e => deserializeJsonServiceVoucherValueRow(e))
  }
}


export function deserializeJsonServiceVoucherValueRow(json: JsonOf<ServiceVoucherValueRow>): ServiceVoucherValueRow {
  return {
    ...json,
    childDateOfBirth: LocalDate.parseIso(json.childDateOfBirth),
    realizedPeriod: FiniteDateRange.parseJson(json.realizedPeriod)
  }
}


export function deserializeJsonStartingPlacementsRow(json: JsonOf<StartingPlacementsRow>): StartingPlacementsRow {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    placementStart: LocalDate.parseIso(json.placementStart)
  }
}


export function deserializeJsonTitaniaErrorConflict(json: JsonOf<TitaniaErrorConflict>): TitaniaErrorConflict {
  return {
    ...json,
    overlappingShiftBegins: LocalTime.parseIso(json.overlappingShiftBegins),
    overlappingShiftEnds: LocalTime.parseIso(json.overlappingShiftEnds),
    shiftBegins: LocalTime.parseIso(json.shiftBegins),
    shiftDate: LocalDate.parseIso(json.shiftDate),
    shiftEnds: LocalTime.parseIso(json.shiftEnds)
  }
}


export function deserializeJsonTitaniaErrorEmployee(json: JsonOf<TitaniaErrorEmployee>): TitaniaErrorEmployee {
  return {
    ...json,
    conflictingShifts: json.conflictingShifts.map(e => deserializeJsonTitaniaErrorConflict(e))
  }
}


export function deserializeJsonTitaniaErrorReportRow(json: JsonOf<TitaniaErrorReportRow>): TitaniaErrorReportRow {
  return {
    ...json,
    requestTime: HelsinkiDateTime.parseIso(json.requestTime),
    units: json.units.map(e => deserializeJsonTitaniaErrorUnit(e))
  }
}


export function deserializeJsonTitaniaErrorUnit(json: JsonOf<TitaniaErrorUnit>): TitaniaErrorUnit {
  return {
    ...json,
    employees: json.employees.map(e => deserializeJsonTitaniaErrorEmployee(e))
  }
}


export function deserializeJsonVardaChildErrorReportRow(json: JsonOf<VardaChildErrorReportRow>): VardaChildErrorReportRow {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    resetTimeStamp: (json.resetTimeStamp != null) ? HelsinkiDateTime.parseIso(json.resetTimeStamp) : null,
    serviceNeedValidity: (json.serviceNeedValidity != null) ? FiniteDateRange.parseJson(json.serviceNeedValidity) : null,
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}


export function deserializeJsonVardaUnitErrorReportRow(json: JsonOf<VardaUnitErrorReportRow>): VardaUnitErrorReportRow {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    erroredAt: HelsinkiDateTime.parseIso(json.erroredAt)
  }
}
