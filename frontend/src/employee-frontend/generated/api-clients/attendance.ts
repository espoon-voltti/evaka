// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import LocalDate from 'lib-common/local-date'
import { ExternalAttendanceBody } from 'lib-common/generated/api-types/attendance'
import { JsonCompatible } from 'lib-common/json'
import { JsonOf } from 'lib-common/json'
import { OpenGroupAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { StaffAttendanceBody } from 'lib-common/generated/api-types/attendance'
import { StaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'
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
    userId: UUID,
    unitId: UUID
  }
): Promise<OpenGroupAttendanceResponse> {
  const params = createUrlSearchParams(
    ['userId', request.userId],
    ['unitId', request.unitId]
  )
  const { data: json } = await client.request<JsonOf<OpenGroupAttendanceResponse>>({
    url: uri`/employee/staff-attendances/realtime/open-attendence`.toString(),
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
    unitId: UUID,
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
