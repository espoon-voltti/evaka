// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import mapValues from 'lodash/mapValues'

import { Failure, Result, Success } from 'lib-common/api'
import { parseDailyServiceTimes } from 'lib-common/api-types/daily-service-times'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  Absence,
  AbsenceCategory,
  AbsenceType
} from 'lib-common/generated/api-types/absence'
import {
  AbsenceRangeRequest,
  AttendanceChild,
  ChildAttendanceStatusResponse,
  DepartureRequest,
  ExpectedAbsencesOnDepartureRequest
} from 'lib-common/generated/api-types/attendance'
import {
  ConfirmedRangeDateUpdate,
  DailyChildReservationResult,
  DayReservationStatisticsResult,
  ReservationChildInfo
} from 'lib-common/generated/api-types/reservations'
import { ConfirmedRangeDate } from 'lib-common/generated/api-types/reservations'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { parseReservationDto } from 'lib-common/reservations'
import { UUID } from 'lib-common/types'

import { client } from '../client'

export async function getUnitChildren(
  unitId: string
): Promise<AttendanceChild[]> {
  return client
    .get<JsonOf<AttendanceChild[]>>(`/attendances/units/${unitId}/children`)
    .then((res) => deserializeChildren(res.data))
}

export async function getUnitAttendanceStatuses(
  unitId: string
): Promise<Record<UUID, ChildAttendanceStatusResponse | undefined>> {
  return client
    .get<
      JsonOf<Record<UUID, ChildAttendanceStatusResponse>>
    >(`/attendances/units/${unitId}/attendances`)
    .then((res) =>
      mapValues(res.data, deserializeChildAttendanceStatusResponse)
    )
}

export async function getUnitConfirmedDayReservations(
  unitId: string,
  examinationDate: LocalDate
): Promise<DailyChildReservationResult> {
  return client
    .get<JsonOf<DailyChildReservationResult>>(
      `/attendance-reservations/confirmed-days/daily`,
      {
        params: { unitId, examinationDate }
      }
    )
    .then((res) => deserializeUnitReservationConfirmedDayResult(res.data))
}

export async function getUnitConfirmedDaysReservationStatistics(
  unitId: string
): Promise<DayReservationStatisticsResult[]> {
  return client
    .get<JsonOf<DayReservationStatisticsResult[]>>(
      `/attendance-reservations/confirmed-days/stats`,
      {
        params: { unitId }
      }
    )
    .then((res) =>
      res.data.map((stat) => ({
        ...stat,
        date: LocalDate.parseIso(stat.date)
      }))
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
  body: AbsenceRangeRequest
): Promise<Result<void>> {
  return client
    .post<JsonOf<void>>(
      `/attendances/units/${unitId}/children/${childId}/absence-range`,
      body
    )
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export async function getConfirmedRange(
  childId: UUID
): Promise<ConfirmedRangeDate[]> {
  return client
    .get<
      JsonOf<ConfirmedRangeDate[]>
    >(`/attendance-reservations/by-child/${childId}/confirmed-range`)
    .then((res) =>
      res.data.map((row) => ({
        ...row,
        date: LocalDate.parseIso(row.date),
        reservations: row.reservations.map((reservation) =>
          parseReservationDto(reservation)
        ),
        dailyServiceTimes:
          row.dailyServiceTimes !== null
            ? parseDailyServiceTimes(row.dailyServiceTimes)
            : null
      }))
    )
}

export async function putConfirmedRange(
  childId: UUID,
  body: ConfirmedRangeDateUpdate[]
): Promise<void> {
  return client.put(
    `/attendance-reservations/by-child/${childId}/confirmed-range`,
    body
  )
}

export async function getFutureAbsencesByChildPlain(
  childId: UUID
): Promise<Absence[]> {
  return client
    .get<JsonOf<Absence[]>>(`/absences/by-child/${childId}/future`)
    .then((res) =>
      res.data.map((absence) => ({
        ...absence,
        date: LocalDate.parseIso(absence.date)
      }))
    )
}

export async function getFutureAbsencesByChild(
  childId: UUID
): Promise<Result<Absence[]>> {
  return getFutureAbsencesByChildPlain(childId)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

export async function getChildExpectedAbsencesOnDeparture({
  unitId,
  childId,
  departed
}: {
  unitId: string
  childId: string
  departed: LocalTime
}): Promise<AbsenceCategory[] | null> {
  const body: JsonOf<ExpectedAbsencesOnDepartureRequest> = {
    departed: departed.format()
  }
  return client
    .post<
      JsonOf<AbsenceCategory[] | null>
    >(`/attendances/units/${unitId}/children/${childId}/departure/expected-absences`, body)
    .then((res) => res.data)
}

export async function createDeparture({
  unitId,
  childId,
  absenceTypeNonbillable,
  absenceTypeBillable,
  departed
}: {
  unitId: string
  childId: string
  absenceTypeNonbillable: AbsenceType | null
  absenceTypeBillable: AbsenceType | null
  departed: LocalTime
}): Promise<void> {
  const body: JsonOf<DepartureRequest> = {
    departed: departed.format(),
    absenceTypeNonbillable,
    absenceTypeBillable
  }
  return client
    .post(`/attendances/units/${unitId}/children/${childId}/departure`, body)
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
  a: AttendanceChild,
  b: AttendanceChild,
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

function deserializeChildren(
  data: JsonOf<AttendanceChild[]>
): AttendanceChild[] {
  return data
    .map((child) => ({
      ...child,
      dateOfBirth: LocalDate.parseIso(child.dateOfBirth),
      dailyNote: child.dailyNote
        ? {
            ...child.dailyNote,
            modifiedAt: HelsinkiDateTime.parseIso(child.dailyNote.modifiedAt)
          }
        : null,
      stickyNotes: child.stickyNotes.map((note) => ({
        ...note,
        modifiedAt: HelsinkiDateTime.parseIso(note.modifiedAt),
        expires: LocalDate.parseIso(note.expires)
      })),
      reservations: child.reservations.map(parseReservationDto),
      dailyServiceTimes: child.dailyServiceTimes
        ? parseDailyServiceTimes(child.dailyServiceTimes)
        : null
    }))
    .sort((a, b) => compareByProperty(a, b, 'firstName'))
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

function deserializeUnitReservationConfirmedDayResult(
  data: JsonOf<DailyChildReservationResult>
): DailyChildReservationResult {
  return {
    children: mapValues(
      data.children,
      (data: JsonOf<ReservationChildInfo>): ReservationChildInfo => ({
        ...data,
        dateOfBirth: LocalDate.parseIso(data.dateOfBirth)
      })
    ),
    childReservations: data.childReservations.map((childResult) => ({
      ...childResult,
      reservations: childResult.reservations.map(parseReservationDto),
      dailyServiceTimes: childResult.dailyServiceTimes
        ? parseDailyServiceTimes(childResult.dailyServiceTimes)
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
