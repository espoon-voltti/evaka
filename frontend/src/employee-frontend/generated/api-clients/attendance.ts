// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { DaycareId } from 'lib-common/generated/api-types/shared'
import type { EmployeeId } from 'lib-common/generated/api-types/shared'
import type { ExternalAttendanceBody } from 'lib-common/generated/api-types/attendance'
import type { JsonCompatible } from 'lib-common/json'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { OpenGroupAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import type { StaffAttendanceBody } from 'lib-common/generated/api-types/attendance'
import type { StaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { client } from '../../api/client'
import { createUrlSearchParams } from 'lib-common/api'
import { deserializeJsonOpenGroupAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { deserializeJsonStaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.getOpenGroupAttendance
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
    url: uri`/employee/staff-attendances/realtime/open-attendance`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonOpenGroupAttendanceResponse(json)
}


/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.getRealtimeStaffAttendances
*/
export async function getRealtimeStaffAttendances(
  request: {
    unitId: DaycareId,
    start: LocalDate,
    end: LocalDate
  }
): Promise<StaffAttendanceResponse> {
  const params = createUrlSearchParams(
    ['unitId', request.unitId],
    ['start', request.start.formatIso()],
    ['end', request.end.formatIso()]
  )
  const { data: json } = await client.request<JsonOf<StaffAttendanceResponse>>({
    url: uri`/employee/staff-attendances/realtime`.toString(),
    method: 'GET',
    params
  })
  return deserializeJsonStaffAttendanceResponse(json)
}


/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.upsertDailyExternalRealtimeAttendances
*/
export async function upsertDailyExternalRealtimeAttendances(
  request: {
    body: ExternalAttendanceBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/staff-attendances/realtime/upsert-external`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<ExternalAttendanceBody>
  })
  return json
}


/**
* Generated from fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.upsertDailyStaffRealtimeAttendances
*/
export async function upsertDailyStaffRealtimeAttendances(
  request: {
    body: StaffAttendanceBody
  }
): Promise<void> {
  const { data: json } = await client.request<JsonOf<void>>({
    url: uri`/employee/staff-attendances/realtime/upsert`.toString(),
    method: 'POST',
    data: request.body satisfies JsonCompatible<StaffAttendanceBody>
  })
  return json
}
