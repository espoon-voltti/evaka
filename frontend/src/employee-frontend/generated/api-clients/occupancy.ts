// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { AttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import { AxiosHeaders } from 'axios'
import { GetUnitOccupanciesForDayBody } from 'lib-common/generated/api-types/occupancy'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { OccupancyResponseSpeculated } from 'lib-common/generated/api-types/occupancy'
import { RealtimeOccupancy } from 'lib-common/generated/api-types/occupancy'
import { UUID } from 'lib-common/types'
import { UnitOccupancies } from 'lib-common/generated/api-types/occupancy'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import { deserializeJsonRealtimeOccupancy } from 'lib-common/generated/api-types/occupancy'
import { deserializeJsonUnitOccupancies } from 'lib-common/generated/api-types/occupancy'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getOccupancyPeriodsSpeculated
*/
export async function getOccupancyPeriodsSpeculated(
  request: {
    unitId: UUID,
    applicationId: UUID,
    from: LocalDate,
    to: LocalDate,
    preschoolDaycareFrom?: LocalDate | null,
    preschoolDaycareTo?: LocalDate | null
  },
  headers?: AxiosHeaders
): Promise<OccupancyResponseSpeculated> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()],
    ['preschoolDaycareFrom', request.preschoolDaycareFrom?.formatIso()],
    ['preschoolDaycareTo', request.preschoolDaycareTo?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<OccupancyResponseSpeculated>>({
    url: uri`/employee/occupancy/by-unit/${request.unitId}/speculated/${request.applicationId}`.toString(),
    method: 'GET',
    headers,
    params
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
    to: LocalDate,
    groupId?: UUID | null
  },
  headers?: AxiosHeaders
): Promise<UnitOccupancies> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()],
    ['groupId', request.groupId]
  )
  const { data: json } = await client.request<JsonOf<UnitOccupancies>>({
    url: uri`/employee/occupancy/units/${request.unitId}`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonUnitOccupancies(json)
}


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getUnitPlannedOccupanciesForDay
*/
export async function getUnitPlannedOccupanciesForDay(
  request: {
    unitId: UUID,
    body: GetUnitOccupanciesForDayBody
  },
  headers?: AxiosHeaders
): Promise<AttendanceReservationReportRow[]> {
  const { data: json } = await client.request<JsonOf<AttendanceReservationReportRow[]>>({
    url: uri`/employee/occupancy/units/${request.unitId}/day/planned`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<GetUnitOccupanciesForDayBody>
  })
  return json.map(e => deserializeJsonAttendanceReservationReportRow(e))
}


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getUnitRealizedOccupanciesForDay
*/
export async function getUnitRealizedOccupanciesForDay(
  request: {
    unitId: UUID,
    body: GetUnitOccupanciesForDayBody
  },
  headers?: AxiosHeaders
): Promise<RealtimeOccupancy> {
  const { data: json } = await client.request<JsonOf<RealtimeOccupancy>>({
    url: uri`/employee/occupancy/units/${request.unitId}/day/realized`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<GetUnitOccupanciesForDayBody>
  })
  return deserializeJsonRealtimeOccupancy(json)
}
