// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import { JsonOf } from 'lib-common/json'
import {
  ApplicationOrigin,
  ApplicationStatus
} from 'lib-common/api-types/application/enums'

export type UUID = string
type ISODate = string
type Timestamp = string
export type FeeDecisionStatus = 'DRAFT' | 'SENT'

export type Language = 'fi' | 'sv' | 'en'

export interface Coordinate {
  lat: number
  lon: number
}

export interface FeeDecisionThresholds {
  minIncomeThreshold: number
  maxIncomeThreshold: number
  minFee: number
  maxFee: number
  incomeMultiplier: number
}

export interface FeeDecision {
  id: UUID
  status: FeeDecisionStatus
  decisionType:
    | 'NORMAL'
    | 'RELIEF_ACCEPTED'
    | 'RELIEF_PARTLY_ACCEPTED'
    | 'RELIEF_REJECTED'
  validDuring: DateRange
  headOfFamily: { id: UUID }
  familySize: number
  feeThresholds: FeeDecisionThresholds
  children: [
    {
      child: {
        id: UUID
        dateOfBirth: string
      }
      placement: {
        unit: { id: UUID }
        type:
          | 'DAYCARE'
          | 'PRESCHOOL_WITH_DAYCARE'
          | 'PREPARATORY_WITH_DAYCARE'
          | 'FIVE_YEARS_OLD_DAYCARE'
      }
      serviceNeed: {
        feeCoefficient: number
        descriptionFi: string
        descriptionSv: string
        missing: boolean
      }
      baseFee: number
      siblingDiscount: number
      fee: number
      feeAlterations: Array<{
        type: 'DISCOUNT' | 'INCREASE' | 'RELIEF'
        amount: number
        isAbsolute: boolean
        effect: number
      }>
      finalFee: number
    }
  ]
}

export interface VoucherValueDecision {
  id: UUID
  status: 'DRAFT' | 'SENT'
  validFrom: ISODate
  validTo: ISODate
  headOfFamily: { id: UUID }
  familySize: number
  feeThresholds: FeeDecisionThresholds
  child: {
    id: UUID
    dateOfBirth: string
  }
  placement: {
    unit: { id: UUID }
    type: PlacementType
  }
  serviceNeed: {
    feeCoefficient: number
    voucherValueCoefficient: number
    feeDescriptionFi: string
    feeDescriptionSv: string
    voucherValueDescriptionFi: string
    voucherValueDescriptionSv: string
  }
  baseCoPayment: number
  siblingDiscount: number
  coPayment: number
  feeAlterations: Array<{
    type: 'DISCOUNT' | 'INCREASE' | 'RELIEF'
    amount: number
    isAbsolute: boolean
    effect: number
  }>
  finalCoPayment: number
  baseValue: number
  ageCoefficient: number
  voucherValue: number
}

export interface Invoice {
  id: UUID
  status: 'DRAFT' | 'WAITING_FOR_SENDING' | 'SENT'
  periodStart: ISODate
  periodEnd: ISODate
  dueDate?: ISODate
  invoiceDate?: ISODate
  agreementType: number
  headOfFamily: { id: UUID }
  rows: {
    id: UUID
    child: { id: UUID; dateOfBirth: ISODate }
    placementUnit: { id: UUID }
    amount: number
    unitPrice: number
    periodStart: ISODate
    periodEnd: ISODate
    product: 'DAYCARE'
    costCenter: string
    subCostCenter?: string
    modifiers: {
      product: 'DAYCARE'
      costCenter: string
      subCostCenter?: string
      amount: number
      unitPrice: number
      periodStart: ISODate
      periodEnd: ISODate
    }[]
  }[]
  number?: number
  sentBy?: UUID
  sentAt?: Timestamp
}

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
  roundTheClock: boolean
  language?: Language
  location?: Coordinate | null
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
  startDate: string
  endDate?: string
}

export interface Child {
  id: UUID
  allergies?: string
  diet?: string
  medication?: string
}

type PlacementType = 'DAYCARE' | 'PRESCHOOL' | 'PRESCHOOL_DAYCARE'

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
  ssn?: string
  email?: string
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
  id?: string
  firstName: string
  lastName: string
  externalId?: string
  email?: string
  roles: UserRole[]
  pin?: string
}

export type UserRole =
  | 'ADMIN'
  | 'FINANCE_ADMIN'
  | 'UNIT_SUPERVISOR'
  | 'SERVICE_WORKER'
  | 'STAFF'
  | 'END_USER'
  | 'DIRECTOR'
  | 'MOBILE'
  | 'MANAGER'
  | 'SPECIAL_EDUCATION_TEACHER'

export interface Application {
  id: UUID
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

export interface ApplicationForm {
  type: string
  child: ApplicationChild
  guardian: ApplicationAdult
  apply: ApplicationApply
  urgent: boolean
  partTime: boolean
  connectedDaycare: boolean
  preferredStartDate: ISODate
  serviceStart: ISODate
  serviceEnd: ISODate
  extendedCare: boolean
  careDetails: ApplicationCareDetails
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus
  guardian2?: ApplicationAdult
  hasOtherAdult: boolean
  otherAdults: ApplicationAdult[]
  hasOtherChildren: boolean
  otherChildren: ApplicationChild[]
  docVersion: number
  additionalDetails: ApplicationDaycareAdditionalDetails
}

export type OtherGuardianAgreementStatus =
  | 'AGREED'
  | 'NOT_AGREED'
  | 'RIGHT_TO_GET_NOTIFIED'
  | null

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
  unitId: UUID
  periodStart: ISODate
  periodEnd: ISODate
  preschoolDaycarePeriodStart?: ISODate | null
  preschoolDaycarePeriodEnd?: ISODate | null
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

export type DecisionType =
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PREPARATORY_EDUCATION'

export type DecisionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

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

export type DaycareDailyNoteLevel = 'GOOD' | 'MEDIUM' | 'NONE'
export type DaycareDailyNoteReminder = 'DIAPERS' | 'CLOTHES' | 'LAUNDRY'

export interface DaycareDailyNote {
  id: UUID
  date: LocalDate | null
  childId?: UUID
  groupId?: UUID
  note?: string | null
  feedingNote: DaycareDailyNoteLevel | null
  sleepingNote: DaycareDailyNoteLevel | null
  sleepingMinutes: string
  reminders: DaycareDailyNoteReminder[]
  reminderNote: string | null
  modifiedAt?: Date | null
  modifiedBy: string | null
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
