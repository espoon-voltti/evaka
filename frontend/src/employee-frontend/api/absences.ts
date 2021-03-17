// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from './client'
import { Failure, Result, Success, Response } from '@evaka/lib-common/api'
import {
  Group,
  AbsencePayload,
  StaffAttendanceGroup,
  StaffAttendance,
  deserializeChild,
  AbsenceType,
  CareType
} from '../types/absence'
import { UUID } from '../types'
import { JsonOf } from '@evaka/lib-common/json'
import LocalDate from '@evaka/lib-common/local-date'

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
        const lastNameCmp = childA.lastName.localeCompare(childB.lastName)
        return lastNameCmp !== 0
          ? lastNameCmp
          : childA.firstName.localeCompare(childB.firstName)
      }),
      operationDays: data.operationDays.map((date) => LocalDate.parseIso(date))
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postGroupAbsences(
  groupId: UUID,
  absences: AbsencePayload[]
): Promise<Result<void>> {
  return client
    .post<void>(`/absences/${groupId}`, { data: absences })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function getStaffAttendances(
  groupId: UUID,
  params: SearchParams
): Promise<Result<StaffAttendanceGroup>> {
  return client
    .get<JsonOf<Response<StaffAttendanceGroup>>>(
      `/staff-attendances/${groupId}`,
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
    .post(`/staff-attendances/${staffAttendance.groupId}`, staffAttendance)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function postChildAbsence(
  absenceType: AbsenceType,
  careType: CareType,
  childId: UUID
): Promise<Result<void>> {
  return client
    .post<void>(`/absences/child/${childId}`, { absenceType, careType })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
