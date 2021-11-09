// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import {
  Absence,
  deserializeAbsence
} from 'lib-common/api-types/child/Absences'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AbsenceThreshold,
  AttendanceResponse,
  Child,
  ChildSensitiveInformation
} from 'lib-common/generated/api-types/attendance'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { AbsenceType, CareType } from '../types'
import { mapPinLoginRequiredError, PinLoginRequired } from './auth-pin-login'
import { client } from './client'

export const getChildSensitiveInformation = (
  childId: string
): Promise<Result<ChildSensitiveInformation | PinLoginRequired>> =>
  client
    .get<JsonOf<ChildSensitiveInformation>>(
      `/children/${childId}/sensitive-info`
    )
    .then((res) => res.data)
    .then((v) => Success.of(v))
    .catch(mapPinLoginRequiredError)

export async function getDaycareAttendances(
  unitId: string
): Promise<Result<AttendanceResponse>> {
  return client
    .get<JsonOf<AttendanceResponse>>(`/attendances/units/${unitId}`)
    .then((res) => deserializeAttendanceResponse(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function childArrivesPOST(
  unitId: string,
  childId: string,
  time: string
): Promise<Result<AttendanceResponse>> {
  return client
    .post<JsonOf<AttendanceResponse>>(
      `/attendances/units/${unitId}/children/${childId}/arrival`,
      {
        arrived: time
      }
    )
    .then((res) => deserializeAttendanceResponse(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

// TODO: update context with the response!
export async function childDeparts(
  unitId: string,
  childId: string,
  time: string
): Promise<Result<AttendanceResponse>> {
  return client
    .post<JsonOf<AttendanceResponse>>(
      `/attendances/units/${unitId}/children/${childId}/departure`,
      {
        departed: time
      }
    )
    .then((res) => deserializeAttendanceResponse(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function returnToComing(
  unitId: string,
  childId: string
): Promise<Result<AttendanceResponse>> {
  return client
    .post<JsonOf<AttendanceResponse>>(
      `/attendances/units/${unitId}/children/${childId}/return-to-coming`
    )
    .then((res) => deserializeAttendanceResponse(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function returnToPresent(
  unitId: string,
  childId: string
): Promise<Result<AttendanceResponse>> {
  return client
    .post<JsonOf<AttendanceResponse>>(
      `/attendances/units/${unitId}/children/${childId}/return-to-present`
    )
    .then((res) => deserializeAttendanceResponse(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postFullDayAbsence(
  unitId: string,
  childId: string,
  absenceType: AbsenceType
): Promise<Result<AttendanceResponse>> {
  return client
    .post<JsonOf<AttendanceResponse>>(
      `/attendances/units/${unitId}/children/${childId}/full-day-absence`,
      {
        absenceType
      }
    )
    .then((res) => deserializeAttendanceResponse(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export interface AbsencePayload {
  absenceType: AbsenceType
  childId: UUID
  date: LocalDate
  careType: CareType
}

export async function postAbsenceRange(
  unitId: string,
  childId: string,
  absenceType: AbsenceType,
  startDate: LocalDate,
  endDate: LocalDate
): Promise<Result<AttendanceResponse>> {
  return client
    .post<JsonOf<AttendanceResponse>>(
      `/attendances/units/${unitId}/children/${childId}/absence-range`,
      {
        absenceType,
        startDate,
        endDate
      }
    )
    .then((res) => deserializeAttendanceResponse(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getFutureAbsencesByChild(
  childId: UUID
): Promise<Result<Absence[]>> {
  return client
    .get<JsonOf<Absence[]>>(`/absences/by-child/${childId}/future`)
    .then((res) => res.data.map((absence) => deserializeAbsence(absence)))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getChildDeparture(
  unitId: string,
  childId: string
): Promise<Result<AbsenceThreshold[]>> {
  return client
    .get<JsonOf<AbsenceThreshold[]>>(
      `/attendances/units/${unitId}/children/${childId}/departure`
    )
    .then((res) => res.data)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function postDeparture(
  unitId: string,
  childId: string,
  absenceType: AbsenceType,
  departed: string
): Promise<Result<AttendanceResponse>> {
  return client
    .post<JsonOf<AttendanceResponse>>(
      `/attendances/units/${unitId}/children/${childId}/departure`,
      {
        departed,
        absenceType
      }
    )
    .then((res) => deserializeAttendanceResponse(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function deleteAbsenceRange(
  unitId: UUID,
  childId: UUID,
  dateRange: FiniteDateRange
): Promise<Result<void>> {
  return client
    .delete(`/attendances/units/${unitId}/children/${childId}/absence-range`, {
      params: {
        from: dateRange.start.formatIso(),
        to: dateRange.end.formatIso()
      }
    })
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

function compareByProperty(
  a: JsonOf<Child>,
  b: JsonOf<Child>,
  property: 'firstName' | 'lastName'
) {
  if (a[property] < b[property]) {
    return -1
  }
  if (a[property] > b[property]) {
    return 1
  }
  return 0
}

function deserializeAttendanceResponse(
  data: JsonOf<AttendanceResponse>
): AttendanceResponse {
  {
    return {
      children: [...data.children]
        .sort((a, b) => compareByProperty(a, b, 'lastName'))
        .sort((a, b) => compareByProperty(a, b, 'firstName'))
        .map((attendanceChild) => {
          return {
            ...attendanceChild,
            attendance: attendanceChild.attendance
              ? {
                  ...attendanceChild.attendance,
                  arrived: new Date(attendanceChild.attendance.arrived),
                  departed: attendanceChild.attendance.departed
                    ? new Date(attendanceChild.attendance.departed)
                    : null
                }
              : null,
            dailyNote: attendanceChild.dailyNote
              ? {
                  ...attendanceChild.dailyNote,
                  modifiedAt: new Date(attendanceChild.dailyNote.modifiedAt)
                }
              : null,
            stickyNotes: attendanceChild.stickyNotes.map((note) => ({
              ...note,
              modifiedAt: new Date(note.modifiedAt),
              expires: LocalDate.parseIso(note.expires)
            }))
          }
        }),
      groupNotes: data.groupNotes.map((groupNote) => ({
        ...groupNote,
        modifiedAt: new Date(groupNote.modifiedAt),
        expires: LocalDate.parseIso(groupNote.expires)
      }))
    }
  }
}
