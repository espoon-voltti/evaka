// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from 'lib-common/finite-date-range'
import { GroupPlacementId } from 'lib-common/generated/api-types/shared'
import { GroupPlacementRequestBody } from 'lib-common/generated/api-types/placement'
import { GroupTransferRequestBody } from 'lib-common/generated/api-types/placement'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PersonId } from 'lib-common/generated/api-types/shared'
import { PlacementCreateRequestBody } from 'lib-common/generated/api-types/placement'
import { PlacementId } from 'lib-common/generated/api-types/shared'
import { PlacementResponse } from 'lib-common/generated/api-types/placement'
import { PlacementUpdateRequestBody } from 'lib-common/generated/api-types/placement'
import { client } from '../../api/client'
import { deserializeJsonPlacementResponse } from 'lib-common/generated/api-types/placement'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.placement.PlacementController.createGroupPlacement
*/
export async function createGroupPlacement(
  request: {
    placementId: PlacementId,
    body: GroupPlacementRequestBody
  }
): Promise<GroupPlacementId> {
  const { data: json } = await client.request<JsonOf<GroupPlacementId>>({
    url: uri`/employee/placements/${request.placementId}/group-placements`.toString(),
    method: 'POST',
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/placements`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<PlacementCreateRequestBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.deleteGroupPlacement
*/
export async function deleteGroupPlacement(
  request: {
    groupPlacementId: GroupPlacementId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/group-placements/${request.groupPlacementId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.deletePlacement
*/
export async function deletePlacement(
  request: {
    placementId: PlacementId
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/placements/${request.placementId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.getChildPlacementPeriods
*/
export async function getChildPlacementPeriods(
  request: {
    adultId: PersonId
  }
): Promise<FiniteDateRange[]> {
  const { data: json } = await client.request<JsonOf<FiniteDateRange[]>>({
    url: uri`/employee/placements/child-placement-periods/${request.adultId}`.toString(),
    method: 'GET'
  })
  return json.map(e => FiniteDateRange.parseJson(e))
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.getChildPlacements
*/
export async function getChildPlacements(
  request: {
    childId: PersonId
  }
): Promise<PlacementResponse> {
  const { data: json } = await client.request<JsonOf<PlacementResponse>>({
    url: uri`/employee/children/${request.childId}/placements`.toString(),
    method: 'GET'
  })
  return deserializeJsonPlacementResponse(json)
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.transferGroupPlacement
*/
export async function transferGroupPlacement(
  request: {
    groupPlacementId: GroupPlacementId,
    body: GroupTransferRequestBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/group-placements/${request.groupPlacementId}/transfer`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<GroupTransferRequestBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.updatePlacementById
*/
export async function updatePlacementById(
  request: {
    placementId: PlacementId,
    body: PlacementUpdateRequestBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/placements/${request.placementId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<PlacementUpdateRequestBody>
  })
  return json
}
