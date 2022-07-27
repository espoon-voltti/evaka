// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
  EmployeeAttendance,
  ExternalAttendance,
  SingleDayStaffAttendanceUpsert,
  StaffAttendanceResponse,
  UpsertStaffAndExternalAttendanceRequest
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import type { JsonOf } from 'lib-common/json'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import { client } from './client'

const mapExternalAttendance = ({
  arrived,
  departed,
  ...rest
}: JsonOf<ExternalAttendance>): ExternalAttendance => ({
  ...rest,
  arrived: HelsinkiDateTime.parseIso(arrived),
  departed: departed ? HelsinkiDateTime.parseIso(departed) : null
})

const mapEmployeeAttendance = ({
  attendances,
  ...rest
}: JsonOf<EmployeeAttendance>): EmployeeAttendance => ({
  ...rest,
  attendances: attendances.map(({ arrived, departed, ...rest }) => ({
    ...rest,
    arrived: HelsinkiDateTime.parseIso(arrived),
    departed: departed ? HelsinkiDateTime.parseIso(departed) : null
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

export function postStaffAndExternalAttendances(
  unitId: UUID,
  body: UpsertStaffAndExternalAttendanceRequest
): Promise<Result<void>> {
  return client
    .post(`/staff-attendances/realtime/${unitId}/upsert`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function postSingleDayStaffAttendances(
  unitId: UUID,
  employeeId: UUID,
  date: LocalDate,
  body: SingleDayStaffAttendanceUpsert[]
): Promise<Result<void>> {
  return client
    .post(
      `/staff-attendances/realtime/${unitId}/${employeeId}/${date.formatIso()}`,
      body
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function deleteStaffAttendance(
  unitId: UUID,
  attendanceId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/staff-attendances/realtime/${unitId}/${attendanceId}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function deleteExternalStaffAttendance(
  unitId: UUID,
  attendanceId: UUID
): Promise<Result<void>> {
  return client
    .delete(`/staff-attendances/realtime/${unitId}/external/${attendanceId}`)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
