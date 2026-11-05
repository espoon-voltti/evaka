// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AddSsnRequest } from 'lib-common/generated/api-types/person'
import type { CreatePersonBody } from 'lib-common/generated/api-types/person'
import type { DisableSsnRequest } from 'lib-common/generated/api-types/person'
import type { EvakaRightsRequest } from 'lib-common/generated/api-types/person'
import type { GetOrCreatePersonBySsnRequest } from 'lib-common/generated/api-types/person'
import type { GuardiansResponse } from 'lib-common/generated/api-types/person'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { MergeRequest } from 'lib-common/generated/api-types/person'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { PersonJSON } from 'lib-common/generated/api-types/person'
import type { PersonPatch } from 'lib-common/generated/api-types/person'
import type { PersonResponse } from 'lib-common/generated/api-types/person'
import type { PersonSensitiveDetails } from 'lib-common/generated/api-types/person'
import type { PersonSummary } from 'lib-common/generated/api-types/person'
import type { SearchPersonBody } from 'lib-common/generated/api-types/person'
import type { Uri } from 'lib-common/uri'
import type { VtjPersonPreview } from 'lib-common/generated/api-types/vtj'
import type { VtjPersonPreviewRequest } from 'lib-common/generated/api-types/person'
import { client } from '../../api/client'
import { deserializeJsonGuardiansResponse } from 'lib-common/generated/api-types/person'
import { deserializeJsonPersonJSON } from 'lib-common/generated/api-types/person'
import { deserializeJsonPersonResponse } from 'lib-common/generated/api-types/person'
import { deserializeJsonPersonSensitiveDetails } from 'lib-common/generated/api-types/person'
import { deserializeJsonPersonSummary } from 'lib-common/generated/api-types/person'
import { deserializeJsonVtjPersonPreview } from 'lib-common/generated/api-types/vtj'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.person.PersonController.addSsn
*/
export async function addSsn(
  request: {
    personId: PersonId,
    body: AddSsnRequest
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/employee/person/${request.personId}/ssn`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<AddSsnRequest>
  })
  return deserializeJsonPersonJSON(json)
}


/**
* Generated from evaka.core.person.PersonController.createPerson
*/
export async function createPerson(
  request: {
    body: CreatePersonBody
  }
): Promise<PersonId> {
  const { data: json } = await client.request<JsonOf<PersonId>>({
    url: uri`/employee/person/create`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<CreatePersonBody>
  })
  return json
}


/**
* Generated from evaka.core.person.PersonController.disableSsn
*/
export async function disableSsn(
  request: {
    personId: PersonId,
    body: DisableSsnRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/${request.personId}/ssn/disable`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DisableSsnRequest>
  })
  return json
}


/**
* Generated from evaka.core.person.PersonController.getAddressPagePdf
*/
export function getAddressPagePdf(
  request: {
    guardianId: PersonId
  }
): { url: Uri } {
  return {
    url: uri`/employee/person/${request.guardianId}/address-page/download`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from evaka.core.person.PersonController.getOrCreatePersonBySsn
*/
export async function getOrCreatePersonBySsn(
  request: {
    body: GetOrCreatePersonBySsnRequest
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/employee/person/details/ssn`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<GetOrCreatePersonBySsnRequest>
  })
  return deserializeJsonPersonJSON(json)
}


/**
* Generated from evaka.core.person.PersonController.getPerson
*/
export async function getPerson(
  request: {
    personId: PersonId
  }
): Promise<PersonResponse> {
  const { data: json } = await client.request<JsonOf<PersonResponse>>({
    url: uri`/employee/person/${request.personId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonPersonResponse(json)
}


/**
* Generated from evaka.core.person.PersonController.getPersonDependants
*/
export async function getPersonDependants(
  request: {
    personId: PersonId
  }
): Promise<PersonJSON[]> {
  const { data: json } = await client.request<JsonOf<PersonJSON[]>>({
    url: uri`/employee/person/dependants/${request.personId}`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonPersonJSON(e))
}


/**
* Generated from evaka.core.person.PersonController.getPersonGuardians
*/
export async function getPersonGuardians(
  request: {
    personId: PersonId
  }
): Promise<GuardiansResponse> {
  const { data: json } = await client.request<JsonOf<GuardiansResponse>>({
    url: uri`/employee/person/guardians/${request.personId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonGuardiansResponse(json)
}


/**
* Generated from evaka.core.person.PersonController.getPersonIdentity
*/
export async function getPersonIdentity(
  request: {
    personId: PersonId
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/employee/person/details/${request.personId}`.toString(),
    method: 'GET'
  })
  return deserializeJsonPersonJSON(json)
}


/**
* Generated from evaka.core.person.PersonController.getPersonSensitiveDetails
*/
export async function getPersonSensitiveDetails(
  request: {
    personId: PersonId
  }
): Promise<PersonSensitiveDetails> {
  const { data: json } = await client.request<JsonOf<PersonSensitiveDetails>>({
    url: uri`/employee/person/${request.personId}/sensitive-details`.toString(),
    method: 'GET'
  })
  return deserializeJsonPersonSensitiveDetails(json)
}


/**
* Generated from evaka.core.person.PersonController.mergePeople
*/
export async function mergePeople(
  request: {
    body: MergeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/merge`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<MergeRequest>
  })
  return json
}


/**
* Generated from evaka.core.person.PersonController.previewVtjPerson
*/
export async function previewVtjPerson(
  request: {
    body: VtjPersonPreviewRequest
  }
): Promise<VtjPersonPreview> {
  const { data: json } = await client.request<JsonOf<VtjPersonPreview>>({
    url: uri`/employee/person/vtj-preview`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<VtjPersonPreviewRequest>
  })
  return deserializeJsonVtjPersonPreview(json)
}


/**
* Generated from evaka.core.person.PersonController.safeDeletePerson
*/
export async function safeDeletePerson(
  request: {
    personId: PersonId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/${request.personId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.person.PersonController.searchPerson
*/
export async function searchPerson(
  request: {
    body: SearchPersonBody
  }
): Promise<PersonSummary[]> {
  const { data: json } = await client.request<JsonOf<PersonSummary[]>>({
    url: uri`/employee/person/search`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<SearchPersonBody>
  })
  return json.map(e => deserializeJsonPersonSummary(e))
}


/**
* Generated from evaka.core.person.PersonController.updateGuardianEvakaRights
*/
export async function updateGuardianEvakaRights(
  request: {
    childId: PersonId,
    body: EvakaRightsRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/${request.childId}/evaka-rights`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<EvakaRightsRequest>
  })
  return json
}


/**
* Generated from evaka.core.person.PersonController.updatePersonAndFamilyFromVtj
*/
export async function updatePersonAndFamilyFromVtj(
  request: {
    personId: PersonId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/person/${request.personId}/vtj-update`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from evaka.core.person.PersonController.updatePersonDetails
*/
export async function updatePersonDetails(
  request: {
    personId: PersonId,
    body: PersonPatch
  }
): Promise<PersonJSON> {
  const { data: json } = await client.request<JsonOf<PersonJSON>>({
    url: uri`/employee/person/${request.personId}`.toString(),
    method: 'PATCH',
    data: request.body satisfies JsonCompatible<PersonPatch>
  })
  return deserializeJsonPersonJSON(json)
}
