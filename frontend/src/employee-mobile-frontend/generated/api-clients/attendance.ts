// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AbsenceRangeRequest } from 'lib-common/generated/api-types/attendance'
import type { ArrivalsRequest } from 'lib-common/generated/api-types/attendance'
import type { AttendanceChild } from 'lib-common/generated/api-types/attendance'
import type { ChildAttendanceStatusResponse } from 'lib-common/generated/api-types/attendance'
import type { CurrentDayStaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { DeparturesRequest } from 'lib-common/generated/api-types/attendance'
import type { EmployeeId } from 'lib-common/generated/api-types/shared'
import type { ExpectedAbsencesOnDeparturesRequest } from 'lib-common/generated/api-types/attendance'
import type { ExpectedAbsencesOnDeparturesResponse } from 'lib-common/generated/api-types/attendance'
import type { ExternalStaffArrivalRequest } from 'lib-common/generated/api-types/attendance'
import type { ExternalStaffDepartureRequest } from 'lib-common/generated/api-types/attendance'
import type { FullDayAbsenceRequest } from 'lib-common/generated/api-types/attendance'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { OpenGroupAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import type { StaffArrivalRequest } from 'lib-common/generated/api-types/attendance'
import type { StaffAttendanceExternalId } from 'lib-common/generated/api-types/shared'
import type { StaffAttendanceUpdateRequest } from 'lib-common/generated/api-types/attendance'
import type { StaffAttendanceUpdateResponse } from 'lib-common/generated/api-types/attendance'
import type { StaffDepartureRequest } from 'lib-common/generated/api-types/attendance'
import type { StaffMember } from 'lib-common/generated/api-types/attendance'
import type { UnitInfo } from 'lib-common/generated/api-types/attendance'
import type { UnitStats } from 'lib-common/generated/api-types/attendance'
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
    unitId: DaycareId,
    childId: PersonId
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
    unitId: DaycareId,
    childId: PersonId,
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
    unitId: DaycareId
  }
): Promise<Partial<Record<PersonId, ChildAttendanceStatusResponse>>> {
  const { data: json } = await client.request<JsonOf<Partial<Record<PersonId, ChildAttendanceStatusResponse>>>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/attendances`.toString(),
    method: 'GET'
  })
  return Object.fromEntries(Object.entries(json).map(
    ([k, v]) => [k, v !== undefined ? deserializeJsonChildAttendanceStatusResponse(v) : v]
  ))
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.getChildren
*/
export async function getChildren(
  request: {
    unitId: DaycareId
  }
): Promise<AttendanceChild[]> {
  const { data: json } = await client.request<JsonOf<AttendanceChild[]>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/children`.toString(),
    method: 'GET'
  })
  return json.map(e => deserializeJsonAttendanceChild(e))
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.getExpectedAbsencesOnDepartures
*/
export async function getExpectedAbsencesOnDepartures(
  request: {
    unitId: DaycareId,
    body: ExpectedAbsencesOnDeparturesRequest
  }
): Promise<ExpectedAbsencesOnDeparturesResponse> {
  const { data: json } = await client.request<JsonOf<ExpectedAbsencesOnDeparturesResponse>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/departure/expected-absences`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ExpectedAbsencesOnDeparturesRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postAbsenceRange
*/
export async function postAbsenceRange(
  request: {
    unitId: DaycareId,
    childId: PersonId,
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
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postArrivals
*/
export async function postArrivals(
  request: {
    unitId: DaycareId,
    body: ArrivalsRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/arrivals`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ArrivalsRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postDepartures
*/
export async function postDepartures(
  request: {
    unitId: DaycareId,
    body: DeparturesRequest
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee-mobile/attendances/units/${request.unitId}/departures`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<DeparturesRequest>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.ChildAttendanceController.postFullDayAbsence
*/
export async function postFullDayAbsence(
  request: {
    unitId: DaycareId,
    childId: PersonId,
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
    unitId: DaycareId,
    childId: PersonId
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
    unitId: DaycareId,
    childId: PersonId
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
    unitId: DaycareId,
    startDate?: LocalDate | null,
    endDate?: LocalDate | null
  }
): Promise<CurrentDayStaffAttendanceResponse> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['startDate', request.startDate?.formatIso()],
    ['endDate', request.endDate?.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<CurrentDayStaffAttendanceResponse>>({
    url: uri`/employee-mobile/realtime-staff-attendances`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonCurrentDayStaffAttendanceResponse(json)
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.getEmployeeAttendances
*/
export async function getEmployeeAttendances(
  request: {
    unitId: DaycareId,
    employeeId: EmployeeId,
    from: LocalDate,
    to: LocalDate
  }
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
    params
  })
  return deserializeJsonStaffMember(json)
}


/**
* Generated from fi.espoo.evaka.attendance.MobileRealtimeStaffAttendanceController.getOpenGroupAttendance
*/
export async function getOpenGroupAttendance(
  request: {
    userId: EmployeeId
  }
): Promise<OpenGroupAttendanceResponse> {
  const params = createUrlSearchParams(
    ['userId', request.userId]
  )
  const { data: json } = await client.request<JsonOf<OpenGroupAttendanceResponse>>({
    url: uri`/employee-mobile/realtime-staff-attendances/open-attendance`.toString(),
    method: 'GET',
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
): Promise<StaffAttendanceExternalId> {
  const { data: json } = await client.request<JsonOf<StaffAttendanceExternalId>>({
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
    unitId: DaycareId,
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
    unitId: DaycareId
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
    unitIds?: DaycareId[] | null
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
