// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { AxiosHeaders } from 'axios'
import { DecisionListResponse } from 'lib-common/generated/api-types/decision'
import { DecisionUnit } from 'lib-common/generated/api-types/decision'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonDecisionListResponse } from 'lib-common/generated/api-types/decision'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.decision.DecisionController.getDecisionUnits
*/
export async function getDecisionUnits(
  headers?: AxiosHeaders
): Promise<DecisionUnit[]> {
  const { data: json } = await client.request<JsonOf<DecisionUnit[]>>({
    url: uri`/employee/decisions/units`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.decision.DecisionController.getDecisionsByGuardian
*/
export async function getDecisionsByGuardian(
  request: {
    id: UUID
  },
  headers?: AxiosHeaders
): Promise<DecisionListResponse> {
  const params = createUrlSearchParams(
    ['id', request.id]
  )
  const { data: json } = await client.request<JsonOf<DecisionListResponse>>({
    url: uri`/employee/decisions/by-guardian`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonDecisionListResponse(json)
}
