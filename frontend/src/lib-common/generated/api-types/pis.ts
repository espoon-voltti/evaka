// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import LocalDate from '../../local-date'
import { Action } from '../action'
import { EmployeeFeatures } from './shared'
import { ExternalId } from './identity'
import { IncomeEffect } from './invoicing'
import { Nationality } from './vtjclient'
import { NativeLanguage } from './vtjclient'
import { UUID } from '../../types'
import { UserRole } from './shared'

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.AddSsnRequest
*/
export interface AddSsnRequest {
  ssn: string
}

/**
* Generated from fi.espoo.evaka.pis.CitizenUser
*/
export interface CitizenUser {
  id: UUID
}

/**
* Generated from fi.espoo.evaka.pis.service.ContactInfo
*/
export interface ContactInfo {
  backupPhone: string
  email: string
  forceManualFeeDecisions: boolean
  invoiceRecipientName: string | null
  invoicingPostOffice: string | null
  invoicingPostalCode: string | null
  invoicingStreetAddress: string | null
  phone: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.CreatePersonBody
*/
export interface CreatePersonBody {
  dateOfBirth: LocalDate
  email: string | null
  firstName: string
  lastName: string
  phone: string | null
  postOffice: string
  postalCode: string
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.DaycareRole
*/
export interface DaycareRole {
  daycareId: UUID
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
* Generated from fi.espoo.evaka.pis.Employee
*/
export interface Employee {
  created: Date
  email: string | null
  externalId: ExternalId | null
  firstName: string
  id: UUID
  lastName: string
  updated: Date | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.EmployeeController.EmployeeUpdate
*/
export interface EmployeeUpdate {
  globalRoles: UserRole[]
}

/**
* Generated from fi.espoo.evaka.pis.EmployeeUser
*/
export interface EmployeeUser {
  allScopedRoles: UserRole[]
  firstName: string
  globalRoles: UserRole[]
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.pis.SystemController.EmployeeUserResponse
*/
export interface EmployeeUserResponse {
  accessibleFeatures: EmployeeFeatures
  allScopedRoles: UserRole[]
  firstName: string
  globalRoles: UserRole[]
  id: UUID
  lastName: string
}

/**
* Generated from fi.espoo.evaka.pis.EmployeeWithDaycareRoles
*/
export interface EmployeeWithDaycareRoles {
  created: Date
  daycareRoles: DaycareRole[]
  email: string | null
  firstName: string
  globalRoles: UserRole[]
  id: UUID
  lastName: string
  updated: Date | null
}

/**
* Generated from fi.espoo.evaka.pis.FamilyContact
*/
export interface FamilyContact {
  backupPhone: string | null
  email: string | null
  firstName: string | null
  id: UUID
  lastName: string | null
  phone: string | null
  postOffice: string
  postalCode: string
  priority: number | null
  role: FamilyContactRole
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.FamilyContactRole
*/
export type FamilyContactRole = 
  | 'LOCAL_GUARDIAN'
  | 'LOCAL_ADULT'
  | 'LOCAL_SIBLING'
  | 'REMOTE_GUARDIAN'

/**
* Generated from fi.espoo.evaka.pis.controllers.FamilyContactUpdate
*/
export interface FamilyContactUpdate {
  childId: UUID
  contactPersonId: UUID
  priority: number | null
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
  headOfChild: UUID | null
  income: FamilyOverviewIncome | null
  lastName: string
  personId: UUID
  postOffice: string
  postalCode: string
  restrictedDetailsEnabled: boolean
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.GetOrCreatePersonBySsnRequest
*/
export interface GetOrCreatePersonBySsnRequest {
  readonly: boolean
  ssn: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.MergeRequest
*/
export interface MergeRequest {
  duplicate: UUID
  master: UUID
}

/**
* Generated from fi.espoo.evaka.pis.NewEmployee
*/
export interface NewEmployee {
  email: string | null
  externalId: ExternalId | null
  firstName: string
  lastName: string
  roles: UserRole[]
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonAddressDTO.Origin
*/
export type Origin = 
  | 'VTJ'
  | 'MUNICIPAL'
  | 'EVAKA'

/**
* Generated from fi.espoo.evaka.pis.service.Parentship
*/
export interface Parentship {
  child: PersonJSON
  childId: UUID
  conflict: boolean
  endDate: LocalDate
  headOfChild: PersonJSON
  headOfChildId: UUID
  id: UUID
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.ParentshipController.ParentshipRequest
*/
export interface ParentshipRequest {
  childId: UUID
  endDate: LocalDate
  headOfChildId: UUID
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
* Generated from fi.espoo.evaka.pis.service.Partnership
*/
export interface Partnership {
  conflict: boolean
  endDate: LocalDate | null
  id: UUID
  partners: PersonJSON[]
  startDate: LocalDate
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PartnershipsController.PartnershipRequest
*/
export interface PartnershipRequest {
  endDate: LocalDate | null
  personIds: UUID[]
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
* Generated from fi.espoo.evaka.pis.service.PersonAddressDTO
*/
export interface PersonAddressDTO {
  city: string
  origin: Origin
  postalCode: string
  residenceCode: string | null
  streetAddress: string
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.PersonIdentityResponseJSON
*/
export interface PersonIdentityResponseJSON {
  id: UUID
  socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonJSON
*/
export interface PersonJSON {
  backupPhone: string
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  email: string | null
  firstName: string | null
  forceManualFeeDecisions: boolean
  id: UUID
  invoiceRecipientName: string
  invoicingPostOffice: string
  invoicingPostalCode: string
  invoicingStreetAddress: string
  language: string | null
  lastName: string | null
  ophPersonOid: string | null
  phone: string | null
  postOffice: string | null
  postalCode: string | null
  residenceCode: string | null
  restrictedDetailsEnabled: boolean
  socialSecurityNumber: string | null
  ssnAddingDisabled: boolean
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
  firstName: string | null
  id: UUID
  lastName: string | null
  restrictedDetailsEnabled: boolean
  socialSecurityNumber: string | null
  streetAddress: string | null
}

/**
* Generated from fi.espoo.evaka.pis.service.PersonWithChildrenDTO
*/
export interface PersonWithChildrenDTO {
  addresses: PersonAddressDTO[]
  children: PersonWithChildrenDTO[]
  dateOfBirth: LocalDate
  dateOfDeath: LocalDate | null
  firstName: string
  id: UUID
  lastName: string
  nationalities: Nationality[]
  nativeLanguage: NativeLanguage | null
  residenceCode: string | null
  restrictedDetails: RestrictedDetails
  socialSecurityNumber: string | null
}

/**
* Generated from fi.espoo.evaka.pis.controllers.PinCode
*/
export interface PinCode {
  pin: string
}

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
  page: number | null
  pageSize: number | null
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
