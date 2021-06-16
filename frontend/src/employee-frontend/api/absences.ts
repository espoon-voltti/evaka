// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from './client'
import { Failure, Result, Success, Response } from 'lib-common/api'
import {
  StaffAttendanceGroup,
  StaffAttendance
} from 'lib-common/api-types/staffAttendances'
import {
  Group,
  AbsencePayload,
  deserializeChild,
  AbsenceUpdatePayload
} from '../types/absence'
import { UUID } from '../types'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'

interface SearchParams {
  year: number
  month: number
}

export async function getGroupAbsences(
  groupId: UUID,
  params: SearchParams
): Promise<Result<Group>> {
  return client
    .get<JsonOf<Response<Group>>>(`/absences/${groupId}`, { params })
    .then((res) => res.data.data)
    .then((data) => ({
      ...data,
      children: data.children.map(deserializeChild).sort((childA, childB) => {
        const lastNameCmp = childA.lastName.localeCompare(
          childB.lastName,
          'fi',
          { ignorePunctuation: true }
        )
        return lastNameCmp !== 0
          ? lastNameCmp
          : childA.firstName.localeCompare(childB.firstName, 'fi', {
              ignorePunctuation: true
            })
      }),
      operationDays: data.operationDays.map((date) => LocalDate.parseIso(date))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postGroupAbsences(
  groupId: UUID,
  absences: AbsenceUpdatePayload[]
): Promise<Result<void>> {
  return client
    .post<void>(`/absences/${groupId}`, { data: absences })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function deleteGroupAbsences(
  groupId: UUID,
  deletions: AbsencePayload[]
): Promise<Result<void>> {
  return client
    .post<void>(`/absences/${groupId}/delete`, deletions)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getStaffAttendances(
  groupId: UUID,
  params: SearchParams
): Promise<Result<StaffAttendanceGroup>> {
  return client
    .get<JsonOf<Response<StaffAttendanceGroup>>>(
      `/staff-attendances/group/${groupId}`,
      {
        params
      }
    )
    .then((res) => res.data.data)
    .then((group) => ({
      ...group,
      startDate: LocalDate.parseIso(group.startDate),
      endDate: LocalDate.parseNullableIso(group.endDate),
      attendances: Object.entries(group.attendances).reduce(
        (attendanceMap, [key, attendance]) => ({
          ...attendanceMap,
          [key]: {
            ...attendance,
            date: LocalDate.parseIso(attendance.date)
          }
        }),
        {}
      )
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postStaffAttendance(
  staffAttendance: StaffAttendance
): Promise<Result<void>> {
  return client
    .post(
      `/staff-attendances/group/${staffAttendance.groupId}`,
      staffAttendance
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
