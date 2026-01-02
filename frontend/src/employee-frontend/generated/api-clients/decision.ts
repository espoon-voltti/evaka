// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { DecisionId } from 'lib-common/generated/api-types/shared'
import type { DecisionUnit } from 'lib-common/generated/api-types/decision'
import type { DecisionWithPermittedActions } from 'lib-common/generated/api-types/decision'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { Uri } from 'lib-common/uri'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonDecisionWithPermittedActions } from 'lib-common/generated/api-types/decision'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.decision.DecisionController.downloadDecisionPdf
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
* Generated from fi.espoo.evaka.decision.DecisionController.getDecisionUnits
*/
export async function getDecisionUnits(): Promise<DecisionUnit[]> {
  const { data: json } = await client.request<JsonOf<DecisionUnit[]>>({
    url: uri`/employee/decisions/units`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.decision.DecisionController.getDecisionsByGuardian
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
* Generated from fi.espoo.evaka.decision.DecisionController.planArchiveDecision
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
