// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  EmployeeAttendance,
  ExternalAttendance,
  StaffAttendanceResponse,
  UpsertStaffAndExternalAttendanceRequest,
  StaffAttendanceBody,
  ExternalAttendanceBody
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'

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
  plannedAttendances,
  ...rest
}: JsonOf<EmployeeAttendance>): EmployeeAttendance => ({
  ...rest,
  attendances: attendances.map(({ arrived, departed, ...rest }) => ({
    ...rest,
    arrived: HelsinkiDateTime.parseIso(arrived),
    departed: departed ? HelsinkiDateTime.parseIso(departed) : null
  })),
  plannedAttendances: plannedAttendances.map(({ start, end, ...rest }) => ({
    ...rest,
    start: HelsinkiDateTime.parseIso(start),
    end: HelsinkiDateTime.parseIso(end)
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

export function upsertStaffAttendances(
  body: StaffAttendanceBody
): Promise<Result<void>> {
  return client
    .post(`/staff-attendances/realtime/upsert`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function upsertExternalAttendances(
  body: ExternalAttendanceBody
): Promise<Result<void>> {
  return client
    .post(`/staff-attendances/realtime/upsert-external`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}
