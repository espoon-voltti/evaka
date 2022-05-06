// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { IncomeEffect, IncomeValue } from 'lib-common/api-types/income'
import { HighestFee } from 'lib-common/api-types/incomeStatement'
import DateRange from 'lib-common/date-range'
import {
  ApplicationForm,
  ApplicationOrigin,
  ApplicationStatus,
  ApplicationType
} from 'lib-common/generated/api-types/application'
import { Language } from 'lib-common/generated/api-types/daycare'
import {
  DecisionStatus,
  DecisionType
} from 'lib-common/generated/api-types/decision'
import {
  FixedPeriodQuestionnaireBody,
  HolidayPeriodBody
} from 'lib-common/generated/api-types/holidayperiod'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import {
  Coordinate,
  PilotFeature,
  UserRole
} from 'lib-common/generated/api-types/shared'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

type ISODate = string

export type HighestFeeFixture = Pick<
  HighestFee,
  'type' | 'startDate' | 'endDate'
>

export interface CareArea {
  id: UUID
  name: string
  shortName: string
  areaCode: string
  subCostCenter: string
}

export interface Daycare {
  id: UUID
  careAreaId: UUID
  name: string
  type: ('CENTRE' | 'PRESCHOOL' | 'PREPARATORY_EDUCATION' | 'CLUB')[]
  openingDate?: string | null
  closingDate?: string | null
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
  roundTheClock: boolean
  language?: Language
  location?: Coordinate | null
  enabledPilotFeatures: PilotFeature[]
}

export interface ChildAttendance {
  childId: UUID
  unitId: UUID
  arrived: Date
  departed: Date
}

export interface DaycareGroup {
  id: UUID
  daycareId: UUID
  name: string
  startDate: string
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
  startDate: string
  endDate: string
}

export interface DaycareGroupPlacement {
  id: UUID
  daycareGroupId: UUID
  daycarePlacementId: UUID
  startDate: string
  endDate: string
}

export interface BackupCare {
  id: UUID
  childId: UUID
  unitId: UUID
  groupId?: UUID
  period: {
    start: string
    end: string
  }
}

export interface PersonDetail {
  id: string
  dateOfBirth: string
  dateOfDeath?: string
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
  restrictedDetailsEndDate?: string | null
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
  email?: string
  roles: UserRole[]
  pin?: string
}

export interface Application {
  id: UUID
  type: ApplicationType
  createdDate?: ISODate
  modifiedDate?: ISODate
  sentDate?: ISODate
  dueDate?: ISODate
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
  dateOfBirth?: ISODate
  address?: ApplicationAddress
  nationality?: string
  language?: string
  hasCorrectingAddress?: boolean
  correctingAddress?: ApplicationAddress
  childMovingDate?: ISODate
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
  guardianMovingDate?: ISODate
  restricted: boolean
}

export interface PlacementPlan {
  applicationId: UUID
  unitId: UUID
  periodStart: ISODate
  periodEnd: ISODate
  preschoolDaycarePeriodStart?: ISODate | null
  preschoolDaycarePeriodEnd?: ISODate | null
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
  dateOfDeath: ISODate | null
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
  endDate: ISODate | null
}

export interface DecisionFixture {
  id: string
  employeeId: string
  applicationId: string
  unitId: string
  type: DecisionType
  startDate: ISODate
  endDate: ISODate
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

export interface ApplicationEmail {
  personId: string
  toAddress: string | null
  language: string
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
  personId: string
  startDate: LocalDate
  endDate: LocalDate
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
  created?: Date
  createdBy?: string
  updated?: Date
  updatedBy?: string
}

export interface ServiceNeedFixture {
  id: string
  placementId: string
  startDate: Date
  endDate: Date
  optionId: string
  shiftCare: boolean
  confirmedBy: UUID
  confirmedAt: LocalDate
}

export interface AssistanceNeed {
  id: string
  updatedBy: string
  childId: string
  startDate: Date
  endDate: Date
  capacityFactor: number
  description: string
  otherBasis: string
}

export interface DevIncome {
  id: string
  personId: string
  validFrom: LocalDate
  validTo: LocalDate
  data: Record<string, IncomeValue>
  effect: IncomeEffect
  updatedAt: Date
  updatedBy: string
}

export interface DevVardaReset {
  evakaChildId: string
  resetTimestamp: Date | null
}

export interface DevVardaServiceNeed {
  evakaServiceNeedId: string
  evakaServiceNeedUpdated: Date
  evakaChildId: string
  updateFailed: boolean
  errors: string[]
}

export interface DevStaffOccupancyCoefficient {
  coefficient: number
  employeeId: UUID
  unitId: UUID
}
