// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { DecisionGenericReasoning } from 'lib-common/generated/api-types/decision'
import type { DecisionGenericReasoningId } from 'lib-common/generated/api-types/shared'
import type { DecisionGenericReasoningRequest } from 'lib-common/generated/api-types/decision'
import type { DecisionId } from 'lib-common/generated/api-types/shared'
import type { DecisionIndividualReasoning } from 'lib-common/generated/api-types/decision'
import type { DecisionIndividualReasoningId } from 'lib-common/generated/api-types/shared'
import type { DecisionIndividualReasoningRequest } from 'lib-common/generated/api-types/decision'
import type { DecisionReasoningCollectionType } from 'lib-common/generated/api-types/decision'
import type { DecisionUnit } from 'lib-common/generated/api-types/decision'
import type { DecisionWithPermittedActions } from 'lib-common/generated/api-types/decision'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { Uri } from 'lib-common/uri'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonDecisionGenericReasoning } from 'lib-common/generated/api-types/decision'
import { deserializeJsonDecisionIndividualReasoning } from 'lib-common/generated/api-types/decision'
import { deserializeJsonDecisionWithPermittedActions } from 'lib-common/generated/api-types/decision'
import { uri } from 'lib-common/uri'


/**
* Generated from evaka.core.decision.DecisionController.downloadDecisionPdf
*/
export function downloadDecisionPdf(
  request: {
    id: DecisionId
  }
): { url: Uri } {
  return {
    url: uri`/employee/decisions/${request.id}/download`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from evaka.core.decision.DecisionController.getDecisionUnits
*/
export async function getDecisionUnits(): Promise<DecisionUnit[]> {
  const { data: json } = await client.request<JsonOf<DecisionUnit[]>>({
    url: uri`/employee/decisions/units`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from evaka.core.decision.DecisionController.getDecisionsByGuardian
*/
export async function getDecisionsByGuardian(
  request: {
    id: PersonId
  }
): Promise<DecisionWithPermittedActions[]> {
  const params = createUrlSearchParams(
    ['id', request.id]
  )
  const { data: json } = await client.request<JsonOf<DecisionWithPermittedActions[]>>({
    url: uri`/employee/decisions/by-guardian`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDecisionWithPermittedActions(e))
}


/**
* Generated from evaka.core.decision.DecisionController.planArchiveDecision
*/
export async function planArchiveDecision(
  request: {
    decisionId: DecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/decisions/${request.decisionId}/archive`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from evaka.core.decision.reasoning.DecisionReasoningController.createGenericReasoning
*/
export async function createGenericReasoning(
  request: {
    body: DecisionGenericReasoningRequest
  }
): Promise<DecisionGenericReasoningId> {
  const { data: json } = await client.request<JsonOf<DecisionGenericReasoningId>>({
    url: uri`/employee/decision-reasonings/generic`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DecisionGenericReasoningRequest>
  })
  return json
}


/**
* Generated from evaka.core.decision.reasoning.DecisionReasoningController.createIndividualReasoning
*/
export async function createIndividualReasoning(
  request: {
    body: DecisionIndividualReasoningRequest
  }
): Promise<DecisionIndividualReasoningId> {
  const { data: json } = await client.request<JsonOf<DecisionIndividualReasoningId>>({
    url: uri`/employee/decision-reasonings/individual`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DecisionIndividualReasoningRequest>
  })
  return json
}


/**
* Generated from evaka.core.decision.reasoning.DecisionReasoningController.deleteGenericReasoning
*/
export async function deleteGenericReasoning(
  request: {
    id: DecisionGenericReasoningId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/decision-reasonings/generic/${request.id}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from evaka.core.decision.reasoning.DecisionReasoningController.getGenericReasonings
*/
export async function getGenericReasonings(
  request: {
    collectionType: DecisionReasoningCollectionType
  }
): Promise<DecisionGenericReasoning[]> {
  const params = createUrlSearchParams(
    ['collectionType', request.collectionType.toString()]
  )
  const { data: json } = await client.request<JsonOf<DecisionGenericReasoning[]>>({
    url: uri`/employee/decision-reasonings/generic`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDecisionGenericReasoning(e))
}


/**
* Generated from evaka.core.decision.reasoning.DecisionReasoningController.getIndividualReasonings
*/
export async function getIndividualReasonings(
  request: {
    collectionType: DecisionReasoningCollectionType
  }
): Promise<DecisionIndividualReasoning[]> {
  const params = createUrlSearchParams(
    ['collectionType', request.collectionType.toString()]
  )
  const { data: json } = await client.request<JsonOf<DecisionIndividualReasoning[]>>({
    url: uri`/employee/decision-reasonings/individual`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonDecisionIndividualReasoning(e))
}


/**
* Generated from evaka.core.decision.reasoning.DecisionReasoningController.removeGenericReasoning
*/
export async function removeGenericReasoning(
  request: {
    id: DecisionGenericReasoningId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/decision-reasonings/generic/${request.id}/remove`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from evaka.core.decision.reasoning.DecisionReasoningController.removeIndividualReasoning
*/
export async function removeIndividualReasoning(
  request: {
    id: DecisionIndividualReasoningId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/decision-reasonings/individual/${request.id}/remove`.toString(),
    method: 'PUT'
  })
  return json
}


/**
* Generated from evaka.core.decision.reasoning.DecisionReasoningController.updateGenericReasoning
*/
export async function updateGenericReasoning(
  request: {
    id: DecisionGenericReasoningId,
    body: DecisionGenericReasoningRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/decision-reasonings/generic/${request.id}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<DecisionGenericReasoningRequest>
  })
  return json
}
