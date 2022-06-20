// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  CurrentDayStaffAttendanceResponse,
  ExternalStaffArrivalRequest,
  ExternalStaffDepartureRequest,
  StaffArrivalRequest,
  StaffDepartureRequest
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from './client'

export async function getUnitStaffAttendances(
  unitId: UUID
): Promise<Result<CurrentDayStaffAttendanceResponse>> {
  return client
    .get<JsonOf<CurrentDayStaffAttendanceResponse>>(
      `/mobile/realtime-staff-attendances`,
      {
        params: { unitId }
      }
    )
    .then((res) =>
      Success.of({
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
          )
        })),
        extraAttendances: res.data.extraAttendances.map((att) => ({
          ...att,
          arrived: HelsinkiDateTime.parseIso(att.arrived)
        }))
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function postStaffArrival(
  request: StaffArrivalRequest
): Promise<Result<void>> {
  return client
    .post(`/mobile/realtime-staff-attendances/arrival`, request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postStaffDeparture(
  attendanceId: UUID,
  request: StaffDepartureRequest
): Promise<Result<void>> {
  return client
    .post(
      `/mobile/realtime-staff-attendances/${attendanceId}/departure`,
      request
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postExternalStaffArrival(
  body: ExternalStaffArrivalRequest
): Promise<Result<void>> {
  return client
    .post(`/mobile/realtime-staff-attendances/arrival-external`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postExternalStaffDeparture(
  body: ExternalStaffDepartureRequest
): Promise<Result<void>> {
  return client
    .post(`/mobile/realtime-staff-attendances/departure-external`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
