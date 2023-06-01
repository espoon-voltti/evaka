// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success, Response } from 'lib-common/api'
import { GroupStaffAttendanceForDates } from 'lib-common/api-types/codegen-excluded'
import {
  Presence,
  AbsenceUpsert,
  GroupMonthCalendar,
  GroupStaffAttendance,
  StaffAttendanceUpdate
} from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import {
  deserializeGroupMonthCalendarChild,
  deserializeGroupMonthCalendarDay
} from '../types/absence'

import { client } from './client'

interface SearchParams {
  year: number
  month: number
}

export async function getGroupMonthCalendar(
  groupId: UUID,
  params: SearchParams
): Promise<Result<GroupMonthCalendar>> {
  return client
    .get<JsonOf<GroupMonthCalendar>>(`/absences/${groupId}`, { params })
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      children: data.children.map(deserializeGroupMonthCalendarChild),
      days: data.days.map(deserializeGroupMonthCalendarDay)
    }))
    .then((v) => Success.of(v))
    .catch((e) => {
      console.error(e)
      return Failure.fromError(e)
    })
}

export async function postGroupAbsences(
  groupId: UUID,
  absences: AbsenceUpsert[]
): Promise<Result<void>> {
  return client
    .post<void>(`/absences/${groupId}`, absences)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function postGroupPresences(
  groupId: UUID,
  presences: Presence[]
): Promise<Result<void>> {
  return client
    .post<void>(`/absences/${groupId}/present`, presences)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function deleteChildAbsences(
  childId: UUID,
  date: LocalDate
): Promise<Result<void>> {
  return client
    .post<void>(`/absences/by-child/${childId}/delete`, { date })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function getStaffAttendances(
  groupId: UUID,
  params: SearchParams
): Promise<Result<GroupStaffAttendanceForDates>> {
  return client
    .get<JsonOf<Response<GroupStaffAttendanceForDates>>>(
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
        (attendanceMap, [key, attendance]) => {
          attendanceMap.set(key, {
            ...attendance,
            date: LocalDate.parseIso(attendance.date),
            updated: HelsinkiDateTime.parseIso(attendance.updated)
          })
          return attendanceMap
        },
        new Map<string, GroupStaffAttendance>()
      )
    }))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postStaffAttendance(
  staffAttendance: StaffAttendanceUpdate
): Promise<Result<void>> {
  return client
    .post(
      `/staff-attendances/group/${staffAttendance.groupId}`,
      staffAttendance
    )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}
