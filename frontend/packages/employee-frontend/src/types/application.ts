// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '~types/index'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Decision } from 'types/decision'
import { PersonDetails } from '~types/person'
import {
  PlacementPlanConfirmationStatus,
  PlacementPlanRejectReason
} from '~types/unit'
import { PlacementType } from '~types/placementdraft'

export interface ApplicationSummary {
  applicationId: UUID
  childId: UUID
  guardianId: UUID
  childName: string
  childSsn: string
  guardianName: string
  startDate: LocalDate
  sentDate: LocalDate | null
  preferredUnitId: UUID
  preferredUnitName: string
  type: string
  status: string
  connectedDaycare: boolean
  preparatoryEducation: boolean
}

export type ApplicationType = 'CLUB' | 'DAYCARE' | 'PRESCHOOL'

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

interface SiblingBasis {
  siblingName: string
  siblingSsn: string
}

interface ServiceNeed {
  startTime: string
  endTime: string
  shiftCare: boolean
  partTime: boolean
}

export interface PreferredUnit {
  id: UUID
  name: string
}

interface Preferences {
  preferredUnits: PreferredUnit[]
  preferredStartDate: LocalDate | null
  serviceNeed: ServiceNeed | null
  siblingBasis: SiblingBasis | null
  preparatory: boolean
  urgent: boolean
}

export interface Address {
  street: string
  postalCode: string
  postOffice: string
}

export interface FutureAddress extends Address {
  movingDate: LocalDate | null
}

export interface PersonBasics {
  firstName: string
  lastName: string
  socialSecurityNumber: string
}

interface SecondGuardian {
  separated: boolean
  agreed: boolean
  person: PersonBasics
  address: Address | null
  futureAddress: FutureAddress | null
  phoneNumber: string
  email: string
  agreementStatus: OtherGuardianAgreementStatus
}

type OtherGuardianAgreementStatus =
  | 'AGREED'
  | 'NOT_AGREED'
  | 'RIGHT_TO_GET_NOTIFIED'
  | 'NOT_SET'

interface Guardian {
  person: PersonBasics
  address: Address | null
  futureAddress: FutureAddress | null
  phoneNumber: string
  email: string
}

interface ChildDetails {
  person: PersonBasics
  dateOfBirth: LocalDate | null // TODO: Can non-nullability be ensured?
  address: Address | null
  futureAddress: FutureAddress | null
  nationality: string
  language: string
  allergies: string
  diet: string
  assistanceNeeded: boolean
  assistanceDescription: string
}

interface ClubDetails {
  wasOnClubCare: boolean
  wasOnDaycare: boolean
}

interface ApplicationForm {
  child: ChildDetails
  guardian: Guardian
  secondGuardian: SecondGuardian | null
  otherPartner: PersonBasics | null
  otherChildren: PersonBasics[]
  preferences: Preferences
  otherInfo: string
  maxFeeAccepted: boolean
  clubDetails: ClubDetails | null
}

export interface ApplicationDetails {
  id: UUID
  type: ApplicationType
  form: ApplicationForm
  status: ApplicationStatus
  origin: ApplicationOrigin
  childId: UUID
  guardianId: UUID
  otherGuardianId: UUID | null
  childRestricted: boolean
  guardianRestricted: boolean
  otherGuardianRestricted: boolean
  otherGuardianLivesInSameAddress: boolean
  transferApplication: boolean
  createdDate: Date | null
  modifiedDate: Date | null
  sentDate: LocalDate | null
  dueDate: LocalDate | null
}

export interface ApplicationResponse {
  application: ApplicationDetails
  decisions: Decision[]
  guardians: PersonDetails[]
  attachments: Attachment[]
}

export type SortByApplications =
  | 'APPLICATION_TYPE'
  | 'CHILD_NAME'
  | 'DUE_DATE'
  | 'START_DATE'
  | 'STATUS'

export interface ApplicationsSearchResponse {
  data: ApplicationListSummary[]
  pages: number
  totalCount: number
}

export interface ApplicationListSummary {
  id: UUID
  firstName: string
  lastName: string
  socialSecurityNumber: string
  dateOfBirth: LocalDate | null
  type: string
  placementType: PlacementType
  dueDate: LocalDate | null
  startDate: LocalDate | null
  preferredUnits: Unit[]
  origin: ApplicationOrigin
  transferApplication: boolean
  checkedByAdmin: boolean
  status: ApplicationStatus
  additionalInfo: boolean
  siblingBasis: boolean
  assistanceNeed: boolean
  wasOnClubCare: boolean | null
  wasOnDaycare: boolean | null
  extendedCare: boolean
  duplicateApplication: boolean
  placementProposalStatus: PlacementProposalStatus | null
  placementProposalUnitName: string | null
}

interface PlacementProposalStatus {
  unitConfirmationStatus: PlacementPlanConfirmationStatus
  unitRejectReason: PlacementPlanRejectReason | null
  unitRejectOtherReason: string | null
}

interface Unit {
  id: UUID
  name: string
}

export interface ApplicationSearchParams {
  area?: string
  units?: string
  basis?: string
  type: string
  preschoolType?: string
  status?: string
  dateType?: string
  distinctions?: string
  periodStart?: string
  periodEnd?: string
  searchTerms?: string
  transferApplications?: string
}

export type ApplicationSummaryStatus =
  | 'SENT'
  | 'WAITING_PLACEMENT'
  | 'WAITING_DECISION'
  | 'WAITING_UNIT_CONFIRMATION'
  | 'WAITING_MAILING'
  | 'WAITING_CONFIRMATION'
  | 'REJECTED'
  | 'ACTIVE'
  | 'CANCELLED'

export interface ApplicationNote {
  id: UUID
  applicationId: UUID
  text: string
  created: Date
  createdBy: UUID
  createdByName: string
  updated: Date
  updatedBy: UUID | null
  updatedByName: string | null
}

export interface Attachment {
  id: UUID
  name: string
  contentType: string
}
