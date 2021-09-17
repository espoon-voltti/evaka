// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import { AttachmentType } from './attachment'
import { CreatePersonBody } from './pis'
import { Decision } from './decision'
import { DecisionDraft } from './decision'
import { DecisionStatus } from './decision'
import { DecisionType } from './decision'
import { DecisionUnit } from './decision'
import { PersonJSON } from './pis'
import { PlacementPlanConfirmationStatus } from './placement'
import { PlacementPlanRejectReason } from './placement'
import { PlacementType } from './placement'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.application.AcceptDecisionRequest
*/
export interface AcceptDecisionRequest {
  decisionId: UUID
  requestedStartDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.application.Address
*/
export interface Address {
  postOffice: string
  postalCode: string
  street: string
}

/**
* Generated from fi.espoo.evaka.application.ApplicationAttachment
*/
export interface ApplicationAttachment {
  contentType: string
  id: UUID
  name: string
  receivedAt: Date
  type: AttachmentType
  updated: Date
  uploadedByEmployee: UUID | null
  uploadedByPerson: UUID | null
}

/**
* Generated from fi.espoo.evaka.application.ApplicationDecisions
*/
export interface ApplicationDecisions {
  applicationId: UUID
  childName: string
  decisions: DecisionSummary[]
}

/**
* Generated from fi.espoo.evaka.application.ApplicationDetails
*/
export interface ApplicationDetails {
  additionalDaycareApplication: boolean
  attachments: ApplicationAttachment[]
  checkedByAdmin: boolean
  childId: UUID
  childRestricted: boolean
  createdDate: Date | null
  dueDate: LocalDate | null
  dueDateSetManuallyAt: Date | null
  form: ApplicationForm
  guardianDateOfDeath: LocalDate | null
  guardianId: UUID
  guardianRestricted: boolean
  hideFromGuardian: boolean
  id: UUID
  modifiedDate: Date | null
  origin: ApplicationOrigin
  otherGuardianId: UUID | null
  otherGuardianLivesInSameAddress: boolean | null
  sentDate: LocalDate | null
  status: ApplicationStatus
  transferApplication: boolean
  type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.application.ApplicationForm
*/
export interface ApplicationForm {
  child: ChildDetails
  clubDetails: ClubDetails | null
  guardian: Guardian
  maxFeeAccepted: boolean
  otherChildren: PersonBasics[]
  otherInfo: string
  otherPartner: PersonBasics | null
  preferences: Preferences
  secondGuardian: SecondGuardian | null
}

/**
* Generated from fi.espoo.evaka.application.ApplicationFormUpdate
*/
export interface ApplicationFormUpdate {
  child: ChildDetailsUpdate
  clubDetails: ClubDetails | null
  guardian: GuardianUpdate
  maxFeeAccepted: boolean
  otherChildren: PersonBasics[]
  otherInfo: string
  otherPartner: PersonBasics | null
  preferences: Preferences
  secondGuardian: SecondGuardian | null
}

/**
* Generated from fi.espoo.evaka.application.ApplicationOrigin
*/
export type ApplicationOrigin = 
  | 'ELECTRONIC'
  | 'PAPER'

/**
* Generated from fi.espoo.evaka.application.ApplicationResponse
*/
export interface ApplicationResponse {
  application: ApplicationDetails
  attachments: ApplicationAttachment[]
  decisions: Decision[]
  guardians: PersonJSON[]
}

/**
* Generated from fi.espoo.evaka.application.ApplicationSortColumn
*/
export type ApplicationSortColumn = 
  | 'APPLICATION_TYPE'
  | 'CHILD_NAME'
  | 'DUE_DATE'
  | 'START_DATE'
  | 'STATUS'

/**
* Generated from fi.espoo.evaka.application.ApplicationSortDirection
*/
export type ApplicationSortDirection = 
  | 'ASC'
  | 'DESC'

/**
* Generated from fi.espoo.evaka.application.ApplicationStatus
*/
export type ApplicationStatus = 
  | 'CREATED'
  | 'SENT'
  | 'WAITING_PLACEMENT'
  | 'WAITING_UNIT_CONFIRMATION'
  | 'WAITING_DECISION'
  | 'WAITING_MAILING'
  | 'WAITING_CONFIRMATION'
  | 'REJECTED'
  | 'ACTIVE'
  | 'CANCELLED'

/**
* Generated from fi.espoo.evaka.application.ApplicationSummary
*/
export interface ApplicationSummary {
  additionalDaycareApplication: boolean
  additionalInfo: boolean
  assistanceNeed: boolean
  attachmentCount: number
  checkedByAdmin: boolean
  currentPlacementUnit: PreferredUnit | null
  dateOfBirth: string | null
  dueDate: string | null
  duplicateApplication: boolean
  extendedCare: boolean
  firstName: string
  id: UUID
  lastName: string
  origin: ApplicationOrigin
  placementProposalStatus: PlacementProposalStatus | null
  placementProposalUnitName: string | null
  placementType: PlacementType
  preferredUnits: PreferredUnit[]
  serviceNeed: ServiceNeedOption | null
  siblingBasis: boolean
  socialSecurityNumber: string | null
  startDate: string | null
  status: ApplicationStatus
  transferApplication: boolean
  type: ApplicationType
  urgent: boolean
  wasOnClubCare: boolean | null
  wasOnDaycare: boolean | null
}

/**
* Generated from fi.espoo.evaka.application.ApplicationType
*/
export type ApplicationType = 
  | 'CLUB'
  | 'DAYCARE'
  | 'PRESCHOOL'

/**
* Generated from fi.espoo.evaka.application.ApplicationTypeToggle
*/
export type ApplicationTypeToggle = 
  | 'CLUB'
  | 'DAYCARE'
  | 'PRESCHOOL'
  | 'ALL'

/**
* Generated from fi.espoo.evaka.application.ApplicationUpdate
*/
export interface ApplicationUpdate {
  dueDate: LocalDate | null
  form: ApplicationFormUpdate
}

/**
* Generated from fi.espoo.evaka.application.ApplicationsOfChild
*/
export interface ApplicationsOfChild {
  applicationSummaries: CitizenApplicationSummary[]
  childId: UUID
  childName: string
}

/**
* Generated from fi.espoo.evaka.application.ChildDetails
*/
export interface ChildDetails {
  address: Address | null
  allergies: string
  assistanceDescription: string
  assistanceNeeded: boolean
  dateOfBirth: LocalDate | null
  diet: string
  futureAddress: FutureAddress | null
  language: string
  nationality: string
  person: PersonBasics
}

/**
* Generated from fi.espoo.evaka.application.ChildDetailsUpdate
*/
export interface ChildDetailsUpdate {
  allergies: string
  assistanceDescription: string
  assistanceNeeded: boolean
  diet: string
  futureAddress: FutureAddress | null
}

/**
* Generated from fi.espoo.evaka.application.ChildInfo
*/
export interface ChildInfo {
  firstName: string
  lastName: string
  ssn: string | null
}

/**
* Generated from fi.espoo.evaka.application.CitizenApplicationSummary
*/
export interface CitizenApplicationSummary {
  allPreferredUnitNames: string[]
  applicationId: UUID
  applicationStatus: ApplicationStatus
  childId: UUID
  childName: string | null
  createdDate: Date
  modifiedDate: Date
  preferredUnitName: string | null
  sentDate: LocalDate | null
  startDate: LocalDate | null
  transferApplication: boolean
  type: string
}

/**
* Generated from fi.espoo.evaka.application.ClubDetails
*/
export interface ClubDetails {
  wasOnClubCare: boolean
  wasOnDaycare: boolean
}

/**
* Generated from fi.espoo.evaka.application.CreateApplicationBody
*/
export interface CreateApplicationBody {
  childId: UUID
  type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.application.DaycarePlacementPlan
*/
export interface DaycarePlacementPlan {
  period: FiniteDateRange
  preschoolDaycarePeriod: FiniteDateRange | null
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.application.DecisionDraftJSON
*/
export interface DecisionDraftJSON {
  child: ChildInfo
  decisions: DecisionDraft[]
  guardian: GuardianInfo
  otherGuardian: GuardianInfo | null
  placementUnitName: string
  unit: DecisionUnit
}

/**
* Generated from fi.espoo.evaka.application.DecisionSummary
*/
export interface DecisionSummary {
  decisionId: UUID
  resolved: LocalDate | null
  sentDate: LocalDate
  status: DecisionStatus
  type: DecisionType
}

/**
* Generated from fi.espoo.evaka.application.FutureAddress
*/
export interface FutureAddress {
  movingDate: LocalDate | null
  postOffice: string
  postalCode: string
  street: string
}

/**
* Generated from fi.espoo.evaka.application.Guardian
*/
export interface Guardian {
  address: Address | null
  email: string
  futureAddress: FutureAddress | null
  person: PersonBasics
  phoneNumber: string
}

/**
* Generated from fi.espoo.evaka.application.GuardianInfo
*/
export interface GuardianInfo {
  firstName: string
  id: UUID | null
  isVtjGuardian: boolean
  lastName: string
  ssn: string | null
}

/**
* Generated from fi.espoo.evaka.application.GuardianUpdate
*/
export interface GuardianUpdate {
  email: string
  futureAddress: FutureAddress | null
  phoneNumber: string
}

/**
* Generated from fi.espoo.evaka.application.notes.NoteJSON
*/
export interface NoteJSON {
  applicationId: UUID
  created: Date
  createdBy: UUID
  createdByName: string
  id: UUID
  text: string
  updated: Date
  updatedBy: UUID | null
  updatedByName: string | null
}

/**
* Generated from fi.espoo.evaka.application.notes.NoteRequest
*/
export interface NoteRequest {
  text: string
}

/**
* Generated from fi.espoo.evaka.application.OtherGuardianAgreementStatus
*/
export type OtherGuardianAgreementStatus = 
  | 'AGREED'
  | 'NOT_AGREED'
  | 'RIGHT_TO_GET_NOTIFIED'

/**
* Generated from fi.espoo.evaka.application.PaperApplicationCreateRequest
*/
export interface PaperApplicationCreateRequest {
  childId: UUID
  guardianId: UUID | null
  guardianSsn: string | null
  guardianToBeCreated: CreatePersonBody | null
  hideFromGuardian: boolean
  sentDate: LocalDate
  transferApplication: boolean
  type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.application.PersonApplicationSummary
*/
export interface PersonApplicationSummary {
  applicationId: UUID
  childId: UUID
  childName: string | null
  childSsn: string | null
  connectedDaycare: boolean
  guardianId: UUID
  guardianName: string
  preferredStartDate: LocalDate | null
  preferredUnitId: UUID | null
  preferredUnitName: string | null
  preparatoryEducation: boolean
  sentDate: LocalDate | null
  status: ApplicationStatus
  type: string
}

/**
* Generated from fi.espoo.evaka.application.PersonBasics
*/
export interface PersonBasics {
  firstName: string
  lastName: string
  socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.application.PlacementProposalConfirmationUpdate
*/
export interface PlacementProposalConfirmationUpdate {
  otherReason: string | null
  reason: PlacementPlanRejectReason | null
  status: PlacementPlanConfirmationStatus
}

/**
* Generated from fi.espoo.evaka.application.PlacementProposalStatus
*/
export interface PlacementProposalStatus {
  unitConfirmationStatus: PlacementPlanConfirmationStatus
  unitRejectOtherReason: string | null
  unitRejectReason: PlacementPlanRejectReason | null
}

/**
* Generated from fi.espoo.evaka.application.Preferences
*/
export interface Preferences {
  preferredStartDate: LocalDate | null
  preferredUnits: PreferredUnit[]
  preparatory: boolean
  serviceNeed: ServiceNeed | null
  siblingBasis: SiblingBasis | null
  urgent: boolean
}

/**
* Generated from fi.espoo.evaka.application.PreferredUnit
*/
export interface PreferredUnit {
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.application.RejectDecisionRequest
*/
export interface RejectDecisionRequest {
  decisionId: UUID
}

/**
* Generated from fi.espoo.evaka.application.SearchApplicationRequest
*/
export interface SearchApplicationRequest {
  area: string | null
  basis: string | null
  dateType: string | null
  distinctions: string | null
  page: number | null
  pageSize: number | null
  periodEnd: LocalDate | null
  periodStart: LocalDate | null
  preschoolType: string | null
  searchTerms: string | null
  sortBy: ApplicationSortColumn | null
  sortDir: ApplicationSortDirection | null
  status: string | null
  transferApplications: TransferApplicationFilter | null
  type: ApplicationTypeToggle
  units: string | null
  voucherApplications: VoucherApplicationFilter | null
}

/**
* Generated from fi.espoo.evaka.application.SecondGuardian
*/
export interface SecondGuardian {
  agreementStatus: OtherGuardianAgreementStatus | null
  email: string
  phoneNumber: string
}

/**
* Generated from fi.espoo.evaka.application.ServiceNeed
*/
export interface ServiceNeed {
  endTime: string
  partTime: boolean
  serviceNeedOption: ServiceNeedOption | null
  shiftCare: boolean
  startTime: string
}

/**
* Generated from fi.espoo.evaka.application.ServiceNeedOption
*/
export interface ServiceNeedOption {
  id: UUID
  name: string
}

/**
* Generated from fi.espoo.evaka.application.SiblingBasis
*/
export interface SiblingBasis {
  siblingName: string
  siblingSsn: string
}

/**
* Generated from fi.espoo.evaka.application.SimpleBatchRequest
*/
export interface SimpleBatchRequest {
  applicationIds: UUID[]
}

/**
* Generated from fi.espoo.evaka.application.TransferApplicationFilter
*/
export type TransferApplicationFilter = 
  | 'TRANSFER_ONLY'
  | 'NO_TRANSFER'
  | 'ALL'

/**
* Generated from fi.espoo.evaka.application.VoucherApplicationFilter
*/
export type VoucherApplicationFilter = 
  | 'VOUCHER_FIRST_CHOICE'
  | 'VOUCHER_ONLY'
  | 'NO_VOUCHER'
