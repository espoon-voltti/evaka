// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ChildPlacementResponse } from 'lib-common/generated/api-types/placement'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { PlacementTerminationRequestBody } from 'lib-common/generated/api-types/placement'
import { client } from '../../api-client'
import { deserializeJsonChildPlacementResponse } from 'lib-common/generated/api-types/placement'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.placement.PlacementControllerCitizen.getPlacements
*/
export async function getPlacements(
  request: {
    childId: PersonId
  }
): Promise<ChildPlacementResponse> {
  const { data: json } = await client.request<JsonOf<ChildPlacementResponse>>({
    url: uri`/citizen/children/${request.childId}/placements`.toString(),
    method: 'GET'
  })
  return deserializeJsonChildPlacementResponse(json)
}


/**
* Generated from fi.espoo.evaka.placement.PlacementControllerCitizen.postPlacementTermination
*/
export async function postPlacementTermination(
  request: {
    childId: PersonId,
    body: PlacementTerminationRequestBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/citizen/children/${request.childId}/placements/terminate`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PlacementTerminationRequestBody>
  })
  return json
}
