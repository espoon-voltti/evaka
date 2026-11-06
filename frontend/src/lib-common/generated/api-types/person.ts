// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { PersonEmailVerificationId } from './shared'
import type { PersonId } from './shared'
import type { UiLanguage } from './shared'

/**
* Generated from evaka.core.person.PersonController.AddSsnRequest
*/
export interface AddSsnRequest {
  ssn: string
}

/**
* Generated from evaka.core.person.CitizenUserDetails
*/
export interface CitizenUserDetails {
  backupPhone: string
  email: string | null
  firstName: string
  id: PersonId
  lastName: string
  mailingPostOffice: string
  mailingPostalCode: string
  mailingStreetAddress: string
  phone: string
  postOffice: string
  postalCode: string
  preferredName: string
  preferredUiLanguage: UiLanguage | null
  streetAddress: string
  weakLoginUsername: string | null
}

/**
* Generated from evaka.core.person.CreatePersonBody
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
* Generated from evaka.core.person.PersonController.DisableSsnRequest
*/
export interface DisableSsnRequest {
  disabled: boolean
}

/**
* Generated from evaka.core.person.EmailVerification
*/
export interface EmailVerification {
  email: string
  expiresAt: HelsinkiDateTime
  id: PersonEmailVerificationId
  sentAt: HelsinkiDateTime | null
}

/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.EmailVerificationRequest
*/
export interface EmailVerificationRequest {
  code: string
  id: PersonEmailVerificationId
}

/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.EmailVerificationStatusResponse
*/
export interface EmailVerificationStatusResponse {
  email: string | null
  latestVerification: EmailVerification | null
  verifiedEmail: string | null
}

/**
* Generated from evaka.core.person.PersonController.EvakaRightsRequest
*/
export interface EvakaRightsRequest {
  denied: boolean
  guardianId: PersonId
}

/**
* Generated from evaka.core.person.GetOrCreatePersonBySsnRequest
*/
export interface GetOrCreatePersonBySsnRequest {
  ssn: string
}

/**
* Generated from evaka.core.person.PersonController.GuardiansResponse
*/
export interface GuardiansResponse {
  blockedGuardians: PersonJSON[] | null
  guardians: PersonJSON[]
}

/**
* Generated from evaka.core.person.PersonController.MergeRequest
*/
export interface MergeRequest {
  duplicate: PersonId
  master: PersonId
}

/**
* Generated from evaka.core.person.NotificationCategory
*/
export const notificationCategories = [
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

export type NotificationCategory = typeof notificationCategories[number]

/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.NotificationSettings
*/
export interface NotificationSettings {
  disabledEmailTypes: NotificationCategory[]
  disabledPushTypes: NotificationCategory[]
}

/**
* Generated from evaka.core.person.PersonBasicInfo
*/
export interface PersonBasicInfo {
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  duplicateOf: PersonId | null
  firstName: string
  hasSsn: boolean
  id: PersonId
  lastName: string
  restrictedDetailsEnabled: boolean
  ssnAddingDisabled: boolean
}

/**
* Generated from evaka.core.person.PersonJSON
*/
export interface PersonJSON {
  backupPhone: string
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  duplicateOf: PersonId | null
  email: string | null
  firstName: string
  id: PersonId
  language: string | null
  lastName: string
  municipalityOfResidence: string
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
* Generated from evaka.core.person.PersonNameDetails
*/
export interface PersonNameDetails {
  firstName: string
  id: PersonId
  lastName: string
}

/**
* Generated from evaka.core.person.PersonPatch
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
* Generated from evaka.core.person.PersonController.PersonResponse
*/
export interface PersonResponse {
  permittedActions: Action.Person[]
  person: PersonBasicInfo
}

/**
* Generated from evaka.core.person.PersonSensitiveDetails
*/
export interface PersonSensitiveDetails {
  backupPhone: string
  email: string | null
  forceManualFeeDecisions: boolean
  invoiceRecipientName: string
  invoicingPostOffice: string
  invoicingPostalCode: string
  invoicingStreetAddress: string
  language: string | null
  mailingPostOffice: string
  mailingPostalCode: string
  mailingStreetAddress: string
  municipalityOfResidence: string
  ophPersonOid: string | null
  phone: string
  postOffice: string
  postalCode: string
  residenceCode: string
  socialSecurityNumber: string | null
  streetAddress: string
  updatedFromVtj: HelsinkiDateTime | null
}

/**
* Generated from evaka.core.person.PersonSummary
*/
export interface PersonSummary {
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  firstName: string
  id: PersonId
  lastName: string
  restrictedDetailsEnabled: boolean
  socialSecurityNumber: string | null
  streetAddress: string | null
}

/**
* Generated from evaka.core.person.PersonalDataUpdate
*/
export interface PersonalDataUpdate {
  backupPhone: string | null
  email: string | null
  phone: string | null
  preferredName: string | null
}

/**
* Generated from evaka.core.person.SearchPersonBody
*/
export interface SearchPersonBody {
  orderBy: string
  searchTerm: string
  sortDirection: string
}

/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.UpdatePreferredUiLanguageRequest
*/
export interface UpdatePreferredUiLanguageRequest {
  preferredUiLanguage: UiLanguage
}

/**
* Generated from evaka.core.person.PersonalDataControllerCitizen.UpdateWeakLoginCredentialsRequest
*/
export interface UpdateWeakLoginCredentialsRequest {
  password: string
}

/**
* Generated from evaka.core.person.VtjPersonPreviewRequest
*/
export interface VtjPersonPreviewRequest {
  ssn: string
}


export function deserializeJsonCreatePersonBody(json: JsonOf<CreatePersonBody>): CreatePersonBody {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth)
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


export function deserializeJsonGuardiansResponse(json: JsonOf<GuardiansResponse>): GuardiansResponse {
  return {
    ...json,
    blockedGuardians: (json.blockedGuardians != null) ? json.blockedGuardians.map(e => deserializeJsonPersonJSON(e)) : null,
    guardians: json.guardians.map(e => deserializeJsonPersonJSON(e))
  }
}


export function deserializeJsonPersonBasicInfo(json: JsonOf<PersonBasicInfo>): PersonBasicInfo {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    dateOfDeath: (json.dateOfDeath != null) ? LocalDate.parseIso(json.dateOfDeath) : null
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
    person: deserializeJsonPersonBasicInfo(json.person)
  }
}


export function deserializeJsonPersonSensitiveDetails(json: JsonOf<PersonSensitiveDetails>): PersonSensitiveDetails {
  return {
    ...json,
    updatedFromVtj: (json.updatedFromVtj != null) ? HelsinkiDateTime.parseIso(json.updatedFromVtj) : null
  }
}


export function deserializeJsonPersonSummary(json: JsonOf<PersonSummary>): PersonSummary {
  return {
    ...json,
    dateOfBirth: LocalDate.parseIso(json.dateOfBirth),
    dateOfDeath: (json.dateOfDeath != null) ? LocalDate.parseIso(json.dateOfDeath) : null
  }
}
