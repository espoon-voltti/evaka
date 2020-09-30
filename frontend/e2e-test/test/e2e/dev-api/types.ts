// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export type UUID = string
type ISODate = string
type Timestamp = string
export type FeeDecisionStatus = 'DRAFT' | 'SENT'

export interface FeeDecision {
  id: UUID
  status: FeeDecisionStatus
  decisionType:
    | 'NORMAL'
    | 'RELIEF_ACCEPTED'
    | 'RELIEF_PARTLY_ACCEPTED'
    | 'RELIEF_REJECTED'
  validFrom: ISODate
  validTo: ISODate
  headOfFamily: { id: UUID }
  pricing: {
    multiplier: string
    maxThresholdDifference: number
    minThreshold2: number
    minThreshold3: number
    minThreshold4: number
    minThreshold5: number
    minThreshold6: number
    thresholdIncrease6Plus: number
  }
  parts: [
    {
      child: {
        id: UUID
        dateOfBirth: string
      }
      placement: {
        unit: UUID
        type:
          | 'DAYCARE'
          | 'PRESCHOOL_WITH_DAYCARE'
          | 'PREPARATORY_WITH_DAYCARE'
          | 'FIVE_YEARS_OLD_DAYCARE'
        serviceNeed:
          | 'MISSING'
          | 'GTE_35'
          | 'GT_25_LT_35'
          | 'LTE_25'
          | 'GTE_25'
          | 'GT_15_LT_25'
          | 'LTE_15'
          | 'LTE_0'
      }
      baseFee: number
      siblingDiscount: number
      fee: number
    }
  ]
}

export interface DevPricing {
  id?: string
  validFrom: ISODate
  validTo: ISODate | null
  multiplier: string
  maxThresholdDifference: number
  minThreshold2: number
  minThreshold3: number
  minThreshold4: number
  minThreshold5: number
  minThreshold6: number
  thresholdIncrease6Plus: number
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
  canApplyClub?: boolean
  canApplyDaycare?: boolean
  canApplyPreschool?: boolean
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
  // todo: could add support for allergies etc
}

type PlacementType = 'DAYCARE' | 'PRESCHOOL'

export interface DaycarePlacement {
  id: UUID
  type: PlacementType
  childId: UUID
  unitId: UUID
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

export interface ApplicationPersonDetail extends PersonDetail {
  ssn: string
}

export interface PersonDetail {
  id: string
  dateOfBirth: string
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

export interface EmployeeDetail {
  id?: string
  firstName: string
  lastName: string
  aad?: string
  email?: string
  roles: UserRole[]
}

export type UserRole =
  | 'ADMIN'
  | 'FINANCE_ADMIN'
  | 'UNIT_SUPERVISOR'
  | 'SERVICE_WORKER'
  | 'STAFF'
  | 'END_USER'
  | 'DIRECTOR'

export type ApplicationStatus =
  | 'CREATED'
  | 'SENT'
  | 'WAITING_PLACEMENT'
  | 'WAITING_DECISION'
  | 'WAITING_UNIT_CONFIRMATION'
  | 'WAITING_MAILING'
  | 'WAITING_CONFIRMATION'
  | 'REJECTED'
  | 'ACTIVE'
  | 'CANCELLED'

export type ApplicationOrigin = 'ELECTRONIC' | 'PAPER'

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
  documentUri: string
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

export interface Decision {
  id: string
  employeeId: string
  applicationId: string
  unitId: string
  type: DecisionType
  startDate: ISODate
  endDate: ISODate
}

export interface ApplicationEmail {
  personId: string
  toAddress: string | null
  language: string
}
