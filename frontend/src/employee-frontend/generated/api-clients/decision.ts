// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { DecisionId } from 'lib-common/generated/api-types/shared'
import { DecisionListResponse } from 'lib-common/generated/api-types/decision'
import { DecisionUnit } from 'lib-common/generated/api-types/decision'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { Uri } from 'lib-common/uri'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonDecisionListResponse } from 'lib-common/generated/api-types/decision'
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
): Promise<DecisionListResponse> {
  const params = createUrlSearchParams(
    ['id', request.id]
  )
  const { data: json } = await client.request<JsonOf<DecisionListResponse>>({
    url: uri`/employee/decisions/by-guardian`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonDecisionListResponse(json)
}
