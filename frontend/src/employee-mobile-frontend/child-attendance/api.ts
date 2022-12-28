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
  ChildrenResponse
} from 'lib-common/generated/api-types/attendance'
import { Absence, AbsenceType } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getUnitChildren(
  unitId: string
): Promise<ChildrenResponse> {
  return client
    .get<JsonOf<ChildrenResponse>>(`/attendances/units/${unitId}/children`)
    .then((res) => deserializeChildrenResponse(res.data))
}

export async function getUnitAttendanceStatuses(
  unitId: string
): Promise<Record<UUID, ChildAttendanceStatusResponse | undefined>> {
  return client
    .get<JsonOf<Record<UUID, ChildAttendanceStatusResponse>>>(
      `/attendances/units/${unitId}/attendances`
    )
    .then((res) =>
      mapValues(res.data, deserializeChildAttendanceStatusResponse)
    )
}

export async function createArrival({
  unitId,
  childId,
  arrived
}: {
  unitId: string
  childId: string
  arrived: string
}): Promise<void> {
  return client
    .post(`/attendances/units/${unitId}/children/${childId}/arrival`, {
      arrived
    })
    .then(() => undefined)
}

export async function returnToComing({
  unitId,
  childId
}: {
  unitId: string
  childId: string
}): Promise<void> {
  return client
    .post(`/attendances/units/${unitId}/children/${childId}/return-to-coming`)
    .then(() => undefined)
}

export async function returnToPresent({
  unitId,
  childId
}: {
  unitId: string
  childId: string
}): Promise<void> {
  return client
    .post(`/attendances/units/${unitId}/children/${childId}/return-to-present`)
    .then(() => undefined)
}

export async function createFullDayAbsence({
  unitId,
  childId,
  absenceType
}: {
  unitId: string
  childId: string
  absenceType: AbsenceType
}): Promise<void> {
  return client
    .post(`/attendances/units/${unitId}/children/${childId}/full-day-absence`, {
      absenceType
    })
    .then(() => undefined)
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

export async function getChildDeparture({
  unitId,
  childId
}: {
  unitId: string
  childId: string
}): Promise<AbsenceThreshold[]> {
  return client
    .get<JsonOf<AbsenceThreshold[]>>(
      `/attendances/units/${unitId}/children/${childId}/departure`
    )
    .then((res) => res.data)
}

export async function createDeparture({
  unitId,
  childId,
  absenceType,
  departed
}: {
  unitId: string
  childId: string
  absenceType: AbsenceType | null
  departed: string
}): Promise<void> {
  return client
    .post(`/attendances/units/${unitId}/children/${childId}/departure`, {
      absenceType,
      departed
    })
    .then(() => undefined)
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

export async function uploadChildImage({
  childId,
  file
}: {
  childId: string
  file: File
}): Promise<void> {
  const formData = new FormData()
  formData.append('file', file)

  return client
    .put(`/children/${childId}/image`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    .then(() => undefined)
}

export async function deleteChildImage(childId: string): Promise<void> {
  return client.delete(`/children/${childId}/image`).then(() => undefined)
}
