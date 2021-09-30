// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { client } from './client'
import {
  StaffArrivalRequest,
  StaffAttendanceResponse,
  StaffDepartureRequest
} from 'lib-common/generated/api-types/attendance'

export async function getUnitStaffAttendances(
  unitId: UUID
): Promise<Result<StaffAttendanceResponse>> {
  return client
    .get<JsonOf<StaffAttendanceResponse>>(
      `/v2/units/${unitId}/staff-attendances`
    )
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
  unitId: UUID,
  request: StaffArrivalRequest
): Promise<Result<void>> {
  return client
    .post(`/v2/units/${unitId}/staff-attendances/arrivals`, request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postStaffDeparture(
  unitId: UUID,
  request: StaffDepartureRequest
): Promise<Result<void>> {
  return client
    .post(`/v2/units/${unitId}/staff-attendances/departures`, request)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
