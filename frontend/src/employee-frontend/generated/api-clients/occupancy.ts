// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import LocalDate from 'lib-common/local-date'
import { JsonOf } from 'lib-common/json'
import { OccupancyResponse } from 'lib-common/generated/api-types/occupancy'
import { OccupancyResponseGroupLevel } from 'lib-common/generated/api-types/occupancy'
import { OccupancyResponseSpeculated } from 'lib-common/generated/api-types/occupancy'
import { OccupancyType } from 'lib-common/generated/api-types/occupancy'
import { UUID } from 'lib-common/types'
import { UnitOccupancies } from 'lib-common/generated/api-types/occupancy'
import { client } from '../../api/client'
import { deserializeJsonOccupancyResponse } from 'lib-common/generated/api-types/occupancy'
import { deserializeJsonOccupancyResponseGroupLevel } from 'lib-common/generated/api-types/occupancy'
import { deserializeJsonUnitOccupancies } from 'lib-common/generated/api-types/occupancy'
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
  const { data: json } = await client.request<JsonOf<OccupancyResponse>>({
    url: uri`/occupancy/by-unit/${request.unitId}`.toString(),
    method: 'GET',
    params: {
      from: request.from.formatIso(),
      to: request.to.formatIso(),
      type: request.type
    }
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
  const { data: json } = await client.request<JsonOf<OccupancyResponseGroupLevel[]>>({
    url: uri`/occupancy/by-unit/${request.unitId}/groups`.toString(),
    method: 'GET',
    params: {
      from: request.from.formatIso(),
      to: request.to.formatIso(),
      type: request.type
    }
  })
  return json.map(e => deserializeJsonOccupancyResponseGroupLevel(e))
}


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getOccupancyPeriodsSpeculated
*/
export async function getOccupancyPeriodsSpeculated(
  request: {
    unitId: UUID,
    applicationId: UUID,
    from: LocalDate,
    to: LocalDate,
    preschoolDaycareFrom: LocalDate | null,
    preschoolDaycareTo: LocalDate | null
  }
): Promise<OccupancyResponseSpeculated> {
  const { data: json } = await client.request<JsonOf<OccupancyResponseSpeculated>>({
    url: uri`/occupancy/by-unit/${request.unitId}/speculated/${request.applicationId}`.toString(),
    method: 'GET',
    params: {
      from: request.from.formatIso(),
      to: request.to.formatIso(),
      preschoolDaycareFrom: request.preschoolDaycareFrom?.formatIso(),
      preschoolDaycareTo: request.preschoolDaycareTo?.formatIso()
    }
  })
  return json
}


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getUnitOccupancies
*/
export async function getUnitOccupancies(
  request: {
    unitId: UUID,
    from: LocalDate,
    to: LocalDate
  }
): Promise<UnitOccupancies> {
  const { data: json } = await client.request<JsonOf<UnitOccupancies>>({
    url: uri`/occupancy/units/${request.unitId}`.toString(),
    method: 'GET',
    params: {
      from: request.from.formatIso(),
      to: request.to.formatIso()
    }
  })
  return deserializeJsonUnitOccupancies(json)
}
