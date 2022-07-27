// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import type FiniteDateRange from 'lib-common/finite-date-range'
import type {
  AbsenceThreshold,
  AttendanceResponse,
  Child,
  DepartureRequest
} from 'lib-common/generated/api-types/attendance'
import type {
  Absence,
  AbsenceType
} from 'lib-common/generated/api-types/daycare'
import type { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

import { client } from './client'

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
): Promise<Result<void>> {
  return client
    .post<JsonOf<void>>(
      `/attendances/units/${unitId}/children/${childId}/arrival`,
      {
        arrived: time
      }
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function childDeparts(
  unitId: string,
  childId: string,
  time: string
): Promise<Result<void>> {
  return client
    .post<JsonOf<void>>(
      `/attendances/units/${unitId}/children/${childId}/departure`,
      {
        departed: time
      }
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function returnToComing(
  unitId: string,
  childId: string
): Promise<Result<void>> {
  return client
    .post<JsonOf<void>>(
      `/attendances/units/${unitId}/children/${childId}/return-to-coming`
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function returnToPresent(
  unitId: string,
  childId: string
): Promise<Result<void>> {
  return client
    .post<JsonOf<void>>(
      `/attendances/units/${unitId}/children/${childId}/return-to-present`
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postFullDayAbsence(
  unitId: string,
  childId: string,
  absenceType: AbsenceType
): Promise<Result<void>> {
  return client
    .post<JsonOf<void>>(
      `/attendances/units/${unitId}/children/${childId}/full-day-absence`,
      {
        absenceType
      }
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function postAbsenceRange(
  unitId: string,
  childId: string,
  absenceType: AbsenceType,
  startDate: LocalDate,
  endDate: LocalDate
): Promise<Result<void>> {
  return client
    .post<JsonOf<void>>(
      `/attendances/units/${unitId}/children/${childId}/absence-range`,
      {
        absenceType,
        startDate,
        endDate
      }
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function getFutureAbsencesByChild(
  childId: UUID
): Promise<Result<Absence[]>> {
  return client
    .get<JsonOf<Absence[]>>(`/absences/by-child/${childId}/future`)
    .then((res) =>
      res.data.map((absence) => ({
        ...absence,
        date: LocalDate.parseIso(absence.date)
      }))
    )
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
  body: DepartureRequest
): Promise<Result<void>> {
  return client
    .post<JsonOf<AttendanceResponse>>(
      `/attendances/units/${unitId}/children/${childId}/departure`,
      body
    )
    .then(() => Success.of())
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
  a: Child,
  b: Child,
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
      children: data.children
        .map((attendanceChild) => {
          return {
            ...attendanceChild,
            attendance: attendanceChild.attendance
              ? {
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
            })),
            reservations: attendanceChild.reservations.map((reservation) => ({
              startTime: new Date(reservation.startTime),
              endTime: new Date(reservation.endTime)
            })),
            dailyServiceTimes: attendanceChild.dailyServiceTimes && {
              ...attendanceChild.dailyServiceTimes,
              validityPeriod: DateRange.parseJson(
                attendanceChild.dailyServiceTimes.validityPeriod
              )
            }
          }
        })
        .sort((a, b) => compareByProperty(a, b, 'lastName'))
        .sort((a, b) => compareByProperty(a, b, 'firstName')),
      groupNotes: data.groupNotes.map((groupNote) => ({
        ...groupNote,
        modifiedAt: new Date(groupNote.modifiedAt),
        expires: LocalDate.parseIso(groupNote.expires)
      }))
    }
  }
}
