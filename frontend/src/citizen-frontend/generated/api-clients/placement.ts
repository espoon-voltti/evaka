// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import { ChildPlacementResponse } from 'lib-common/generated/api-types/placement'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PlacementTerminationRequestBody } from 'lib-common/generated/api-types/placement'
import { UUID } from 'lib-common/types'
import { client } from '../../api-client'
import { deserializeJsonChildPlacementResponse } from 'lib-common/generated/api-types/placement'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.placement.PlacementControllerCitizen.getPlacements
*/
export async function getPlacements(
  request: {
    childId: UUID
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
    childId: UUID,
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
