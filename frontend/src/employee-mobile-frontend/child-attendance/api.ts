// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'

import { Failure, Result, Success } from 'lib-common/api'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AbsenceThreshold,
  Child,
  ChildAttendanceStatusResponse,
  ChildrenResponse,
  DepartureRequest
} from 'lib-common/generated/api-types/attendance'
import { Absence, AbsenceType } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getUnitChildren(
  unitId: string
): Promise<Result<ChildrenResponse>> {
  return client
    .get<JsonOf<ChildrenResponse>>(`/attendances/units/${unitId}/children`)
    .then((res) => deserializeChildrenResponse(res.data))
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getUnitAttendanceStatuses(
  unitId: string
): Promise<Result<Record<UUID, ChildAttendanceStatusResponse>>> {
  return client
    .get<JsonOf<Record<UUID, ChildAttendanceStatusResponse>>>(
      `/attendances/units/${unitId}/attendances`
    )
    .then((res) =>
      mapValues(res.data, deserializeChildAttendanceStatusResponse)
    )
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
    .post(`/attendances/units/${unitId}/children/${childId}/departure`, body)
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

function deserializeChildrenResponse(
  data: JsonOf<ChildrenResponse>
): ChildrenResponse {
  {
    return {
      children: data.children
        .map((attendanceChild) => {
          return {
            ...attendanceChild,
            dailyNote: attendanceChild.dailyNote
              ? {
                  ...attendanceChild.dailyNote,
                  modifiedAt: HelsinkiDateTime.parseIso(
                    attendanceChild.dailyNote.modifiedAt
                  )
                }
              : null,
            stickyNotes: attendanceChild.stickyNotes.map((note) => ({
              ...note,
              modifiedAt: HelsinkiDateTime.parseIso(note.modifiedAt),
              expires: LocalDate.parseIso(note.expires)
            })),
            reservations: attendanceChild.reservations.map((reservation) => ({
              startTime: HelsinkiDateTime.parseIso(reservation.startTime),
              endTime: HelsinkiDateTime.parseIso(reservation.endTime)
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
        modifiedAt: HelsinkiDateTime.parseIso(groupNote.modifiedAt),
        expires: LocalDate.parseIso(groupNote.expires)
      }))
    }
  }
}

function deserializeChildAttendanceStatusResponse(
  data: JsonOf<ChildAttendanceStatusResponse>
): ChildAttendanceStatusResponse {
  return {
    ...data,
    attendances: data.attendances.map((attendance) => ({
      arrived: HelsinkiDateTime.parseIso(attendance.arrived),
      departed: attendance.departed
        ? HelsinkiDateTime.parseIso(attendance.departed)
        : null
    }))
  }
}
