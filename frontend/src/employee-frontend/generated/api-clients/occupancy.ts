// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import type { AttendanceReservationReportRow } from 'lib-common/generated/api-types/reports'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { GetUnitOccupanciesForDayBody } from 'lib-common/generated/api-types/occupancy'
import type { GroupId } from 'lib-common/generated/api-types/shared'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { OccupancyResponseSpeculated } from 'lib-common/generated/api-types/occupancy'
import type { RealtimeOccupancy } from 'lib-common/generated/api-types/occupancy'
import type { UnitOccupancies } from 'lib-common/generated/api-types/occupancy'
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
    unitId: DaycareId,
    applicationId: ApplicationId,
    from: LocalDate,
    to: LocalDate,
    preschoolDaycareFrom?: LocalDate | null,
    preschoolDaycareTo?: LocalDate | null
  }
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
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getUnitOccupancies
*/
export async function getUnitOccupancies(
  request: {
    unitId: DaycareId,
    from: LocalDate,
    to: LocalDate,
    groupId?: GroupId | null
  }
): Promise<UnitOccupancies> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()],
    ['groupId', request.groupId]
  )
  const { data: json } = await client.request<JsonOf<UnitOccupancies>>({
    url: uri`/employee/occupancy/units/${request.unitId}`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonUnitOccupancies(json)
}


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getUnitPlannedOccupanciesForDay
*/
export async function getUnitPlannedOccupanciesForDay(
  request: {
    unitId: DaycareId,
    body: GetUnitOccupanciesForDayBody
  }
): Promise<AttendanceReservationReportRow[]> {
  const { data: json } = await client.request<JsonOf<AttendanceReservationReportRow[]>>({
    url: uri`/employee/occupancy/units/${request.unitId}/day/planned`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<GetUnitOccupanciesForDayBody>
  })
  return json.map(e => deserializeJsonAttendanceReservationReportRow(e))
}


/**
* Generated from fi.espoo.evaka.occupancy.OccupancyController.getUnitRealizedOccupanciesForDay
*/
export async function getUnitRealizedOccupanciesForDay(
  request: {
    unitId: DaycareId,
    body: GetUnitOccupanciesForDayBody
  }
): Promise<RealtimeOccupancy> {
  const { data: json } = await client.request<JsonOf<RealtimeOccupancy>>({
    url: uri`/employee/occupancy/units/${request.unitId}/day/realized`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<GetUnitOccupanciesForDayBody>
  })
  return deserializeJsonRealtimeOccupancy(json)
}
