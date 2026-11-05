// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { CreateFosterParentRelationshipBody } from 'lib-common/generated/api-types/family'
import DateRange from 'lib-common/date-range'
import type { FamilyContact } from 'lib-common/generated/api-types/family'
import type { FamilyContactPriorityUpdate } from 'lib-common/generated/api-types/family'
import type { FamilyContactUpdate } from 'lib-common/generated/api-types/family'
import type { FamilyOverview } from 'lib-common/generated/api-types/family'
import type { FosterParentId } from 'lib-common/generated/api-types/shared'
import type { FosterParentRelationship } from 'lib-common/generated/api-types/family'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { ParentshipId } from 'lib-common/generated/api-types/shared'
import type { ParentshipRequest } from 'lib-common/generated/api-types/family'
import type { ParentshipUpdateRequest } from 'lib-common/generated/api-types/family'
import type { ParentshipWithPermittedActions } from 'lib-common/generated/api-types/family'
import type { PartnershipId } from 'lib-common/generated/api-types/shared'
import type { PartnershipRequest } from 'lib-common/generated/api-types/family'
import type { PartnershipUpdateRequest } from 'lib-common/generated/api-types/family'
import type { PartnershipWithPermittedActions } from 'lib-common/generated/api-types/family'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonFamilyOverview } from 'lib-common/generated/api-types/family'
import { deserializeJsonFosterParentRelationship } from 'lib-common/generated/api-types/family'
import { deserializeJsonParentshipWithPermittedActions } from 'lib-common/generated/api-types/family'
import { deserializeJsonPartnershipWithPermittedActions } from 'lib-common/generated/api-types/family'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.family.FamilyController.getFamilyByPerson
*/
export async function getFamilyByPerson(
  request: {
    id: PersonId
  }
): Promise<FamilyOverview> {
  const { data: json } = await client.request<JsonOf<FamilyOverview>>({
    url: uri`/employee/family/by-adult/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonFamilyOverview(json)
}


/**
* Generated from evaka.core.family.FamilyController.getFamilyContactSummary
*/
export async function getFamilyContactSummary(
  request: {
    childId: PersonId
  }
): Promise<FamilyContact[]> {
  const params = createUrlSearchParams(
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<FamilyContact[]>>({
    url: uri`/employee/family/contacts`.toString(),
    method: 'GET',
    params
  })
  return json
}


/**
* Generated from evaka.core.family.FamilyController.updateFamilyContactDetails
*/
export async function updateFamilyContactDetails(
  request: {
    body: FamilyContactUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/family/contacts`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FamilyContactUpdate>
  })
  return json
}


/**
* Generated from evaka.core.family.FamilyController.updateFamilyContactPriority
*/
export async function updateFamilyContactPriority(
  request: {
    body: FamilyContactPriorityUpdate
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/family/contacts/priority`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FamilyContactPriorityUpdate>
  })
  return json
}


/**
* Generated from evaka.core.family.FosterParentController.createFosterParentRelationship
*/
export async function createFosterParentRelationship(
  request: {
    body: CreateFosterParentRelationshipBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/foster-parent`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreateFosterParentRelationshipBody>
  })
  return json
}


/**
* Generated from evaka.core.family.FosterParentController.deleteFosterParentRelationship
*/
export async function deleteFosterParentRelationship(
  request: {
    id: FosterParentId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/foster-parent/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.family.FosterParentController.getFosterChildren
*/
export async function getFosterChildren(
  request: {
    parentId: PersonId
  }
): Promise<FosterParentRelationship[]> {
  const { data: json } = await client.request<JsonOf<FosterParentRelationship[]>>({
    url: uri`/employee/foster-parent/by-parent/${request.parentId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFosterParentRelationship(e))
}


/**
* Generated from evaka.core.family.FosterParentController.getFosterParents
*/
export async function getFosterParents(
  request: {
    childId: PersonId
  }
): Promise<FosterParentRelationship[]> {
  const { data: json } = await client.request<JsonOf<FosterParentRelationship[]>>({
    url: uri`/employee/foster-parent/by-child/${request.childId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonFosterParentRelationship(e))
}


/**
* Generated from evaka.core.family.FosterParentController.updateFosterParentRelationshipValidity
*/
export async function updateFosterParentRelationshipValidity(
  request: {
    id: FosterParentId,
    body: DateRange
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/foster-parent/${request.id}`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DateRange>
  })
  return json
}


/**
* Generated from evaka.core.family.ParentshipController.createParentship
*/
export async function createParentship(
  request: {
    body: ParentshipRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/parentships`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ParentshipRequest>
  })
  return json
}


/**
* Generated from evaka.core.family.ParentshipController.deleteParentship
*/
export async function deleteParentship(
  request: {
    id: ParentshipId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/parentships/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.family.ParentshipController.getParentships
*/
export async function getParentships(
  request: {
    headOfChildId?: PersonId | null,
    childId?: PersonId | null
  }
): Promise<ParentshipWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['headOfChildId', request.headOfChildId],
    ['childId', request.childId]
  )
  const { data: json } = await client.request<JsonOf<ParentshipWithPermittedActions[]>>({
    url: uri`/employee/parentships`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonParentshipWithPermittedActions(e))
}


/**
* Generated from evaka.core.family.ParentshipController.retryParentship
*/
export async function retryParentship(
  request: {
    id: ParentshipId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/parentships/${request.id}/retry`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from evaka.core.family.ParentshipController.updateParentship
*/
export async function updateParentship(
  request: {
    id: ParentshipId,
    body: ParentshipUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/parentships/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<ParentshipUpdateRequest>
  })
  return json
}


/**
* Generated from evaka.core.family.PartnershipsController.createPartnership
*/
export async function createPartnership(
  request: {
    body: PartnershipRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/partnerships`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PartnershipRequest>
  })
  return json
}


/**
* Generated from evaka.core.family.PartnershipsController.deletePartnership
*/
export async function deletePartnership(
  request: {
    partnershipId: PartnershipId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/partnerships/${request.partnershipId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.family.PartnershipsController.getPartnerships
*/
export async function getPartnerships(
  request: {
    personId: PersonId
  }
): Promise<PartnershipWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['personId', request.personId]
  )
  const { data: json } = await client.request<JsonOf<PartnershipWithPermittedActions[]>>({
    url: uri`/employee/partnerships`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonPartnershipWithPermittedActions(e))
}


/**
* Generated from evaka.core.family.PartnershipsController.retryPartnership
*/
export async function retryPartnership(
  request: {
    partnershipId: PartnershipId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/partnerships/${request.partnershipId}/retry`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from evaka.core.family.PartnershipsController.updatePartnership
*/
export async function updatePartnership(
  request: {
    partnershipId: PartnershipId,
    body: PartnershipUpdateRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/partnerships/${request.partnershipId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<PartnershipUpdateRequest>
  })
  return json
}
