// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  EmployeeAttendance,
  ExternalAttendance,
  StaffAttendanceResponse,
  UpdateExternalAttendanceRequest,
  UpdateStaffAttendanceRequest
} from 'lib-common/generated/api-types/attendance'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

import { client } from './client'

const mapExternalAttendance = ({
  arrived,
  departed,
  ...rest
}: JsonOf<ExternalAttendance>): ExternalAttendance => ({
  ...rest,
  arrived: new Date(arrived),
  departed: departed ? new Date(departed) : null
})

const mapEmployeeAttendance = ({
  attendances,
  ...rest
}: JsonOf<EmployeeAttendance>): EmployeeAttendance => ({
  ...rest,
  attendances: attendances.map(({ arrived, departed, ...rest }) => ({
    ...rest,
    arrived: new Date(arrived),
    departed: departed ? new Date(departed) : null
  }))
})

export function getStaffAttendances(
  unitId: UUID,
  range: FiniteDateRange
): Promise<Result<StaffAttendanceResponse>> {
  return client
    .get<JsonOf<StaffAttendanceResponse>>('/staff-attendances/realtime', {
      params: {
        unitId,
        start: range.start.formatIso(),
        end: range.end.formatIso()
      }
    })
    .then(({ data: { extraAttendances, staff } }) => ({
      extraAttendances: extraAttendances.map(mapExternalAttendance),
      staff: staff.map(mapEmployeeAttendance)
    }))
    .then((data) => Success.of(data))
    .catch((e) => Failure.fromError(e))
}

export function postUpdateStaffAttendance(
  unitId: UUID,
  body: UpdateStaffAttendanceRequest
): Promise<Result<void>> {
  return client
    .post(`/staff-attendances/realtime/${unitId}/update`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function postUpdateExternalAttendance(
  unitId: UUID,
  body: UpdateExternalAttendanceRequest
): Promise<Result<void>> {
  return client
    .post(`/staff-attendances/realtime/${unitId}/update-external`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
