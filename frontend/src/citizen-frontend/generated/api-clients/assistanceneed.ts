// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedDecisionCitizenListItem } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedDecisionId } from 'lib-common/generated/api-types/shared'
import type { AssistanceNeedPreschoolDecision } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedPreschoolDecisionCitizenListItem } from 'lib-common/generated/api-types/assistanceneed'
import type { AssistanceNeedPreschoolDecisionId } from 'lib-common/generated/api-types/shared'
import type { JsonOf } from 'lib-common/json'
import type { UnreadAssistanceNeedDecisionItem } from 'lib-common/generated/api-types/assistanceneed'
import type { Uri } from 'lib-common/uri'
import { client } from '../../api-client'
import { deserializeJsonAssistanceNeedDecision } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedDecisionCitizenListItem } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedPreschoolDecision } from 'lib-common/generated/api-types/assistanceneed'
import { deserializeJsonAssistanceNeedPreschoolDecisionCitizenListItem } from 'lib-common/generated/api-types/assistanceneed'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionCitizenController.getAssistanceNeedDecision
*/
export async function getAssistanceNeedDecision(
  request: {
    id: AssistanceNeedDecisionId
  }
): Promise<AssistanceNeedDecision> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedDecision>>({
    url: uri`/citizen/children/assistance-need-decision/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonAssistanceNeedDecision(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionCitizenController.getAssistanceNeedDecisionPdf
*/
export function getAssistanceNeedDecisionPdf(
  request: {
    id: AssistanceNeedDecisionId
  }
): { url: Uri } {
  return {
    url: uri`/citizen/children/assistance-need-decision/${request.id}/pdf`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionCitizenController.getAssistanceNeedDecisionUnreadCount
*/
export async function getAssistanceNeedDecisionUnreadCount(): Promise<UnreadAssistanceNeedDecisionItem[]> {
  const { data: json } = await client.request<JsonOf<UnreadAssistanceNeedDecisionItem[]>>({
    url: uri`/citizen/children/assistance-need-decisions/unread-counts`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionCitizenController.getAssistanceNeedDecisions
*/
export async function getAssistanceNeedDecisions(): Promise<AssistanceNeedDecisionCitizenListItem[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedDecisionCitizenListItem[]>>({
    url: uri`/citizen/assistance-need-decisions`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedDecisionCitizenListItem(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionCitizenController.markAssistanceNeedDecisionAsRead
*/
export async function markAssistanceNeedDecisionAsRead(
  request: {
    id: AssistanceNeedDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/children/assistance-need-decision/${request.id}/read`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionCitizenController.getAssistanceNeedPreschoolDecision
*/
export async function getAssistanceNeedPreschoolDecision(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): Promise<AssistanceNeedPreschoolDecision> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedPreschoolDecision>>({
    url: uri`/citizen/children/assistance-need-preschool-decisions/${request.id}`.toString(),
    method: 'GET'
  })
  return deserializeJsonAssistanceNeedPreschoolDecision(json)
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionCitizenController.getAssistanceNeedPreschoolDecisionPdf
*/
export function getAssistanceNeedPreschoolDecisionPdf(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): { url: Uri } {
  return {
    url: uri`/citizen/children/assistance-need-preschool-decisions/${request.id}/pdf`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionCitizenController.getAssistanceNeedPreschoolDecisionUnreadCount
*/
export async function getAssistanceNeedPreschoolDecisionUnreadCount(): Promise<UnreadAssistanceNeedDecisionItem[]> {
  const { data: json } = await client.request<JsonOf<UnreadAssistanceNeedDecisionItem[]>>({
    url: uri`/citizen/children/assistance-need-preschool-decisions/unread-counts`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionCitizenController.getAssistanceNeedPreschoolDecisions
*/
export async function getAssistanceNeedPreschoolDecisions(): Promise<AssistanceNeedPreschoolDecisionCitizenListItem[]> {
  const { data: json } = await client.request<JsonOf<AssistanceNeedPreschoolDecisionCitizenListItem[]>>({
    url: uri`/citizen/assistance-need-preschool-decisions`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAssistanceNeedPreschoolDecisionCitizenListItem(e))
}


/**
* Generated from fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionCitizenController.markAssistanceNeedPreschoolDecisionAsRead
*/
export async function markAssistanceNeedPreschoolDecisionAsRead(
  request: {
    id: AssistanceNeedPreschoolDecisionId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/children/assistance-need-preschool-decisions/${request.id}/read`.toString(),
    method: 'PUT'
  })
  return json
}
