// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import DateRange from '../../date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import LocalDate from '../../local-date'
import { Action } from '../action'
import { ApplicationId } from './shared'
import { ApplicationType } from './application'
import { CitizenAuthLevel } from './shared'
import { CitizenFeatures } from './shared'
import { DaycareId } from './shared'
import { EmployeeFeatures } from './shared'
import { EmployeeId } from './shared'
import { EvakaUser } from './user'
import { EvakaUserId } from './shared'
import { FosterParentId } from './shared'
import { GroupId } from './shared'
import { IncomeEffect } from './invoicing'
import { JsonOf } from '../../json'
import { MobileDevice } from './pairing'
import { Nationality } from './vtjclient'
import { NativeLanguage } from './vtjclient'
import { ParentshipId } from './shared'
import { PartnershipId } from './shared'
import { PersonEmailVerificationId } from './shared'
import { PersonId } from './shared'
import { UserRole } from './shared'

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.AddSsnRequest
*/
export interface AddSsnRequest {
  ssn: string
}

/**
* Generated from fi.espoo.evaka.pis.CitizenUserDetails
*/
export interface CitizenUserDetails {
  backupPhone: string
  email: string | null
  firstName: string
  id: PersonId
  keycloakEmail: string | null
  lastName: string
  phone: string
  postOffice: string
  postalCode: string
  preferredName: string
  streetAddress: string
  weakLoginUsername: string | null
}

/**
* Generated from fi.espoo.evaka.pis.SystemController.CitizenUserResponse
*/
export interface CitizenUserResponse {
  accessibleFeatures: CitizenFeatures
  authLevel: CitizenAuthLevel
  details: CitizenUserDetails
}

/**
* Generated from fi.espoo.evaka.pis.controllers.CreateFosterParentRelationshipBody
*/
export interface CreateFosterParentRelationshipBody {
  childId: PersonId
  parentId: PersonId
  validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.pis.controllers.CreatePersonBody
*/
export interface CreatePersonBody {
  dateOfBirth: LocalDate
  email: string | null
  firstName: string
  lastName: string
  phone: string
  postOffice: string
  postalCode: string
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.CreateSource
*/
export type CreateSource =
  | 'USER'
  | 'APPLICATION'
  | 'DVV'

/**
* Generated from fi.espoo.evaka.pis.CreationModificationMetadata
*/
export interface CreationModificationMetadata {
  createSource: CreateSource | null
  createdAt: HelsinkiDateTime | null
  createdBy: EvakaUserId | null
  createdByName: string | null
  createdFromApplication: ApplicationId | null
  createdFromApplicationCreated: HelsinkiDateTime | null
  createdFromApplicationType: ApplicationType | null
  modifiedAt: HelsinkiDateTime | null
  modifiedBy: EvakaUserId | null
  modifiedByName: string | null
  modifySource: ModifySource | null
}

/**
* Generated from fi.espoo.evaka.pis.DaycareGroupRole
*/
export interface DaycareGroupRole {
  daycareId: DaycareId
  daycareName: string
  groupId: GroupId
  groupName: string
}

/**
* Generated from fi.espoo.evaka.pis.DaycareRole
*/
export interface DaycareRole {
  daycareId: DaycareId
  daycareName: string
  role: UserRole
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.DisableSsnRequest
*/
export interface DisableSsnRequest {
  disabled: boolean
}

/**
* Generated from fi.espoo.evaka.pis.EmailMessageType
*/
export const emailMessageTypes = [
  'TRANSACTIONAL',
  'MESSAGE_NOTIFICATION',
  'BULLETIN_NOTIFICATION',
  'INCOME_NOTIFICATION',
  'CALENDAR_EVENT_NOTIFICATION',
  'DECISION_NOTIFICATION',
  'DOCUMENT_NOTIFICATION',
  'INFORMAL_DOCUMENT_NOTIFICATION',
  'ATTENDANCE_RESERVATION_NOTIFICATION',
  'DISCUSSION_TIME_NOTIFICATION'
] as const

export type EmailMessageType = typeof emailMessageTypes[number]

/**
* Generated from fi.espoo.evaka.pis.EmailVerification
*/
export interface EmailVerification {
  email: string
  expiresAt: HelsinkiDateTime
  id: PersonEmailVerificationId
  sentAt: HelsinkiDateTime | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.EmailVerificationRequest
*/
export interface EmailVerificationRequest {
  code: string
  id: PersonEmailVerificationId
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.EmailVerificationStatusResponse
*/
export interface EmailVerificationStatusResponse {
  email: string | null
  latestVerification: EmailVerification | null
  verifiedEmail: string | null
}

/**
* Generated from fi.espoo.evaka.pis.Employee
*/
export interface Employee {
  active: boolean
  created: HelsinkiDateTime
  email: string | null
  externalId: string | null
  firstName: string
  id: EmployeeId
  lastName: string
  preferredFirstName: string | null
  temporaryInUnitId: DaycareId | null
  updated: HelsinkiDateTime | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.EmployeePreferredFirstName
*/
export interface EmployeePreferredFirstName {
  preferredFirstName: string | null
  preferredFirstNameOptions: string[]
}

/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.EmployeeSetPreferredFirstNameUpdateRequest
*/
export interface EmployeeSetPreferredFirstNameUpdateRequest {
  preferredFirstName: string | null
}

/**
* Generated from fi.espoo.evaka.pis.SystemController.EmployeeUserResponse
*/
export interface EmployeeUserResponse {
  accessibleFeatures: EmployeeFeatures
  allScopedRoles: UserRole[]
  firstName: string
  globalRoles: UserRole[]
  id: EmployeeId
  lastName: string
  permittedGlobalActions: Action.Global[]
}

/**
* Generated from fi.espoo.evaka.pis.EmployeeWithDaycareRoles
*/
export interface EmployeeWithDaycareRoles {
  active: boolean
  created: HelsinkiDateTime
  daycareGroupRoles: DaycareGroupRole[]
  daycareRoles: DaycareRole[]
  email: string | null
  employeeNumber: string | null
  externalId: string | null
  firstName: string
  globalRoles: UserRole[]
  id: EmployeeId
  lastLogin: HelsinkiDateTime | null
  lastName: string
  personalMobileDevices: MobileDevice[]
  temporaryUnitName: string | null
  updated: HelsinkiDateTime | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.EvakaRightsRequest
*/
export interface EvakaRightsRequest {
  denied: boolean
  guardianId: PersonId
}

/**
* Generated from fi.espoo.evaka.pis.FamilyContact
*/
export interface FamilyContact {
  backupPhone: string
  email: string | null
  firstName: string
  id: PersonId
  lastName: string
  phone: string
  postOffice: string
  postalCode: string
  priority: number | null
  role: FamilyContactRole
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.FamilyContactPriorityUpdate
*/
export interface FamilyContactPriorityUpdate {
  childId: PersonId
  contactPersonId: PersonId
  priority: number | null
}

/**
* Generated from fi.espoo.evaka.pis.FamilyContactRole
*/
export type FamilyContactRole =
  | 'LOCAL_GUARDIAN'
  | 'LOCAL_FOSTER_PARENT'
  | 'LOCAL_ADULT'
  | 'LOCAL_SIBLING'
  | 'REMOTE_GUARDIAN'
  | 'REMOTE_FOSTER_PARENT'

/**
* Generated from fi.espoo.evaka.pis.controllers.FamilyContactUpdate
*/
export interface FamilyContactUpdate {
  backupPhone: string | null
  childId: PersonId
  contactPersonId: PersonId
  email: string | null
  phone: string | null
}

/**
* Generated from fi.espoo.evaka.pis.service.FamilyOverview
*/
export interface FamilyOverview {
  children: FamilyOverviewPerson[]
  headOfFamily: FamilyOverviewPerson
  partner: FamilyOverviewPerson | null
  totalIncome: FamilyOverviewIncome | null
}

/**
* Generated from fi.espoo.evaka.pis.service.FamilyOverviewIncome
*/
export interface FamilyOverviewIncome {
  effect: IncomeEffect | null
  total: number | null
}

/**
* Generated from fi.espoo.evaka.pis.service.FamilyOverviewPerson
*/
export interface FamilyOverviewPerson {
  dateOfBirth: LocalDate
  firstName: string
  headOfChild: PersonId | null
  income: FamilyOverviewIncome | null
  lastName: string
  personId: PersonId
  postOffice: string
  postalCode: string
  restrictedDetailsEnabled: boolean
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.FosterParentRelationship
*/
export interface FosterParentRelationship {
  child: PersonSummary
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
  parent: PersonSummary
  relationshipId: FosterParentId
  validDuring: DateRange
}

/**
* Generated from fi.espoo.evaka.pis.controllers.GetOrCreatePersonBySsnRequest
*/
export interface GetOrCreatePersonBySsnRequest {
  readonly: boolean
  ssn: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.GuardiansResponse
*/
export interface GuardiansResponse {
  blockedGuardians: PersonJSON[] | null
  guardians: PersonJSON[]
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.MergeRequest
*/
export interface MergeRequest {
  duplicate: PersonId
  master: PersonId
}

/**
* Generated from fi.espoo.evaka.pis.ModifySource
*/
export type ModifySource =
  | 'USER'
  | 'DVV'

/**
* Generated from fi.espoo.evaka.pis.NewEmployee
*/
export interface NewEmployee {
  active: boolean
  email: string | null
  employeeNumber: string | null
  externalId: string | null
  firstName: string
  lastName: string
  roles: UserRole[]
  temporaryInUnitId: DaycareId | null
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonAddressDTO.Origin
*/
export type Origin =
  | 'VTJ'
  | 'MUNICIPAL'
  | 'EVAKA'

/**
* Generated from fi.espoo.evaka.pis.PagedEmployeesWithDaycareRoles
*/
export interface PagedEmployeesWithDaycareRoles {
  data: EmployeeWithDaycareRoles[]
  pages: number
  total: number
}

/**
* Generated from fi.espoo.evaka.pis.service.Parentship
*/
export interface Parentship {
  child: PersonJSON
  childId: PersonId
  conflict: boolean
  endDate: LocalDate
  headOfChild: PersonJSON
  headOfChildId: PersonId
  id: ParentshipId
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.service.ParentshipDetailed
*/
export interface ParentshipDetailed {
  child: PersonJSON
  childId: PersonId
  conflict: boolean
  creationModificationMetadata: CreationModificationMetadata
  endDate: LocalDate
  headOfChild: PersonJSON
  headOfChildId: PersonId
  id: ParentshipId
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.ParentshipRequest
*/
export interface ParentshipRequest {
  childId: PersonId
  endDate: LocalDate
  headOfChildId: PersonId
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.ParentshipUpdateRequest
*/
export interface ParentshipUpdateRequest {
  endDate: LocalDate
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.ParentshipWithPermittedActions
*/
export interface ParentshipWithPermittedActions {
  data: ParentshipDetailed
  permittedActions: Action.Parentship[]
}

/**
* Generated from fi.espoo.evaka.pis.service.Partnership
*/
export interface Partnership {
  conflict: boolean
  creationModificationMetadata: CreationModificationMetadata
  endDate: LocalDate | null
  id: PartnershipId
  partners: PersonJSON[]
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.PartnershipRequest
*/
export interface PartnershipRequest {
  endDate: LocalDate | null
  person1Id: PersonId
  person2Id: PersonId
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.PartnershipUpdateRequest
*/
export interface PartnershipUpdateRequest {
  endDate: LocalDate | null
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.PartnershipWithPermittedActions
*/
export interface PartnershipWithPermittedActions {
  data: Partnership
  permittedActions: Action.Partnership[]
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonAddressDTO
*/
export interface PersonAddressDTO {
  origin: Origin
  postOffice: string
  postalCode: string
  residenceCode: string
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.PersonIdentityResponseJSON
*/
export interface PersonIdentityResponseJSON {
  id: PersonId
  socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonJSON
*/
export interface PersonJSON {
  backupPhone: string
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  duplicateOf: PersonId | null
  email: string | null
  firstName: string
  forceManualFeeDecisions: boolean
  id: PersonId
  invoiceRecipientName: string
  invoicingPostOffice: string
  invoicingPostalCode: string
  invoicingStreetAddress: string
  language: string | null
  lastName: string
  municipalityOfResidence: string
  ophPersonOid: string | null
  phone: string
  postOffice: string
  postalCode: string
  residenceCode: string
  restrictedDetailsEnabled: boolean
  socialSecurityNumber: string | null
  ssnAddingDisabled: boolean
  streetAddress: string
  updatedFromVtj: HelsinkiDateTime | null
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonPatch
*/
export interface PersonPatch {
  backupPhone: string | null
  dateOfBirth: LocalDate | null
  email: string | null
  firstName: string | null
  forceManualFeeDecisions: boolean | null
  invoiceRecipientName: string | null
  invoicingPostOffice: string | null
  invoicingPostalCode: string | null
  invoicingStreetAddress: string | null
  lastName: string | null
  municipalityOfResidence: string | null
  ophPersonOid: string | null
  phone: string | null
  postOffice: string | null
  postalCode: string | null
  streetAddress: string | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.PersonResponse
*/
export interface PersonResponse {
  permittedActions: Action.Person[]
  person: PersonJSON
}

/**
* Generated from fi.espoo.evaka.pis.PersonSummary
*/
export interface PersonSummary {
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  firstName: string
  id: PersonId
  lastName: string
  restrictedDetailsEnabled: boolean
  socialSecurityNumber: string | null
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonWithChildrenDTO
*/
export interface PersonWithChildrenDTO {
  address: PersonAddressDTO
  backupPhone: string
  children: PersonWithChildrenDTO[]
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  duplicateOf: PersonId | null
  email: string | null
  firstName: string
  id: PersonId
  lastName: string
  municipalityOfResidence: string
  nationalities: Nationality[]
  nativeLanguage: NativeLanguage | null
  phone: string
  preferredName: string
  residenceCode: string
  restrictedDetails: RestrictedDetails
  socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.pis.PersonalDataUpdate
*/
export interface PersonalDataUpdate {
  backupPhone: string
  email: string
  phone: string
  preferredName: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PinCode
*/
export interface PinCode {
  pin: string
}

/**
* Generated from fi.espoo.evaka.pis.SystemController.PinLoginEmployee
*/
export interface PinLoginEmployee {
  firstName: string
  lastName: string
}

/**
* Generated from fi.espoo.evaka.pis.SystemController.PinLoginResponse
*/
export interface PinLoginResponse {
  employee: PinLoginEmployee | null
  status: PinLoginStatus
}

/**
* Generated from fi.espoo.evaka.pis.SystemController.PinLoginStatus
*/
export type PinLoginStatus =
  | 'SUCCESS'
  | 'WRONG_PIN'
  | 'PIN_LOCKED'

/**
* Generated from fi.espoo.evaka.pis.service.RestrictedDetails
*/
export interface RestrictedDetails {
  enabled: boolean
  endDate: LocalDate | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.SearchEmployeeRequest
*/
export interface SearchEmployeeRequest {
  globalRoles: UserRole[] | null
  hideDeactivated: boolean | null
  page: number | null
  searchTerm: string | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.SearchPersonBody
*/
export interface SearchPersonBody {
  orderBy: string
  searchTerm: string
  sortDirection: string
}

/**
* Generated from fi.espoo.evaka.pis.TemporaryEmployee
*/
export interface TemporaryEmployee {
  firstName: string
  groupIds: GroupId[]
  hasStaffOccupancyEffect: boolean
  lastName: string
  pinCode: PinCode | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen.UpdateWeakLoginCredentialsRequest
*/
export interface UpdateWeakLoginCredentialsRequest {
  password: string | null
  username: string | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.UpsertEmployeeDaycareRolesRequest
*/
export interface UpsertEmployeeDaycareRolesRequest {
  daycareIds: DaycareId[]
  role: UserRole
}


export function deserializeJsonCreateFosterParentRelationshipBody(json: JsonOf<CreateFosterParentRelationshipBody>): CreateFosterParentRelationshipBody {
  return {
    ...json,
    validDuring: DateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonCreatePersonBody(json: JsonOf<CreatePersonBody>): CreatePersonBody {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}


export function deserializeJsonCreationModificationMetadata(json: JsonOf<CreationModificationMetadata>): CreationModificationMetadata {
  return {
    ...json,
    createdAt: (json.createdAt != null) ? HelsinkiDateTime.parseIso(json.createdAt) : null,
    createdFromApplicationCreated: (json.createdFromApplicationCreated != null) ? HelsinkiDateTime.parseIso(json.createdFromApplicationCreated) : null,
    modifiedAt: (json.modifiedAt != null) ? HelsinkiDateTime.parseIso(json.modifiedAt) : null
  }
}


export function deserializeJsonEmailVerification(json: JsonOf<EmailVerification>): EmailVerification {
  return {
    ...json,
    expiresAt: HelsinkiDateTime.parseIso(json.expiresAt),
    sentAt: (json.sentAt != null) ? HelsinkiDateTime.parseIso(json.sentAt) : null
  }
}


export function deserializeJsonEmailVerificationStatusResponse(json: JsonOf<EmailVerificationStatusResponse>): EmailVerificationStatusResponse {
  return {
    ...json,
    latestVerification: (json.latestVerification != null) ? deserializeJsonEmailVerification(json.latestVerification) : null
  }
}


export function deserializeJsonEmployee(json: JsonOf<Employee>): Employee {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    updated: (json.updated != null) ? HelsinkiDateTime.parseIso(json.updated) : null
  }
}


export function deserializeJsonEmployeeWithDaycareRoles(json: JsonOf<EmployeeWithDaycareRoles>): EmployeeWithDaycareRoles {
  return {
    ...json,
    created: HelsinkiDateTime.parseIso(json.created),
    lastLogin: (json.lastLogin != null) ? HelsinkiDateTime.parseIso(json.lastLogin) : null,
    updated: (json.updated != null) ? HelsinkiDateTime.parseIso(json.updated) : null
  }
}


export function deserializeJsonFamilyOverview(json: JsonOf<FamilyOverview>): FamilyOverview {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonFamilyOverviewPerson(e)),
    headOfFamily: deserializeJsonFamilyOverviewPerson(json.headOfFamily),
    partner: (json.partner != null) ? deserializeJsonFamilyOverviewPerson(json.partner) : null
  }
}


export function deserializeJsonFamilyOverviewPerson(json: JsonOf<FamilyOverviewPerson>): FamilyOverviewPerson {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
  }
}


export function deserializeJsonFosterParentRelationship(json: JsonOf<FosterParentRelationship>): FosterParentRelationship {
  return {
    ...json,
    child: deserializeJsonPersonSummary(json.child),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    parent: deserializeJsonPersonSummary(json.parent),
    validDuring: DateRange.parseJson(json.validDuring)
  }
}


export function deserializeJsonGuardiansResponse(json: JsonOf<GuardiansResponse>): GuardiansResponse {
  return {
    ...json,
    blockedGuardians: (json.blockedGuardians != null) ? json.blockedGuardians.map(e => deserializeJsonPersonJSON(e)) : null,
    guardians: json.guardians.map(e => deserializeJsonPersonJSON(e))
  }
}


export function deserializeJsonPagedEmployeesWithDaycareRoles(json: JsonOf<PagedEmployeesWithDaycareRoles>): PagedEmployeesWithDaycareRoles {
  return {
    ...json,
    data: json.data.map(e => deserializeJsonEmployeeWithDaycareRoles(e))
  }
}


export function deserializeJsonParentship(json: JsonOf<Parentship>): Parentship {
  return {
    ...json,
    child: deserializeJsonPersonJSON(json.child),
    endDate: LocalDate.parseIso(json.endDate),
    headOfChild: deserializeJsonPersonJSON(json.headOfChild),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonParentshipDetailed(json: JsonOf<ParentshipDetailed>): ParentshipDetailed {
  return {
    ...json,
    child: deserializeJsonPersonJSON(json.child),
    creationModificationMetadata: deserializeJsonCreationModificationMetadata(json.creationModificationMetadata),
    endDate: LocalDate.parseIso(json.endDate),
    headOfChild: deserializeJsonPersonJSON(json.headOfChild),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonParentshipRequest(json: JsonOf<ParentshipRequest>): ParentshipRequest {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonParentshipUpdateRequest(json: JsonOf<ParentshipUpdateRequest>): ParentshipUpdateRequest {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonParentshipWithPermittedActions(json: JsonOf<ParentshipWithPermittedActions>): ParentshipWithPermittedActions {
  return {
    ...json,
    data: deserializeJsonParentshipDetailed(json.data)
  }
}


export function deserializeJsonPartnership(json: JsonOf<Partnership>): Partnership {
  return {
    ...json,
    creationModificationMetadata: deserializeJsonCreationModificationMetadata(json.creationModificationMetadata),
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    partners: json.partners.map(e => deserializeJsonPersonJSON(e)),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonPartnershipRequest(json: JsonOf<PartnershipRequest>): PartnershipRequest {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonPartnershipUpdateRequest(json: JsonOf<PartnershipUpdateRequest>): PartnershipUpdateRequest {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonPartnershipWithPermittedActions(json: JsonOf<PartnershipWithPermittedActions>): PartnershipWithPermittedActions {
  return {
    ...json,
    data: deserializeJsonPartnership(json.data)
  }
}


export function deserializeJsonPersonJSON(json: JsonOf<PersonJSON>): PersonJSON {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    dateOfDeath: (json.dateOfDeath != null) ? LocalDate.parseIso(json.dateOfDeath) : null,
    updatedFromVtj: (json.updatedFromVtj != null) ? HelsinkiDateTime.parseIso(json.updatedFromVtj) : null
  }
}


export function deserializeJsonPersonPatch(json: JsonOf<PersonPatch>): PersonPatch {
  return {
    ...json,
    dateOfBirth: (json.dateOfBirth != null) ? LocalDate.parseIso(json.dateOfBirth) : null
  }
}


export function deserializeJsonPersonResponse(json: JsonOf<PersonResponse>): PersonResponse {
  return {
    ...json,
    person: deserializeJsonPersonJSON(json.person)
  }
}


export function deserializeJsonPersonSummary(json: JsonOf<PersonSummary>): PersonSummary {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    dateOfDeath: (json.dateOfDeath != null) ? LocalDate.parseIso(json.dateOfDeath) : null
  }
}


export function deserializeJsonPersonWithChildrenDTO(json: JsonOf<PersonWithChildrenDTO>): PersonWithChildrenDTO {
  return {
    ...json,
    children: json.children.map(e => deserializeJsonPersonWithChildrenDTO(e)),
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    dateOfDeath: (json.dateOfDeath != null) ? LocalDate.parseIso(json.dateOfDeath) : null,
    restrictedDetails: deserializeJsonRestrictedDetails(json.restrictedDetails)
  }
}


export function deserializeJsonRestrictedDetails(json: JsonOf<RestrictedDetails>): RestrictedDetails {
  return {
    ...json,
    endDate: (json.endDate != null) ? LocalDate.parseIso(json.endDate) : null
  }
}
