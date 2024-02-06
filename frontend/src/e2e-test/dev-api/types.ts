// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IncomeEffect, IncomeValue } from 'lib-common/api-types/income'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'
import {
  ApplicationForm,
  ApplicationOrigin,
  ApplicationStatus,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import {
  AssistanceNeedDecisionStatus,
  AssistanceNeedPreschoolDecisionForm
} from 'lib-common/generated/api-types/assistanceneed'
import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import { DailyServiceTimesType } from 'lib-common/generated/api-types/dailyservicetimes'
import { Language } from 'lib-common/generated/api-types/daycare'
import {
  DecisionStatus,
  DecisionType
} from 'lib-common/generated/api-types/decision'
import {
  DocumentContent,
  DocumentStatus,
  DocumentTemplateContent,
  DocumentType
} from 'lib-common/generated/api-types/document'
import {
  FixedPeriodQuestionnaireBody,
  HolidayPeriodBody
} from 'lib-common/generated/api-types/holidayperiod'
import { PaymentStatus } from 'lib-common/generated/api-types/invoicing'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ShiftCareType } from 'lib-common/generated/api-types/serviceneed'
import {
  Coordinate,
  PilotFeature,
  TimeRange,
  UserRole
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

export interface CareArea {
  id: UUID
  name: string
  shortName: string
  areaCode: string
  subCostCenter: string
}

export interface Daycare {
  id: UUID
  areaId: UUID
  name: string
  type: ('CENTRE' | 'PRESCHOOL' | 'PREPARATORY_EDUCATION' | 'CLUB')[]
  dailyPreschoolTime: TimeRange | null
  dailyPreparatoryTime: TimeRange | null
  openingDate?: LocalDate | null
  closingDate?: LocalDate | null
  costCenter: string
  streetAddress: string
  postalCode: string
  postOffice: string
  decisionDaycareName: string
  decisionPreschoolName: string
  decisionHandler: string
  decisionHandlerAddress: string
  daycareApplyPeriod?: DateRange | null
  preschoolApplyPeriod?: DateRange | null
  clubApplyPeriod?: DateRange | null
  providerType:
    | 'MUNICIPAL'
    | 'MUNICIPAL_SCHOOL'
    | 'PURCHASED'
    | 'PRIVATE'
    | 'PRIVATE_SERVICE_VOUCHER'
    | 'EXTERNAL_PURCHASED'
  operationDays: number[]
  operationTimes: (TimeRange | null)[]
  roundTheClock: boolean
  language?: Language
  location?: Coordinate | null
  enabledPilotFeatures: PilotFeature[]
  invoicedByMunicipality?: boolean | null
  businessId: string
  iban: string
  providerId: string
}

export interface ChildAttendance {
  childId: UUID
  unitId: UUID
  date: LocalDate
  arrived: LocalTime
  departed: LocalTime | null
}

export interface DaycareGroup {
  id: UUID
  daycareId: UUID
  name: string
  startDate: LocalDate
}

export interface DaycareCaretakers {
  groupId: UUID
  amount: number
  startDate: LocalDate
  endDate: LocalDate | null
}

export interface Child {
  id: UUID
  allergies?: string
  diet?: string
  medication?: string
}

export interface DaycarePlacement {
  id: UUID
  type: PlacementType
  childId: UUID
  unitId: UUID
  startDate: LocalDate
  endDate: LocalDate
  placeGuarantee: boolean
}

export interface DaycareGroupPlacement {
  id: UUID
  daycareGroupId: UUID
  daycarePlacementId: UUID
  startDate: LocalDate
  endDate: LocalDate
}

export interface BackupCare {
  id: UUID
  childId: UUID
  unitId: UUID
  groupId?: UUID
  period: {
    start: LocalDate
    end: LocalDate
  }
}

export interface PersonDetail {
  id: string
  dateOfBirth: LocalDate
  dateOfDeath?: LocalDate
  firstName: string
  lastName: string
  preferredName?: string
  ssn?: string
  email?: string | null
  phone?: string
  language?: string
  residenceCode?: string
  streetAddress?: string
  postalCode?: string
  postOffice?: string
  nationalities?: string[]
  restrictedDetailsEnabled?: boolean
  restrictedDetailsEndDate?: LocalDate | null
  ophPersonOid?: string | null
  duplicateOf?: string | null
}

export interface PersonDetailWithDependantsAndGuardians extends PersonDetail {
  dependants?: PersonDetailWithDependantsAndGuardians[]
  guardians?: PersonDetailWithDependantsAndGuardians[]
}

export interface Family {
  guardian: PersonDetailWithDependantsAndGuardians
  otherGuardian?: PersonDetailWithDependantsAndGuardians
  children: PersonDetailWithDependantsAndGuardians[]
}

export interface EmployeeDetail {
  id: string
  firstName: string
  lastName: string
  externalId: string
  roles: UserRole[]
  email?: string
  preferredFirstName?: string
}

export interface Application {
  id: UUID
  type: ApplicationType
  createdDate?: LocalDate
  modifiedDate?: LocalDate
  sentDate?: LocalDate
  dueDate?: LocalDate
  status: ApplicationStatus
  guardianId: UUID
  childId: UUID
  origin: ApplicationOrigin
  checkedByAdmin: boolean
  hideFromGuardian: boolean
  transferApplication: boolean
  otherGuardianId?: UUID
  form: ApplicationForm
}

export interface ApplicationDaycareAdditionalDetails {
  allergyType: string
  dietType: string
  otherInfo: string
}

export interface ApplicationCareDetails {
  preparatory?: boolean
  assistanceNeeded: boolean
  assistanceDescription?: string
}

export interface ApplicationApply {
  preferredUnits: UUID[]
  siblingBasis: boolean
  siblingName?: string
  siblingSsn?: string
}

export interface ApplicationAddress {
  street?: string
  postalCode?: string
  city?: string
  editable: boolean
}

export interface ApplicationChild {
  firstName?: string
  lastName?: string
  socialSecurityNumber?: string
  dateOfBirth?: LocalDate
  address?: ApplicationAddress
  nationality?: string
  language?: string
  hasCorrectingAddress?: boolean
  correctingAddress?: ApplicationAddress
  childMovingDate?: LocalDate
  restricted: boolean
}

export interface ApplicationAdult {
  firstName?: string
  lastName?: string
  socialSecurityNumber?: string
  address?: ApplicationAddress
  phoneNumber?: string
  email?: string
  hasCorrectingAddress?: boolean
  guardianMovingDate?: LocalDate
  restricted: boolean
}

export interface PlacementPlan {
  applicationId: UUID
  unitId: UUID
  periodStart: LocalDate
  periodEnd: LocalDate
  preschoolDaycarePeriodStart?: LocalDate | null
  preschoolDaycarePeriodEnd?: LocalDate | null
}

export type DevHolidayPeriod = HolidayPeriodBody & {
  id: UUID
}

export type DevFixedPeriodQuestionnaire = FixedPeriodQuestionnaireBody & {
  id: UUID
}

export interface SuomiFiMessage {
  messageId: string
  documentId: string
  documentBucket: string
  documentKey: string
  documentDisplayName: string
  ssn: string
  firstName: string
  lastName: string
  language: string
  streetAddress: string
  postalCode: string
  postOffice: string
  countryCode: string
  messageHeader: string
  messageContent: string
  emailHeader: string
  emailContent: string
}

export interface VtjPerson {
  firstNames: string
  lastName: string
  socialSecurityNumber: string
  address: VtjPersonAddress | null
  dependants: VtjPerson[]
  guardians: VtjPerson[]
  nationalities: VtjNationality[]
  nativeLanguage: VtjNativeLanguage | null
  restrictedDetails: VtjRestrictedDetails | null
  dateOfDeath: LocalDate | null
  residenceCode: string | null
}

export interface VtjPersonAddress {
  streetAddress: string | null
  postalCode: string | null
  postOffice: string | null
  streetAddressSe: string | null
  postOfficeSe: string | null
}

export interface VtjNationality {
  countryName: string
  countryCode: string
}

export interface VtjNativeLanguage {
  languageName: string
  code: string
}

export interface VtjRestrictedDetails {
  enabled: boolean
  endDate: LocalDate | null
}

export interface DecisionFixture {
  id: string
  employeeId: string
  applicationId: string
  unitId: string
  type: DecisionType
  startDate: LocalDate
  endDate: LocalDate
  status: DecisionStatus
}

export interface Decision {
  id: UUID
  type: DecisionType
  startDate: LocalDate
  endDate: LocalDate
  unit: DecisionUnit
  applicationId: UUID
  childId: UUID
  childName: string
  documentKey: string | null
  decisionNumber: number
  sentDate: LocalDate
  status: DecisionStatus
}

export interface DecisionUnit {
  id: UUID
  name: string
  daycareDecisionName: string
  preschoolDecisionName: string
  manager: string | null
  streetAddress: string
  postalCode: string
  postOffice: string
  approverName: string
  decisionHandler: string
  decisionHandlerAddress: string
}

export function deserializeDecision(json: JsonOf<Decision>): Decision {
  return {
    ...json,
    startDate: LocalDate.parseIso(json.startDate),
    endDate: LocalDate.parseIso(json.endDate),
    sentDate: LocalDate.parseIso(json.sentDate)
  }
}

export interface Email {
  traceId: string
  toAddress: string
  fromAddress: string
  content: {
    subject: string
    html: string
    language: string
    text: string
  }
}

export interface FamilyContact {
  id: string
  childId: string
  contactPersonId: string
  priority: number
}

export interface BackupPickup {
  id: string
  childId: string
  name: string
  phone: string
}

export interface FridgeChild {
  id: string
  childId: string
  headOfChild: string
  startDate: LocalDate
  endDate: LocalDate
}

export interface FridgePartner {
  partnershipId: string
  indx: number
  otherIndx: number
  personId: string
  startDate: LocalDate
  endDate: LocalDate
  createdAt: HelsinkiDateTime
}

export interface FosterParent {
  childId: string
  parentId: string
  validDuring: DateRange
}

export interface EmployeePin {
  id: string
  employeeExternalId?: string
  userId?: string
  pin: string
  locked?: boolean
}

export interface PedagogicalDocument {
  id: string
  childId: string
  description: string
  created?: HelsinkiDateTime
  createdBy?: string
  updated?: HelsinkiDateTime
  updatedBy?: string
}

export interface ServiceNeedFixture {
  id: string
  placementId: string
  startDate: LocalDate
  endDate: LocalDate
  optionId: string
  shiftCare: ShiftCareType
  confirmedBy: UUID
  confirmedAt: LocalDate
}

export interface DevAssistanceNeedPreschoolDecision {
  id: UUID
  decisionNumber: number
  childId: UUID
  form: AssistanceNeedPreschoolDecisionForm
  status: AssistanceNeedDecisionStatus
  annulmentReason: string
  sentForDecision: LocalDate | null
  decisionMade: LocalDate | null
  unreadGuardianIds: UUID[] | null
}

export interface AssistanceNeedDecision {
  assistanceLevels: (
    | 'ASSISTANCE_ENDS'
    | 'ASSISTANCE_SERVICES_FOR_TIME'
    | 'ENHANCED_ASSISTANCE'
    | 'SPECIAL_ASSISTANCE'
  )[]
  careMotivation: string | null
  childId: UUID
  decisionMade: LocalDate | null
  decisionMaker: {
    employeeId: UUID | null
    title: string | null
  } | null
  decisionNumber: number | null
  endDate: LocalDate | null
  expertResponsibilities: string | null
  guardianInfo: {
    details: string | null
    id: UUID | null
    isHeard: boolean
    name: string
    personId: UUID | null
  }[]
  guardiansHeardOn: LocalDate | null
  id: UUID | null
  language: 'FI' | 'SV'
  motivationForDecision: string | null
  otherRepresentativeDetails: string | null
  otherRepresentativeHeard: boolean
  pedagogicalMotivation: string | null
  preparedBy1: {
    employeeId: UUID | null
    title: string | null
    phoneNumber: string | null
  } | null
  preparedBy2: {
    employeeId: UUID | null
    title: string | null
    phoneNumber: string | null
  } | null
  selectedUnit: {
    id: UUID | null
  } | null
  sentForDecision: LocalDate | null
  serviceOptions: {
    consultationSpecialEd: boolean
    fullTimeSpecialEd: boolean
    interpretationAndAssistanceServices: boolean
    partTimeSpecialEd: boolean
    specialAides: boolean
  }
  servicesMotivation: string | null
  validityPeriod: DateRange
  status: AssistanceNeedDecisionStatus
  structuralMotivationDescription: string | null
  structuralMotivationOptions: {
    additionalStaff: boolean
    childAssistant: boolean
    groupAssistant: boolean
    smallGroup: boolean
    smallerGroup: boolean
    specialGroup: boolean
  }
  viewOfGuardians: string | null
  unreadGuardianIds: UUID[] | null
  annulmentReason: string
}

export interface DevIncome {
  id: string
  personId: string
  validFrom: LocalDate
  validTo: LocalDate
  data: Record<string, IncomeValue>
  effect: IncomeEffect
  updatedAt: HelsinkiDateTime
  updatedBy: string
}

export interface DevVardaReset {
  evakaChildId: string
  resetTimestamp: HelsinkiDateTime | null
}

export interface DevVardaServiceNeed {
  evakaServiceNeedId: string
  evakaServiceNeedUpdated: HelsinkiDateTime
  evakaChildId: string
  updateFailed: boolean
  errors: string[]
}

export interface DevStaffOccupancyCoefficient {
  coefficient: number
  employeeId: UUID
  unitId: UUID
}

export interface DevDailyServiceTime {
  id: UUID
  childId: UUID
  type: DailyServiceTimesType
  validityPeriod: DateRange
  regularTimes: TimeRange | null
  mondayTimes: TimeRange | null
  tuesdayTimes: TimeRange | null
  wednesdayTimes: TimeRange | null
  thursdayTimes: TimeRange | null
  fridayTimes: TimeRange | null
  saturdayTimes: TimeRange | null
  sundayTimes: TimeRange | null
}

export interface DevDailyServiceTimeNotification {
  id: UUID
  guardianId: UUID
  dailyServiceTimeId: UUID
  dateFrom: LocalDate
  hasDeletedReservations: boolean
}

export interface DevPayment {
  id: UUID
  unitId: UUID
  unitName: string
  unitBusinessId: string | null
  unitIban: string | null
  unitProviderId: string | null
  period: FiniteDateRange
  number: number
  amount: number
  status: PaymentStatus
  paymentDate: LocalDate | null
  dueDate: LocalDate | null
  sentAt: HelsinkiDateTime | null
  sentBy: UUID | null
}

export interface DevRealtimeStaffAttendance {
  id: UUID
  employeeId: UUID
  groupId: UUID | null
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
  occupancyCoefficient: number
  type: StaffAttendanceType
  departedAutomatically: boolean
}

export interface DevCalendarEvent {
  id: UUID
  title: string
  description: string
  period: FiniteDateRange
}

export interface DevCalendarEventAttendee {
  id: UUID
  calendarEventId: UUID
  unitId: UUID
  groupId: UUID | null
  childId: UUID | null
}

export interface DevStaffAttendancePlan {
  id: UUID
  employeeId: UUID
  type: StaffAttendanceType
  startTime: HelsinkiDateTime
  endTime: HelsinkiDateTime
  description: string | null
}

export interface DevAbsence {
  id: UUID
  childId: UUID
  date: LocalDate
  absenceType: AbsenceType
  modifiedAt: HelsinkiDateTime
  modifiedBy: UUID
  absenceCategory: AbsenceCategory
  questionnaireId: UUID | null
}

export interface DevHoliday {
  date: LocalDate
  description: string
}

export interface DevDocumentTemplate {
  id: UUID
  name: string
  type: DocumentType
  language: string
  confidential: boolean
  legalBasis: string
  validity: DateRange
  published: boolean
  content: DocumentTemplateContent
}

export interface DevChildDocument {
  id: UUID
  status: DocumentStatus
  childId: UUID
  templateId: UUID
  content: DocumentContent
  publishedContent: DocumentContent | null
  modifiedAt: HelsinkiDateTime | null
  publishedAt: HelsinkiDateTime | null
}
