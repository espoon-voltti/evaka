// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { GroupPlacementRequestBody } from 'lib-common/generated/api-types/placement'
import { GroupTransferRequestBody } from 'lib-common/generated/api-types/placement'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { PlacementCreateRequestBody } from 'lib-common/generated/api-types/placement'
import { PlacementPlanDetails } from 'lib-common/generated/api-types/placement'
import { PlacementResponse } from 'lib-common/generated/api-types/placement'
import { PlacementUpdateRequestBody } from 'lib-common/generated/api-types/placement'
import { UUID } from 'lib-common/types'
import { client } from '../../api/client'
import { deserializeJsonPlacementPlanDetails } from 'lib-common/generated/api-types/placement'
import { deserializeJsonPlacementResponse } from 'lib-common/generated/api-types/placement'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.placement.PlacementController.createGroupPlacement
*/
export async function createGroupPlacement(
  request: {
    placementId: UUID,
    body: GroupPlacementRequestBody
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/placements/${request.placementId}/group-placements`.toString(),
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
    url: uri`/placements`.toString(),
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
    groupPlacementId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/group-placements/${request.groupPlacementId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.deletePlacement
*/
export async function deletePlacement(
  request: {
    placementId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/placements/${request.placementId}`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.getChildPlacementPeriods
*/
export async function getChildPlacementPeriods(
  request: {
    adultId: UUID
  }
): Promise<FiniteDateRange[]> {
  const { data: json } = await client.request<JsonOf<FiniteDateRange[]>>({
    url: uri`/placements/child-placement-periods/${request.adultId}`.toString(),
    method: 'GET'
  })
  return json.map(e => FiniteDateRange.parseJson(e))
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.getPlacementPlans
*/
export async function getPlacementPlans(
  request: {
    daycareId: UUID,
    from: LocalDate,
    to: LocalDate
  }
): Promise<PlacementPlanDetails[]> {
  const { data: json } = await client.request<JsonOf<PlacementPlanDetails[]>>({
    url: uri`/placements/plans`.toString(),
    method: 'GET',
    params: {
      daycareId: request.daycareId,
      from: request.from.formatIso(),
      to: request.to.formatIso()
    }
  })
  return json.map(e => deserializeJsonPlacementPlanDetails(e))
}


/**
* Generated from fi.espoo.evaka.placement.PlacementController.getPlacements
*/
export async function getPlacements(
  request: {
    daycareId: UUID | null,
    childId: UUID | null,
    from: LocalDate | null,
    to: LocalDate | null
  }
): Promise<PlacementResponse> {
  const { data: json } = await client.request<JsonOf<PlacementResponse>>({
    url: uri`/placements`.toString(),
    method: 'GET',
    params: {
      daycareId: request.daycareId,
      childId: request.childId,
      from: request.from?.formatIso(),
      to: request.to?.formatIso()
    }
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
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/group-placements/${request.groupPlacementId}/transfer`.toString(),
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
    placementId: UUID,
    body: PlacementUpdateRequestBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/placements/${request.placementId}`.toString(),
    method: 'PUT',
    data: request.body satisfies JsonCompatible<PlacementUpdateRequestBody>
  })
  return json
}
