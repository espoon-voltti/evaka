// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { AbsenceRangeRequest } from 'lib-common/generated/api-types/attendance'
import { ArrivalRequest } from 'lib-common/generated/api-types/attendance'
import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { ChildAttendanceStatusResponse } from 'lib-common/generated/api-types/attendance'
import { CurrentDayStaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { DepartureRequest } from 'lib-common/generated/api-types/attendance'
import { ExpectedAbsencesOnDepartureRequest } from 'lib-common/generated/api-types/attendance'
import { ExpectedAbsencesOnDepartureResponse } from 'lib-common/generated/api-types/attendance'
import { ExternalStaffArrivalRequest } from 'lib-common/generated/api-types/attendance'
import { ExternalStaffDepartureRequest } from 'lib-common/generated/api-types/attendance'
import { FullDayAbsenceRequest } from 'lib-common/generated/api-types/attendance'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { StaffArrivalRequest } from 'lib-common/generated/api-types/attendance'
import { StaffAttendanceUpdateRequest } from 'lib-common/generated/api-types/attendance'
import { StaffAttendanceUpdateResponse } from 'lib-common/generated/api-types/attendance'
import { StaffDepartureRequest } from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { UnitInfo } from 'lib-common/generated/api-types/attendance'
import { UnitStats } from 'lib-common/generated/api-types/attendance'
import { client } from '../../client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAttendanceChild } from 'lib-common/generated/api-types/attendance'
import { deserializeJsonChildAttendanceStatusResponse } from 'lib-common/generated/api-types/attendance'
import { deserializeJsonCurrentDayStaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.cancelFullDayAbsence
*/
export async function cancelFullDayAbsence(
  request: {
    unitId: UUID,
    childId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/full-day-absence`.toString(),
    method: 'DELETE'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.deleteAbsenceRange
*/
export async function deleteAbsenceRange(
  request: {
    unitId: UUID,
    childId: UUID,
    from: LocalDate,
    to: LocalDate
  }
): Promise<void> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/absence-range`.toString(),
    method: 'DELETE',
    params
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.getAttendanceStatuses
*/
export async function getAttendanceStatuses(
  request: {
    unitId: UUID
  }
): Promise<Record<UUID, ChildAttendanceStatusResponse>> {
  const { data: json } = await client.request<JsonOf<Record<UUID, ChildAttendanceStatusResponse>>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/attendances`.toString(),
    method: 'GET'
  })
  return Object.fromEntries(Object.entries(json).map(
    ([k, v]) => [k, deserializeJsonChildAttendanceStatusResponse(v)]
  ))
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.getChildExpectedAbsencesOnDeparture
*/
export async function getChildExpectedAbsencesOnDeparture(
  request: {
    unitId: UUID,
    childId: UUID,
    body: ExpectedAbsencesOnDepartureRequest
  }
): Promise<ExpectedAbsencesOnDepartureResponse> {
  const { data: json } = await client.request<JsonOf<ExpectedAbsencesOnDepartureResponse>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/departure/expected-absences`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ExpectedAbsencesOnDepartureRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.getChildren
*/
export async function getChildren(
  request: {
    unitId: UUID
  }
): Promise<AttendanceChild[]> {
  const { data: json } = await client.request<JsonOf<AttendanceChild[]>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAttendanceChild(e))
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postAbsenceRange
*/
export async function postAbsenceRange(
  request: {
    unitId: UUID,
    childId: UUID,
    body: AbsenceRangeRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/absence-range`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<AbsenceRangeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postArrival
*/
export async function postArrival(
  request: {
    unitId: UUID,
    childId: UUID,
    body: ArrivalRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/arrival`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ArrivalRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postDeparture
*/
export async function postDeparture(
  request: {
    unitId: UUID,
    childId: UUID,
    body: DepartureRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/departure`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DepartureRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postFullDayAbsence
*/
export async function postFullDayAbsence(
  request: {
    unitId: UUID,
    childId: UUID,
    body: FullDayAbsenceRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/full-day-absence`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<FullDayAbsenceRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.returnToComing
*/
export async function returnToComing(
  request: {
    unitId: UUID,
    childId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/return-to-coming`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.returnToPresent
*/
export async function returnToPresent(
  request: {
    unitId: UUID,
    childId: UUID
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/return-to-present`.toString(),
    method: 'POST'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.getAttendancesByUnit
*/
export async function getAttendancesByUnit(
  request: {
    unitId: UUID,
    date?: LocalDate | null
  }
): Promise<CurrentDayStaffAttendanceResponse> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['date', request.date?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<CurrentDayStaffAttendanceResponse>>({
    url: uri`/employee-mobile/realtime-staff-attendances`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonCurrentDayStaffAttendanceResponse(json)
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.markArrival
*/
export async function markArrival(
  request: {
    body: StaffArrivalRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/realtime-staff-attendances/arrival`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<StaffArrivalRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.markDeparture
*/
export async function markDeparture(
  request: {
    body: StaffDepartureRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/realtime-staff-attendances/departure`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<StaffDepartureRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.markExternalArrival
*/
export async function markExternalArrival(
  request: {
    body: ExternalStaffArrivalRequest
  }
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee-mobile/realtime-staff-attendances/arrival-external`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ExternalStaffArrivalRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.markExternalDeparture
*/
export async function markExternalDeparture(
  request: {
    body: ExternalStaffDepartureRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/realtime-staff-attendances/departure-external`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ExternalStaffDepartureRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.setAttendances
*/
export async function setAttendances(
  request: {
    unitId: UUID,
    body: StaffAttendanceUpdateRequest
  }
): Promise<StaffAttendanceUpdateResponse> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId]
  )
  const { data: json } = await client.request<JsonOf<StaffAttendanceUpdateResponse>>({
    url: uri`/employee-mobile/realtime-staff-attendances`.toString(),
    method: 'PUT',
    params,
    data: request.body satisfies JsonCompatible<StaffAttendanceUpdateRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.MobileUnitController.getUnitInfo
*/
export async function getUnitInfo(
  request: {
    unitId: UUID
  }
): Promise<UnitInfo> {
  const { data: json } = await client.request<JsonOf<UnitInfo>>({
    url: uri`/employee-mobile/units/${request.unitId}`.toString(),
    method: 'GET'
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.MobileUnitController.getUnitStats
*/
export async function getUnitStats(
  request: {
    unitIds?: UUID[] | null
  }
): Promise<UnitStats[]> {
  const params = createUrlSearchParams(
    ...(request.unitIds?.map((e): [string, string | null | undefined] => ['unitIds', e]) ?? [])
  )
  const { data: json } = await client.request<JsonOf<UnitStats[]>>({
    url: uri`/employee-mobile/units/stats`.toString(),
    method: 'GET',
    params
  })
  return json
}
