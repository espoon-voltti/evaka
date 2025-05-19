// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { ApplicationId } from './shared'
import type { ApplicationNoteId } from './shared'
import type { AreaId } from './shared'
import type { AttachmentId } from './shared'
import type { CreatePersonBody } from './pis'
import type { DaycareId } from './shared'
import type { Decision } from './decision'
import type { DecisionDraft } from './decision'
import type { DecisionId } from './shared'
import type { DecisionStatus } from './decision'
import type { DecisionType } from './decision'
import type { DecisionUnit } from './decision'
import type { EmployeeId } from './shared'
import type { EvakaUser } from './user'
import type { EvakaUserId } from './shared'
import type { FinanceDecisionType } from './invoicing'
import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { MessageContentId } from './shared'
import type { MessageThreadId } from './shared'
import type { PersonId } from './shared'
import type { PersonJSON } from './pis'
import type { PlacementPlanConfirmationStatus } from './placement'
import type { PlacementPlanDetails } from './placement'
import type { PlacementPlanRejectReason } from './placement'
import type { PlacementType } from './placement'
import type { ServiceNeedOptionId } from './shared'
import type { UUID } from '../../types'
import { deserializeJsonCreatePersonBody } from './pis'
import { deserializeJsonDecision } from './decision'
import { deserializeJsonDecisionDraft } from './decision'
import { deserializeJsonPersonJSON } from './pis'
import { deserializeJsonPlacementPlanDetails } from './placement'

/**
* Generated from fi.espoo.evaka.application.AcceptDecisionRequest
*/
export interface AcceptDecisionRequest {
  decisionId: DecisionId
  requestedStartDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.application.AcceptPlacementProposalRequest
*/
export interface AcceptPlacementProposalRequest {
  rejectReasonTranslations: Partial<Record<PlacementPlanRejectReason, string>>
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
  id: AttachmentId
  name: string
  receivedAt: HelsinkiDateTime
  type: ApplicationAttachmentType
  updated: HelsinkiDateTime
  uploadedByEmployee: EmployeeId | null
  uploadedByPerson: PersonId | null
}

/**
* Generated from fi.espoo.evaka.application.ApplicationAttachmentType
*/
export type ApplicationAttachmentType =
  | 'URGENCY'
  | 'EXTENDED_CARE'
  | 'SERVICE_WORKER_ATTACHMENT'

/**
* Generated from fi.espoo.evaka.application.ApplicationBasis
*/
export type ApplicationBasis =
  | 'ADDITIONAL_INFO'
  | 'SIBLING_BASIS'
  | 'ASSISTANCE_NEED'
  | 'CLUB_CARE'
  | 'CONTINUATION'
  | 'DAYCARE'
  | 'EXTENDED_CARE'
  | 'DUPLICATE_APPLICATION'
  | 'URGENT'
  | 'HAS_ATTACHMENTS'

/**
* Generated from fi.espoo.evaka.application.ApplicationDateType
*/
export type ApplicationDateType =
  | 'DUE'
  | 'START'
  | 'ARRIVAL'

/**
* Generated from fi.espoo.evaka.application.ApplicationDecisions
*/
export interface ApplicationDecisions {
  decidableApplications: ApplicationId[]
  decisions: DecisionSummary[]
  permittedActions: Partial<Record<DecisionId, Action.Citizen.Decision[]>>
}

/**
* Generated from fi.espoo.evaka.application.ApplicationDetails
*/
export interface ApplicationDetails {
  additionalDaycareApplication: boolean
  allowOtherGuardianAccess: boolean
  attachments: ApplicationAttachment[]
  checkedByAdmin: boolean
  childId: PersonId
  childRestricted: boolean
  confidential: boolean | null
  createdAt: HelsinkiDateTime | null
  createdBy: EvakaUser | null
  dueDate: LocalDate | null
  dueDateSetManuallyAt: HelsinkiDateTime | null
  form: ApplicationForm
  guardianDateOfDeath: LocalDate | null
  guardianId: PersonId
  guardianRestricted: boolean
  hasOtherGuardian: boolean
  hideFromGuardian: boolean
  id: ApplicationId
  modifiedAt: HelsinkiDateTime | null
  modifiedBy: EvakaUser | null
  origin: ApplicationOrigin
  otherGuardianLivesInSameAddress: boolean | null
  sentDate: LocalDate | null
  status: ApplicationStatus
  transferApplication: boolean
  type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.application.ApplicationDistinctions
*/
export type ApplicationDistinctions =
  | 'SECONDARY'

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
* Generated from fi.espoo.evaka.application.ApplicationNote
*/
export interface ApplicationNote {
  applicationId: ApplicationId
  content: string
  created: HelsinkiDateTime
  createdBy: EvakaUserId
  createdByName: string
  id: ApplicationNoteId
  messageContentId: MessageContentId | null
  messageThreadId: MessageThreadId | null
  updated: HelsinkiDateTime
  updatedBy: EvakaUserId
  updatedByName: string
}

/**
* Generated from fi.espoo.evaka.application.notes.ApplicationNoteResponse
*/
export interface ApplicationNoteResponse {
  note: ApplicationNote
  permittedActions: Action.ApplicationNote[]
}

/**
* Generated from fi.espoo.evaka.application.ApplicationOrigin
*/
export type ApplicationOrigin =
  | 'ELECTRONIC'
  | 'PAPER'

/**
* Generated from fi.espoo.evaka.application.ApplicationPreschoolTypeToggle
*/
export type ApplicationPreschoolTypeToggle =
  | 'PRESCHOOL_ONLY'
  | 'PRESCHOOL_DAYCARE'
  | 'PRESCHOOL_CLUB'
  | 'PREPARATORY_ONLY'
  | 'PREPARATORY_DAYCARE'
  | 'DAYCARE_ONLY'

/**
* Generated from fi.espoo.evaka.application.ApplicationResponse
*/
export interface ApplicationResponse {
  application: ApplicationDetails
  attachments: ApplicationAttachment[]
  decisions: Decision[]
  guardians: PersonJSON[]
  permittedActions: Action.Application[]
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
  | 'UNIT_NAME'

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
* Generated from fi.espoo.evaka.application.ApplicationStatusOption
*/
export const applicationStatusOptions = [
  'SENT',
  'WAITING_PLACEMENT',
  'WAITING_UNIT_CONFIRMATION',
  'WAITING_DECISION',
  'WAITING_MAILING',
  'WAITING_CONFIRMATION',
  'REJECTED',
  'ACTIVE',
  'CANCELLED'
] as const

export type ApplicationStatusOption = typeof applicationStatusOptions[number]

/**
* Generated from fi.espoo.evaka.application.ApplicationSummary
*/
export interface ApplicationSummary {
  additionalDaycareApplication: boolean
  additionalInfo: boolean
  assistanceNeed: boolean
  attachmentCount: number
  checkedByAdmin: boolean
  confidential: boolean | null
  continuation: boolean
  currentPlacementUnit: PreferredUnit | null
  dateOfBirth: LocalDate | null
  dueDate: LocalDate | null
  duplicateApplication: boolean
  extendedCare: boolean
  firstName: string
  id: ApplicationId
  lastName: string
  origin: ApplicationOrigin
  placementPlanStartDate: LocalDate | null
  placementPlanUnitName: string | null
  placementProposalStatus: PlacementProposalStatus | null
  placementType: PlacementType
  postOffice: string | null
  postalCode: string | null
  preferredUnits: PreferredUnit[]
  serviceNeed: ServiceNeedOption | null
  serviceWorkerNote: string
  siblingBasis: boolean
  startDate: LocalDate | null
  status: ApplicationStatus
  streetAddress: string | null
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
* Generated from fi.espoo.evaka.application.ApplicationUnitSummary
*/
export interface ApplicationUnitSummary {
  applicationId: ApplicationId
  dateOfBirth: LocalDate
  extendedCare: boolean
  firstName: string
  guardianEmail: string | null
  guardianFirstName: string
  guardianLastName: string
  guardianPhone: string | null
  lastName: string
  preferenceOrder: number
  preferredStartDate: LocalDate
  requestedPlacementType: PlacementType
  serviceNeed: ServiceNeedOption | null
  status: ApplicationStatus
}

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
  childId: PersonId
  childName: string
  decidableApplications: ApplicationId[]
  duplicateOf: PersonId | null
  permittedActions: Partial<Record<ApplicationId, Action.Citizen.Application[]>>
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
  applicationId: ApplicationId
  applicationStatus: ApplicationStatus
  childId: PersonId
  childName: string | null
  createdDate: HelsinkiDateTime
  modifiedDate: HelsinkiDateTime
  preferredUnitName: string | null
  sentDate: LocalDate | null
  startDate: LocalDate | null
  transferApplication: boolean
  type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.application.CitizenApplicationUpdate
*/
export interface CitizenApplicationUpdate {
  allowOtherGuardianAccess: boolean
  form: ApplicationFormUpdate
}

/**
* Generated from fi.espoo.evaka.application.CitizenChildren
*/
export interface CitizenChildren {
  dateOfBirth: LocalDate
  duplicateOf: PersonId | null
  firstName: string
  id: PersonId
  lastName: string
  socialSecurityNumber: string | null
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
  childId: PersonId
  type: ApplicationType
}

/**
* Generated from fi.espoo.evaka.application.DaycarePlacementPlan
*/
export interface DaycarePlacementPlan {
  period: FiniteDateRange
  preschoolDaycarePeriod: FiniteDateRange | null
  unitId: DaycareId
}

/**
* Generated from fi.espoo.evaka.application.DecisionDraftGroup
*/
export interface DecisionDraftGroup {
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
  applicationId: ApplicationId
  childId: PersonId
  id: DecisionId
  resolved: LocalDate | null
  sentDate: LocalDate
  status: DecisionStatus
  type: DecisionType
}

/**
* Generated from fi.espoo.evaka.application.ApplicationControllerCitizen.DecisionWithValidStartDatePeriod
*/
export interface DecisionWithValidStartDatePeriod {
  canDecide: boolean
  decision: Decision
  permittedActions: Action.Citizen.Decision[]
  validRequestedStartDatePeriod: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.application.FinanceDecisionChildInfo
*/
export interface FinanceDecisionChildInfo {
  firstName: string
  id: PersonId
  lastName: string
}

/**
* Generated from fi.espoo.evaka.application.FinanceDecisionCitizenInfo
*/
export interface FinanceDecisionCitizenInfo {
  coDebtors: LiableCitizenInfo[]
  decisionChildren: FinanceDecisionChildInfo[]
  id: UUID
  sentAt: HelsinkiDateTime
  type: FinanceDecisionType
  validFrom: LocalDate
  validTo: LocalDate | null
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
  email: string | null
  futureAddress: FutureAddress | null
  person: PersonBasics
  phoneNumber: string
}

/**
* Generated from fi.espoo.evaka.application.GuardianInfo
*/
export interface GuardianInfo {
  firstName: string
  id: PersonId | null
  isVtjGuardian: boolean
  lastName: string
  ssn: string | null
}

/**
* Generated from fi.espoo.evaka.application.GuardianUpdate
*/
export interface GuardianUpdate {
  email: string | null
  futureAddress: FutureAddress | null
  phoneNumber: string
}

/**
* Generated from fi.espoo.evaka.application.LiableCitizenInfo
*/
export interface LiableCitizenInfo {
  firstName: string
  id: PersonId
  lastName: string
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
  | 'AUTOMATED'

/**
* Generated from fi.espoo.evaka.application.PagedApplicationSummaries
*/
export interface PagedApplicationSummaries {
  data: ApplicationSummary[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.application.PaperApplicationCreateRequest
*/
export interface PaperApplicationCreateRequest {
  childId: PersonId
  guardianId: PersonId | null
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
  applicationId: ApplicationId
  childId: PersonId
  childName: string | null
  childSsn: string | null
  connectedDaycare: boolean
  guardianId: PersonId
  guardianName: string
  preferredStartDate: LocalDate | null
  preferredUnitId: DaycareId | null
  preferredUnitName: string | null
  preparatoryEducation: boolean
  sentDate: LocalDate | null
  status: ApplicationStatus
  type: ApplicationType
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
  modifiedAt: HelsinkiDateTime | null
  modifiedBy: EvakaUser | null
  unitConfirmationStatus: PlacementPlanConfirmationStatus
  unitRejectOtherReason: string | null
  unitRejectReason: PlacementPlanRejectReason | null
}

/**
* Generated from fi.espoo.evaka.application.PlacementToolValidation
*/
export interface PlacementToolValidation {
  count: number
  existing: number
}

/**
* Generated from fi.espoo.evaka.application.Preferences
*/
export interface Preferences {
  connectedDaycarePreferredStartDate: LocalDate | null
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
  id: DaycareId
  name: string
}

/**
* Generated from fi.espoo.evaka.application.RejectDecisionRequest
*/
export interface RejectDecisionRequest {
  decisionId: DecisionId
}

/**
* Generated from fi.espoo.evaka.application.SearchApplicationRequest
*/
export interface SearchApplicationRequest {
  areas: AreaId[] | null
  basis: ApplicationBasis[] | null
  dateType: ApplicationDateType[] | null
  distinctions: ApplicationDistinctions[] | null
  page: number | null
  periodEnd: LocalDate | null
  periodStart: LocalDate | null
  preschoolType: ApplicationPreschoolTypeToggle[] | null
  searchTerms: string | null
  sortBy: ApplicationSortColumn | null
  sortDir: ApplicationSortDirection | null
  statuses: ApplicationStatusOption[] | null
  transferApplications: TransferApplicationFilter | null
  type: ApplicationTypeToggle
  units: DaycareId[] | null
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
  id: ServiceNeedOptionId
  nameEn: string
  nameFi: string
  nameSv: string
  validPlacementType: PlacementType | null
}

/**
* Generated from fi.espoo.evaka.application.SiblingBasis
*/
export interface SiblingBasis {
  siblingName: string
  siblingSsn: string
  siblingUnit: string
}

/**
* Generated from fi.espoo.evaka.application.SimpleApplicationAction
*/
export type SimpleApplicationAction =
  | 'MOVE_TO_WAITING_PLACEMENT'
  | 'RETURN_TO_SENT'
  | 'CANCEL_PLACEMENT_PLAN'
  | 'SEND_DECISIONS_WITHOUT_PROPOSAL'
  | 'SEND_PLACEMENT_PROPOSAL'
  | 'WITHDRAW_PLACEMENT_PROPOSAL'
  | 'CONFIRM_DECISION_MAILED'

/**
* Generated from fi.espoo.evaka.application.SimpleBatchRequest
*/
export interface SimpleBatchRequest {
  applicationIds: ApplicationId[]
}

/**
* Generated from fi.espoo.evaka.application.TransferApplicationFilter
*/
export type TransferApplicationFilter =
  | 'TRANSFER_ONLY'
  | 'NO_TRANSFER'
  | 'ALL'

/**
* Generated from fi.espoo.evaka.application.TransferApplicationUnitSummary
*/
export interface TransferApplicationUnitSummary {
  applicationId: ApplicationId
  dateOfBirth: LocalDate
  firstName: string
  lastName: string
  preferredStartDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.application.UnitApplications
*/
export interface UnitApplications {
  applications: ApplicationUnitSummary[]
  placementPlans: PlacementPlanDetails[]
  placementProposals: PlacementPlanDetails[]
  transferApplications: TransferApplicationUnitSummary[] | null
}

/**
* Generated from fi.espoo.evaka.application.VoucherApplicationFilter
*/
export type VoucherApplicationFilter =
  | 'VOUCHER_FIRST_CHOICE'
  | 'VOUCHER_ONLY'
  | 'NO_VOUCHER'


export function deserializeJsonAcceptDecisionRequest(json: JsonOf<AcceptDecisionRequest>): AcceptDecisionRequest {
  return {
    ...json,
    requestedStartDate: LocalDate.parseIso(json.requestedStartDate)
  }
}


export function deserializeJsonApplicationAttachment(json: JsonOf<ApplicationAttachment>): ApplicationAttachment {
  return {
    ...json,
    receivedAt: HelsinkiDateTime.parseIso(json.receivedAt),
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}


export function deserializeJsonApplicationDecisions(json: JsonOf<ApplicationDecisions>): ApplicationDecisions {
  return {
    ...json,
    decisions: json.decisions.map(e => deserializeJsonDecisionSummary(e))
  }
}


export function deserializeJsonApplicationDetails(json: JsonOf<ApplicationDetails>): ApplicationDetails {
  return {
    ...json,
    attachments: json.attachments.map(e => deserializeJsonApplicationAttachment(e)),
    createdAt: (json.createdAt != null) ? HelsinkiDateTime.parseIso(json.createdAt) : null,
    dueDate: (json.dueDate != null) ? LocalDate.parseIso(json.dueDate) : null,
    dueDateSetManuallyAt: (json.dueDateSetManuallyAt != null) ? HelsinkiDateTime.parseIso(json.dueDateSetManuallyAt) : null,
    form: deserializeJsonApplicationForm(json.form),
    guardianDateOfDeath: (json.guardianDateOfDeath != null) ? LocalDate.parseIso(json.guardianDateOfDeath) : null,
    modifiedAt: (json.modifiedAt != null) ? HelsinkiDateTime.parseIso(json.modifiedAt) : null,
    sentDate: (json.sentDate != null) ? LocalDate.parseIso(json.sentDate) : null
  }
}


export function deserializeJsonApplicationForm(json: JsonOf<ApplicationForm>): ApplicationForm {
  return {
    ...json,
    child: deserializeJsonChildDetails(json.child),
    guardian: deserializeJsonGuardian(json.guardian),
    preferences: deserializeJsonPreferences(json.preferences)
  }
}


export function deserializeJsonApplicationFormUpdate(json: JsonOf<ApplicationFormUpdate>): ApplicationFormUpdate {
  return {
    ...json,
    child: deserializeJsonChildDetailsUpdate(json.child),
    guardian: deserializeJsonGuardianUpdate(json.guardian),
    preferences: deserializeJsonPreferences(json.preferences)
  }
}


export function deserializeJsonApplicationNote(json: JsonOf<ApplicationNote>): ApplicationNote {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    updated: HelsinkiDateTime.parseIso(json.updated)
  }
}


export function deserializeJsonApplicationNoteResponse(json: JsonOf<ApplicationNoteResponse>): ApplicationNoteResponse {
  return {
    ...json,
    note: deserializeJsonApplicationNote(json.note)
  }
}


export function deserializeJsonApplicationResponse(json: JsonOf<ApplicationResponse>): ApplicationResponse {
  return {
    ...json,
    application: deserializeJsonApplicationDetails(json.application),
    attachments: json.attachments.map(e => deserializeJsonApplicationAttachment(e)),
    decisions: json.decisions.map(e => deserializeJsonDecision(e)),
    guardians: json.guardians.map(e => deserializeJsonPersonJSON(e))
  }
}


export function deserializeJsonApplicationSummary(json: JsonOf<ApplicationSummary>): ApplicationSummary {
  return {
    ...json,
    dateOfBirth: (json.dateOfBirth != null) ? LocalDate.parseIso(json.dateOfBirth) : null,
    dueDate: (json.dueDate != null) ? LocalDate.parseIso(json.dueDate) : null,
    placementPlanStartDate: (json.placementPlanStartDate != null) ? LocalDate.parseIso(json.placementPlanStartDate) : null,
    placementProposalStatus: (json.placementProposalStatus != null) ? deserializeJsonPlacementProposalStatus(json.placementProposalStatus) : null,
    startDate: (json.startDate != null) ? LocalDate.parseIso(json.startDate) : null
  }
}


export function deserializeJsonApplicationUnitSummary(json: JsonOf<ApplicationUnitSummary>): ApplicationUnitSummary {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    preferredStartDate: LocalDate.parseIso(json.preferredStartDate)
  }
}


export function deserializeJsonApplicationUpdate(json: JsonOf<ApplicationUpdate>): ApplicationUpdate {
  return {
    ...json,
    dueDate: (json.dueDate != null) ? LocalDate.parseIso(json.dueDate) : null,
    form: deserializeJsonApplicationFormUpdate(json.form)
  }
}


export function deserializeJsonApplicationsOfChild(json: JsonOf<ApplicationsOfChild>): ApplicationsOfChild {
  return {
    ...json,
    applicationSummaries: json.applicationSummaries.map(e => deserializeJsonCitizenApplicationSummary(e))
  }
}


export function deserializeJsonChildDetails(json: JsonOf<ChildDetails>): ChildDetails {
  return {
    ...json,
    dateOfBirth: (json.dateOfBirth != null) ? LocalDate.parseIso(json.dateOfBirth) : null,
    futureAddress: (json.futureAddress != null) ? deserializeJsonFutureAddress(json.futureAddress) : null
  }
}


export function deserializeJsonChildDetailsUpdate(json: JsonOf<ChildDetailsUpdate>): ChildDetailsUpdate {
  return {
    ...json,
    futureAddress: (json.futureAddress != null) ? deserializeJsonFutureAddress(json.futureAddress) : null
  }
}


export function deserializeJsonCitizenApplicationSummary(json: JsonOf<CitizenApplicationSummary>): CitizenApplicationSummary {
  return {
    ...json,
    createdDate: HelsinkiDateTime.parseIso(json.createdDate),
    modifiedDate: HelsinkiDateTime.parseIso(json.modifiedDate),
    sentDate: (json.sentDate != null) ? LocalDate.parseIso(json.sentDate) : null,
    startDate: (json.startDate != null) ? LocalDate.parseIso(json.startDate) : null
  }
}


export function deserializeJsonCitizenApplicationUpdate(json: JsonOf<CitizenApplicationUpdate>): CitizenApplicationUpdate {
  return {
    ...json,
    form: deserializeJsonApplicationFormUpdate(json.form)
  }
}


export function deserializeJsonCitizenChildren(json: JsonOf<CitizenChildren>): CitizenChildren {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}


export function deserializeJsonDaycarePlacementPlan(json: JsonOf<DaycarePlacementPlan>): DaycarePlacementPlan {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period),
    preschoolDaycarePeriod: (json.preschoolDaycarePeriod != null) ? FiniteDateRange.parseJson(json.preschoolDaycarePeriod) : null
  }
}


export function deserializeJsonDecisionDraftGroup(json: JsonOf<DecisionDraftGroup>): DecisionDraftGroup {
  return {
    ...json,
    decisions: json.decisions.map(e => deserializeJsonDecisionDraft(e))
  }
}


export function deserializeJsonDecisionSummary(json: JsonOf<DecisionSummary>): DecisionSummary {
  return {
    ...json,
    resolved: (json.resolved != null) ? LocalDate.parseIso(json.resolved) : null,
    sentDate: LocalDate.parseIso(json.sentDate)
  }
}


export function deserializeJsonDecisionWithValidStartDatePeriod(json: JsonOf<DecisionWithValidStartDatePeriod>): DecisionWithValidStartDatePeriod {
  return {
    ...json,
    decision: deserializeJsonDecision(json.decision),
    validRequestedStartDatePeriod: FiniteDateRange.parseJson(json.validRequestedStartDatePeriod)
  }
}


export function deserializeJsonFinanceDecisionCitizenInfo(json: JsonOf<FinanceDecisionCitizenInfo>): FinanceDecisionCitizenInfo {
  return {
    ...json,
    sentAt: HelsinkiDateTime.parseIso(json.sentAt),
    validFrom: LocalDate.parseIso(json.validFrom),
    validTo: (json.validTo != null) ? LocalDate.parseIso(json.validTo) : null
  }
}


export function deserializeJsonFutureAddress(json: JsonOf<FutureAddress>): FutureAddress {
  return {
    ...json,
    movingDate: (json.movingDate != null) ? LocalDate.parseIso(json.movingDate) : null
  }
}


export function deserializeJsonGuardian(json: JsonOf<Guardian>): Guardian {
  return {
    ...json,
    futureAddress: (json.futureAddress != null) ? deserializeJsonFutureAddress(json.futureAddress) : null
  }
}


export function deserializeJsonGuardianUpdate(json: JsonOf<GuardianUpdate>): GuardianUpdate {
  return {
    ...json,
    futureAddress: (json.futureAddress != null) ? deserializeJsonFutureAddress(json.futureAddress) : null
  }
}


export function deserializeJsonPagedApplicationSummaries(json: JsonOf<PagedApplicationSummaries>): PagedApplicationSummaries {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonApplicationSummary(e))
  }
}


export function deserializeJsonPaperApplicationCreateRequest(json: JsonOf<PaperApplicationCreateRequest>): PaperApplicationCreateRequest {
  return {
    ...json,
    guardianToBeCreated: (json.guardianToBeCreated != null) ? deserializeJsonCreatePersonBody(json.guardianToBeCreated) : null,
    sentDate: LocalDate.parseIso(json.sentDate)
  }
}


export function deserializeJsonPersonApplicationSummary(json: JsonOf<PersonApplicationSummary>): PersonApplicationSummary {
  return {
    ...json,
    preferredStartDate: (json.preferredStartDate != null) ? LocalDate.parseIso(json.preferredStartDate) : null,
    sentDate: (json.sentDate != null) ? LocalDate.parseIso(json.sentDate) : null
  }
}


export function deserializeJsonPlacementProposalStatus(json: JsonOf<PlacementProposalStatus>): PlacementProposalStatus {
  return {
    ...json,
    modifiedAt: (json.modifiedAt != null) ? HelsinkiDateTime.parseIso(json.modifiedAt) : null
  }
}


export function deserializeJsonPreferences(json: JsonOf<Preferences>): Preferences {
  return {
    ...json,
    connectedDaycarePreferredStartDate: (json.connectedDaycarePreferredStartDate != null) ? LocalDate.parseIso(json.connectedDaycarePreferredStartDate) : null,
    preferredStartDate: (json.preferredStartDate != null) ? LocalDate.parseIso(json.preferredStartDate) : null
  }
}


export function deserializeJsonSearchApplicationRequest(json: JsonOf<SearchApplicationRequest>): SearchApplicationRequest {
  return {
    ...json,
    periodEnd: (json.periodEnd != null) ? LocalDate.parseIso(json.periodEnd) : null,
    periodStart: (json.periodStart != null) ? LocalDate.parseIso(json.periodStart) : null
  }
}


export function deserializeJsonTransferApplicationUnitSummary(json: JsonOf<TransferApplicationUnitSummary>): TransferApplicationUnitSummary {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    preferredStartDate: LocalDate.parseIso(json.preferredStartDate)
  }
}


export function deserializeJsonUnitApplications(json: JsonOf<UnitApplications>): UnitApplications {
  return {
    ...json,
    applications: json.applications.map(e => deserializeJsonApplicationUnitSummary(e)),
    placementPlans: json.placementPlans.map(e => deserializeJsonPlacementPlanDetails(e)),
    placementProposals: json.placementProposals.map(e => deserializeJsonPlacementPlanDetails(e)),
    transferApplications: (json.transferApplications != null) ? json.transferApplications.map(e => deserializeJsonTransferApplicationUnitSummary(e)) : null
  }
}
