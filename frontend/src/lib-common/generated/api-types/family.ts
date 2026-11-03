// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { ApplicationId } from './shared'
import type { ApplicationType } from './application'
import DateRange from '../../date-range'
import type { EvakaUser } from './user'
import type { EvakaUserId } from './shared'
import type { FosterParentId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { IncomeEffect } from './invoicing'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { ParentshipId } from './shared'
import type { PartnershipId } from './shared'
import type { PersonId } from './shared'
import type { PersonJSON } from './person'
import type { PersonSummary } from './person'
import { deserializeJsonPersonJSON } from './person'
import { deserializeJsonPersonSummary } from './person'

/**
* Generated from evaka.core.family.CreateFosterParentRelationshipBody
*/
export interface CreateFosterParentRelationshipBody {
  childId: PersonId
  parentId: PersonId
  validDuring: DateRange
}

/**
* Generated from evaka.core.family.CreateSource
*/
export type CreateSource =
  | 'USER'
  | 'APPLICATION'
  | 'DVV'

/**
* Generated from evaka.core.family.CreationModificationMetadata
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
* Generated from evaka.core.family.FamilyContact
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
* Generated from evaka.core.family.FamilyContactPriorityUpdate
*/
export interface FamilyContactPriorityUpdate {
  childId: PersonId
  contactPersonId: PersonId
  priority: number | null
}

/**
* Generated from evaka.core.family.FamilyContactRole
*/
export type FamilyContactRole =
  | 'LOCAL_GUARDIAN'
  | 'LOCAL_FOSTER_PARENT'
  | 'LOCAL_ADULT'
  | 'LOCAL_SIBLING'
  | 'REMOTE_GUARDIAN'
  | 'REMOTE_FOSTER_PARENT'

/**
* Generated from evaka.core.family.FamilyContactUpdate
*/
export interface FamilyContactUpdate {
  backupPhone: string | null
  childId: PersonId
  contactPersonId: PersonId
  email: string | null
  phone: string | null
}

/**
* Generated from evaka.core.family.FamilyMember
*/
export interface FamilyMember {
  firstName: string
  lastName: string
  personId: PersonId
}

/**
* Generated from evaka.core.family.FamilyMembers
*/
export interface FamilyMembers {
  adults: FamilyMember[]
  children: FamilyMember[]
}

/**
* Generated from evaka.core.family.FamilyOverview
*/
export interface FamilyOverview {
  children: FamilyOverviewPerson[]
  headOfFamily: FamilyOverviewPerson
  partner: FamilyOverviewPerson | null
  totalIncome: FamilyOverviewIncome | null
}

/**
* Generated from evaka.core.family.FamilyOverviewIncome
*/
export interface FamilyOverviewIncome {
  effect: IncomeEffect | null
  total: number | null
}

/**
* Generated from evaka.core.family.FamilyOverviewPerson
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
* Generated from evaka.core.family.FosterParentRelationship
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
* Generated from evaka.core.family.ModifySource
*/
export type ModifySource =
  | 'USER'
  | 'DVV'

/**
* Generated from evaka.core.family.ParentshipDetailed
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
* Generated from evaka.core.family.ParentshipController.ParentshipRequest
*/
export interface ParentshipRequest {
  childId: PersonId
  endDate: LocalDate
  headOfChildId: PersonId
  startDate: LocalDate
}

/**
* Generated from evaka.core.family.ParentshipController.ParentshipUpdateRequest
*/
export interface ParentshipUpdateRequest {
  endDate: LocalDate
  startDate: LocalDate
}

/**
* Generated from evaka.core.family.ParentshipController.ParentshipWithPermittedActions
*/
export interface ParentshipWithPermittedActions {
  data: ParentshipDetailed
  permittedActions: Action.Parentship[]
}

/**
* Generated from evaka.core.family.Partnership
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
* Generated from evaka.core.family.PartnershipsController.PartnershipRequest
*/
export interface PartnershipRequest {
  endDate: LocalDate | null
  person1Id: PersonId
  person2Id: PersonId
  startDate: LocalDate
}

/**
* Generated from evaka.core.family.PartnershipsController.PartnershipUpdateRequest
*/
export interface PartnershipUpdateRequest {
  endDate: LocalDate | null
  startDate: LocalDate
}

/**
* Generated from evaka.core.family.PartnershipsController.PartnershipWithPermittedActions
*/
export interface PartnershipWithPermittedActions {
  data: Partnership
  permittedActions: Action.Partnership[]
}


export function deserializeJsonCreateFosterParentRelationshipBody(json: JsonOf<CreateFosterParentRelationshipBody>): CreateFosterParentRelationshipBody {
  return {
    ...json,
    validDuring: DateRange.parseJson(json.validDuring)
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
