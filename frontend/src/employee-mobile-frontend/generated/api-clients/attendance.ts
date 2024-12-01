// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { AbsenceRangeRequest } from 'lib-common/generated/api-types/attendance'
import { ArrivalsRequest } from 'lib-common/generated/api-types/attendance'
import { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import { AxiosHeaders } from 'axios'
import { ChildAttendanceStatusResponse } from 'lib-common/generated/api-types/attendance'
import { CurrentDayStaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { DeparturesRequest } from 'lib-common/generated/api-types/attendance'
import { ExpectedAbsencesOnDeparturesRequest } from 'lib-common/generated/api-types/attendance'
import { ExpectedAbsencesOnDeparturesResponse } from 'lib-common/generated/api-types/attendance'
import { ExternalStaffArrivalRequest } from 'lib-common/generated/api-types/attendance'
import { ExternalStaffDepartureRequest } from 'lib-common/generated/api-types/attendance'
import { FullDayAbsenceRequest } from 'lib-common/generated/api-types/attendance'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { OpenGroupAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { StaffArrivalRequest } from 'lib-common/generated/api-types/attendance'
import { StaffAttendanceUpdateRequest } from 'lib-common/generated/api-types/attendance'
import { StaffAttendanceUpdateResponse } from 'lib-common/generated/api-types/attendance'
import { StaffDepartureRequest } from 'lib-common/generated/api-types/attendance'
import { StaffMember } from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
import { UnitInfo } from 'lib-common/generated/api-types/attendance'
import { UnitStats } from 'lib-common/generated/api-types/attendance'
import { client } from '../../client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonAttendanceChild } from 'lib-common/generated/api-types/attendance'
import { deserializeJsonChildAttendanceStatusResponse } from 'lib-common/generated/api-types/attendance'
import { deserializeJsonCurrentDayStaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { deserializeJsonOpenGroupAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { deserializeJsonStaffMember } from 'lib-common/generated/api-types/attendance'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.cancelFullDayAbsence
*/
export async function cancelFullDayAbsence(
  request: {
    unitId: UUID,
    childId: UUID
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/full-day-absence`.toString(),
    method: 'DELETE',
    headers
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const params = createUrlSearchParams(
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/absence-range`.toString(),
    method: 'DELETE',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<Record<UUID, ChildAttendanceStatusResponse>> {
  const { data: json } = await client.request<JsonOf<Record<UUID, ChildAttendanceStatusResponse>>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/attendances`.toString(),
    method: 'GET',
    headers
  })
  return Object.fromEntries(Object.entries(json).map(
    ([k, v]) => [k, deserializeJsonChildAttendanceStatusResponse(v)]
  ))
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.getChildren
*/
export async function getChildren(
  request: {
    unitId: UUID
  },
  headers?: AxiosHeaders
): Promise<AttendanceChild[]> {
  const { data: json } = await client.request<JsonOf<AttendanceChild[]>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children`.toString(),
    method: 'GET',
    headers
  })
  return json.map(e => deserializeJsonAttendanceChild(e))
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.getExpectedAbsencesOnDepartures
*/
export async function getExpectedAbsencesOnDepartures(
  request: {
    unitId: UUID,
    body: ExpectedAbsencesOnDeparturesRequest
  },
  headers?: AxiosHeaders
): Promise<ExpectedAbsencesOnDeparturesResponse> {
  const { data: json } = await client.request<JsonOf<ExpectedAbsencesOnDeparturesResponse>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/departure/expected-absences`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<ExpectedAbsencesOnDeparturesRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postAbsenceRange
*/
export async function postAbsenceRange(
  request: {
    unitId: UUID,
    childId: UUID,
    body: AbsenceRangeRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/absence-range`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<AbsenceRangeRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postArrivals
*/
export async function postArrivals(
  request: {
    unitId: UUID,
    body: ArrivalsRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/arrivals`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<ArrivalsRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postDepartures
*/
export async function postDepartures(
  request: {
    unitId: UUID,
    body: DeparturesRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/departures`.toString(),
    method: 'POST',
    headers,
    data: request.body satisfies JsonCompatible<DeparturesRequest>
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/full-day-absence`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/return-to-coming`.toString(),
    method: 'POST',
    headers
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children/${request.childId}/return-to-present`.toString(),
    method: 'POST',
    headers
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
  },
  headers?: AxiosHeaders
): Promise<CurrentDayStaffAttendanceResponse> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['date', request.date?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<CurrentDayStaffAttendanceResponse>>({
    url: uri`/employee-mobile/realtime-staff-attendances`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonCurrentDayStaffAttendanceResponse(json)
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.getEmployeeAttendances
*/
export async function getEmployeeAttendances(
  request: {
    unitId: UUID,
    employeeId: UUID,
    from: LocalDate,
    to: LocalDate
  },
  headers?: AxiosHeaders
): Promise<StaffMember> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['employeeId', request.employeeId],
    ['from', request.from.formatIso()],
    ['to', request.to.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<StaffMember>>({
    url: uri`/employee-mobile/realtime-staff-attendances/employee`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonStaffMember(json)
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.getOpenGroupAttendance
*/
export async function getOpenGroupAttendance(
  request: {
    userId: UUID
  },
  headers?: AxiosHeaders
): Promise<OpenGroupAttendanceResponse> {
  const params = createUrlSearchParams(
    ['userId', request.userId]
  )
  const { data: json } = await client.request<JsonOf<OpenGroupAttendanceResponse>>({
    url: uri`/employee-mobile/realtime-staff-attendances/open-attendance`.toString(),
    method: 'GET',
    headers,
    params
  })
  return deserializeJsonOpenGroupAttendanceResponse(json)
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.markArrival
*/
export async function markArrival(
  request: {
    body: StaffArrivalRequest
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/realtime-staff-attendances/arrival`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/realtime-staff-attendances/departure`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<UUID> {
  const { data: json } = await client.request<JsonOf<UUID>>({
    url: uri`/employee-mobile/realtime-staff-attendances/arrival-external`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/realtime-staff-attendances/departure-external`.toString(),
    method: 'POST',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<StaffAttendanceUpdateResponse> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId]
  )
  const { data: json } = await client.request<JsonOf<StaffAttendanceUpdateResponse>>({
    url: uri`/employee-mobile/realtime-staff-attendances`.toString(),
    method: 'PUT',
    headers,
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
  },
  headers?: AxiosHeaders
): Promise<UnitInfo> {
  const { data: json } = await client.request<JsonOf<UnitInfo>>({
    url: uri`/employee-mobile/units/${request.unitId}`.toString(),
    method: 'GET',
    headers
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.MobileUnitController.getUnitStats
*/
export async function getUnitStats(
  request: {
    unitIds?: UUID[] | null
  },
  headers?: AxiosHeaders
): Promise<UnitStats[]> {
  const params = createUrlSearchParams(
    ...(request.unitIds?.map((e): [string, string | null | undefined] => ['unitIds', e]) ?? [])
  )
  const { data: json } = await client.request<JsonOf<UnitStats[]>>({
    url: uri`/employee-mobile/units/stats`.toString(),
    method: 'GET',
    headers,
    params
  })
  return json
}
