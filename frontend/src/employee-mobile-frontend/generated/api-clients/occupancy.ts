// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { JsonOf } from 'lib-common/json'
import { OccupancyResponse } from 'lib-common/generated/api-types/occupancy'
import { OccupancyResponseGroupLevel } from 'lib-common/generated/api-types/occupancy'
import { OccupancyType } from 'lib-common/generated/api-types/occupancy'
import { UUID } from 'lib-common/types'
import { client } from '../../client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonOccupancyResponse } from 'lib-common/generated/api-types/occupancy'
import { deserializeJsonOccupancyResponseGroupLevel } from 'lib-common/generated/api-types/occupancy'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getOccupancyPeriods
*/
export async function getOccupancyPeriods(
  request: {
    unitId: UUID,
    from: LocalDate,
    to: LocalDate,
    type: OccupancyType
  }
): Promise<OccupancyResponse> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()],
    ['type', request.type.toString()]
  )
  const { data: json } = await client.request<JsonOf<OccupancyResponse>>({
    url: uri`/employee-mobile/occupancy/by-unit/${request.unitId}`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonOccupancyResponse(json)
}


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getOccupancyPeriodsOnGroups
*/
export async function getOccupancyPeriodsOnGroups(
  request: {
    unitId: UUID,
    from: LocalDate,
    to: LocalDate,
    type: OccupancyType
  }
): Promise<OccupancyResponseGroupLevel[]> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()],
    ['type', request.type.toString()]
  )
  const { data: json } = await client.request<JsonOf<OccupancyResponseGroupLevel[]>>({
    url: uri`/employee-mobile/occupancy/by-unit/${request.unitId}/groups`.toString(),
    method: 'GET',
    params
  })
  return json.map(e => deserializeJsonOccupancyResponseGroupLevel(e))
}
