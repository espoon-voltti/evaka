// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  CurrentDayStaffAttendanceResponse,
  ExternalStaffArrivalRequest,
  ExternalStaffDepartureRequest,
  StaffArrivalRequest,
  StaffAttendanceUpdateRequest,
  StaffDepartureRequest
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getUnitStaffAttendances(
  unitId: UUID
): Promise<CurrentDayStaffAttendanceResponse> {
  return client
    .get<JsonOf<CurrentDayStaffAttendanceResponse>>(
      `/mobile/realtime-staff-attendances`,
      {
        params: { unitId }
      }
    )
    .then((res) => ({
      ...res.data,
      staff: res.data.staff.map((staff) => ({
        ...staff,
        latestCurrentDayAttendance: staff.latestCurrentDayAttendance
          ? {
              ...staff.latestCurrentDayAttendance,
              arrived: HelsinkiDateTime.parseIso(
                staff.latestCurrentDayAttendance.arrived
              ),
              departed: staff.latestCurrentDayAttendance.departed
                ? HelsinkiDateTime.parseIso(
                    staff.latestCurrentDayAttendance.departed
                  )
                : null
            }
          : null,
        plannedAttendances: staff.plannedAttendances.map(
          ({ start, end, ...plan }) => ({
            ...plan,
            start: HelsinkiDateTime.parseIso(start),
            end: HelsinkiDateTime.parseIso(end)
          })
        ),
        attendances: staff.attendances.map(
          ({ arrived, departed, ...attendance }) => ({
            ...attendance,
            arrived: HelsinkiDateTime.parseIso(arrived),
            departed: departed ? HelsinkiDateTime.parseIso(departed) : null
          })
        ),
        spanningPlan: staff.spanningPlan
          ? {
              start: HelsinkiDateTime.parseIso(staff.spanningPlan.start),
              end: HelsinkiDateTime.parseIso(staff.spanningPlan.end)
            }
          : null
      })),
      extraAttendances: res.data.extraAttendances.map((att) => ({
        ...att,
        arrived: HelsinkiDateTime.parseIso(att.arrived)
      }))
    }))
}

export async function postStaffArrival(
  request: StaffArrivalRequest
): Promise<void> {
  return client
    .post(`/mobile/realtime-staff-attendances/arrival`, request)
    .then(() => undefined)
}

export async function postStaffDeparture(
  request: StaffDepartureRequest
): Promise<void> {
  return client
    .post(`/mobile/realtime-staff-attendances/departure`, request)
    .then(() => undefined)
}

export async function postExternalStaffArrival(
  body: ExternalStaffArrivalRequest
): Promise<void> {
  return client
    .post(`/mobile/realtime-staff-attendances/arrival-external`, body)
    .then(() => undefined)
}

export async function postExternalStaffDeparture(
  body: ExternalStaffDepartureRequest
): Promise<void> {
  return client
    .post(`/mobile/realtime-staff-attendances/departure-external`, body)
    .then(() => undefined)
}

export async function putStaffAttendances(
  unitId: UUID,
  body: StaffAttendanceUpdateRequest
): Promise<void> {
  return client
    .put(`/mobile/realtime-staff-attendances`, body, { params: { unitId } })
    .then(() => undefined)
}
