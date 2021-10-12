// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from './client'
import {
  ExternalStaffArrivalRequest,
  ExternalStaffDepartureRequest,
  StaffArrivalRequest,
  StaffAttendanceResponse,
  StaffDepartureRequest
} from 'lib-common/generated/api-types/attendance'

export async function getUnitStaffAttendances(
  unitId: UUID
): Promise<Result<StaffAttendanceResponse>> {
  return client
    .get<JsonOf<StaffAttendanceResponse>>(`/v2/staff-attendances`, {
      params: { unitId }
    })
    .then((res) =>
      Success.of({
        staff: res.data.staff.map((staff) => ({
          ...staff,
          latestCurrentDayAttendance: staff.latestCurrentDayAttendance
            ? {
                ...staff.latestCurrentDayAttendance,
                arrived: new Date(staff.latestCurrentDayAttendance.arrived),
                departed: staff.latestCurrentDayAttendance.departed
                  ? new Date(staff.latestCurrentDayAttendance.departed)
                  : null
              }
            : null
        })),
        extraAttendances: res.data.extraAttendances.map((att) => ({
          ...att,
          arrived: new Date(att.arrived)
        }))
      })
    )
    .catch((e) => Failure.fromError(e))
}

export async function postStaffArrival(
  request: StaffArrivalRequest
): Promise<Result<void>> {
  return client
    .post(`/v2/staff-attendances/arrival`, request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postStaffDeparture(
  attendanceId: UUID,
  request: StaffDepartureRequest
): Promise<Result<void>> {
  return client
    .post(`/v2/staff-attendances/${attendanceId}/departure`, request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postExternalStaffArrival(
  body: ExternalStaffArrivalRequest
): Promise<Result<void>> {
  return client
    .post(`/v2/staff-attendances/arrival-external`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postExternalStaffDeparture(
  body: ExternalStaffDepartureRequest
): Promise<Result<void>> {
  return client
    .post(`/v2/staff-attendances/departure-external`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
