// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Response, Result, Success } from 'lib-common/api'
import {
  AbsenceUpsert,
  GroupMonthCalendar,
  HolidayReservationsDelete,
  Presence
} from 'lib-common/generated/api-types/absence'
import {
  StaffAttendanceForDates,
  StaffAttendanceUpdate
} from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import TimeRange from 'lib-common/time-range'
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
  params: SearchParams,
  includeNonOperationalDays: boolean
): Promise<Result<GroupMonthCalendar>> {
  return client
    .get<JsonOf<GroupMonthCalendar>>(`/absences/${groupId}`, {
      params: { ...params, includeNonOperationalDays }
    })
    .then((res) => res.data)
    .then((data) => ({
      ...data,
      children: data.children.map(deserializeGroupMonthCalendarChild),
      daycareOperationTimes: data.daycareOperationTimes.map((range) =>
        range !== null ? TimeRange.parseJson(range) : null
      ),
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

export async function deleteGroupHolidayReservations(
  groupId: UUID,
  deletions: HolidayReservationsDelete[]
): Promise<Result<void>> {
  return client
    .post<void>(`/absences/${groupId}/delete-holiday-reservations`, deletions)
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
): Promise<Result<StaffAttendanceForDates>> {
  return client
    .get<JsonOf<Response<StaffAttendanceForDates>>>(
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
      attendances: Object.keys(group.attendances).reduce((acc, key) => {
        const attendance = group.attendances[key]
        return {
          ...acc,
          [key]: {
            ...attendance,
            date: LocalDate.parseIso(attendance.date),
            updated: HelsinkiDateTime.parseIso(attendance.updated)
          }
        }
      }, {})
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
