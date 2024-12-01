// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from 'lib-common/finite-date-range'
import { AxiosHeaders } from 'axios'
import { GroupPlacementRequestBody } from 'lib-common/generated/api-types/placement'
import { GroupTransferRequestBody } from 'lib-common/generated/api-types/placement'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PlacementCreateRequestBody } from 'lib-common/generated/api-types/placement'
import { PlacementResponse } from 'lib-common/generated/api-types/placement'
import { PlacementUpdateRequestBody } from 'lib-common/generated/api-types/placement'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonPlacementResponse } from 'lib-common/generated/api-types/placement'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.placement.PlacementController.createGroupPlacement
*/
export async function createGroupPlacement(
  request: {
    placementId: UUID,
    body: GroupPlacementRequestBody
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee/placements/${request.placementId}/group-placements`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<GroupPlacementRequestBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.createPlacement
*/
export async function createPlacement(
  request: {
    body: PlacementCreateRequestBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/placements`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<PlacementCreateRequestBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.deleteGroupPlacement
*/
export async function deleteGroupPlacement(
  request: {
    groupPlacementId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/group-placements/${request.groupPlacementId}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.deletePlacement
*/
export async function deletePlacement(
  request: {
    placementId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/placements/${request.placementId}`.toString(),
    method: 'DELETE',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.getChildPlacementPeriods
*/
export async function getChildPlacementPeriods(
  request: {
    adultId: UUID
  },
  headers?: AxiosHeaders
): Promise<FiniteDateRange[]> {
  const { data: json } = await client.request<JsonOf<FiniteDateRange[]>>({
    url: uri`/employee/placements/child-placement-periods/${request.adultId}`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => FiniteDateRange.parseJson(e))
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.getChildPlacements
*/
export async function getChildPlacements(
  request: {
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<PlacementResponse> {
  const { data: json } = await client.request<JsonOf<PlacementResponse>>({
    url: uri`/employee/children/${request.childId}/placements`.toString(),
    method: 'GET',
    headers
  })
  return deserializeJsonPlacementResponse(json)
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.transferGroupPlacement
*/
export async function transferGroupPlacement(
  request: {
    groupPlacementId: UUID,
    body: GroupTransferRequestBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/group-placements/${request.groupPlacementId}/transfer`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<GroupTransferRequestBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.updatePlacementById
*/
export async function updatePlacementById(
  request: {
    placementId: UUID,
    body: PlacementUpdateRequestBody
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/placements/${request.placementId}`.toString(),
    method: 'PUT',
    headers,
    data: request.body satisfies JsonCompatible<PlacementUpdateRequestBody>
  })
  return json
}
