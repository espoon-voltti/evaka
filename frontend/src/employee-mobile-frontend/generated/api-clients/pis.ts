// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AddSsnRequest } from 'lib-common/generated/api-types/pis'
import { CreatePersonBody } from 'lib-common/generated/api-types/pis'
import { DisableSsnRequest } from 'lib-common/generated/api-types/pis'
import { EvakaRightsRequest } from 'lib-common/generated/api-types/pis'
import { GetOrCreatePersonBySsnRequest } from 'lib-common/generated/api-types/pis'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { MergeRequest } from 'lib-common/generated/api-types/pis'
import { PersonIdentityResponseJSON } from 'lib-common/generated/api-types/pis'
import { PersonJSON } from 'lib-common/generated/api-types/pis'
import { PersonPatch } from 'lib-common/generated/api-types/pis'
import { PersonResponse } from 'lib-common/generated/api-types/pis'
import { PersonWithChildrenDTO } from 'lib-common/generated/api-types/pis'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { deserializeJsonPersonJSON } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPersonResponse } from 'lib-common/generated/api-types/pis'
import { deserializeJsonPersonWithChildrenDTO } from 'lib-common/generated/api-types/pis'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.addSsn
*/
export async function addSsn(
  request: {
    personId: UUID,
    body: AddSsnRequest
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/person/${request.personId}/ssn`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AddSsnRequest>
  })
  return deserializeJsonPersonJSON(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.createEmpty
*/
export async function createEmpty(): Promise<PersonIdentityResponseJSON> {
  const { data: json } = await client.request<JsonOf<PersonIdentityResponseJSON>>({
    url: uri`/person`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.createPerson
*/
export async function createPerson(
  request: {
    body: CreatePersonBody
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/person/create`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreatePersonBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.disableSsn
*/
export async function disableSsn(
  request: {
    personId: UUID,
    body: DisableSsnRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/person/${request.personId}/ssn/disable`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DisableSsnRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.duplicatePerson
*/
export async function duplicatePerson(
  request: {
    personId: UUID
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/person/${request.personId}/duplicate`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getOrCreatePersonBySsn
*/
export async function getOrCreatePersonBySsn(
  request: {
    body: GetOrCreatePersonBySsnRequest
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/person/details/ssn`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<GetOrCreatePersonBySsnRequest>
  })
  return deserializeJsonPersonJSON(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getPerson
*/
export async function getPerson(
  request: {
    personId: UUID
  }
): Promise<PersonResponse> {
  const { data: json } = await client.request<JsonOf<PersonResponse>>({
    url: uri`/person/${request.personId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonPersonResponse(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getPersonBlockedGuardians
*/
export async function getPersonBlockedGuardians(
  request: {
    personId: UUID
  }
): Promise<PersonJSON[]> {
  const { data: json } = await client.request<JsonOf<PersonJSON[]>>({
    url: uri`/person/blocked-guardians/${request.personId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPersonJSON(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getPersonDependants
*/
export async function getPersonDependants(
  request: {
    personId: UUID
  }
): Promise<PersonWithChildrenDTO[]> {
  const { data: json } = await client.request<JsonOf<PersonWithChildrenDTO[]>>({
    url: uri`/person/dependants/${request.personId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPersonWithChildrenDTO(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getPersonGuardians
*/
export async function getPersonGuardians(
  request: {
    personId: UUID
  }
): Promise<PersonJSON[]> {
  const { data: json } = await client.request<JsonOf<PersonJSON[]>>({
    url: uri`/person/guardians/${request.personId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPersonJSON(e))
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.getPersonIdentity
*/
export async function getPersonIdentity(
  request: {
    personId: UUID
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/person/details/${request.personId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonPersonJSON(json)
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.mergePeople
*/
export async function mergePeople(
  request: {
    body: MergeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/person/merge`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<MergeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.safeDeletePerson
*/
export async function safeDeletePerson(
  request: {
    personId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/person/${request.personId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.updateGuardianEvakaRights
*/
export async function updateGuardianEvakaRights(
  request: {
    childId: UUID,
    body: EvakaRightsRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/person/${request.childId}/evaka-rights`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<EvakaRightsRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.updatePersonAndFamilyFromVtj
*/
export async function updatePersonAndFamilyFromVtj(
  request: {
    personId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/person/${request.personId}/vtj-update`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.pis.controllers.PersonController.updatePersonDetails
*/
export async function updatePersonDetails(
  request: {
    personId: UUID,
    body: PersonPatch
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/person/${request.personId}`.toString(),
    method: 'PATCH',
    data: request.body satisfies JsonCompatible<PersonPatch>
  })
  return deserializeJsonPersonJSON(json)
}
