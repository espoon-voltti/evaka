// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from '@evaka/lib-common/src/json'
import { Failure, Result, Success } from '@evaka/lib-common/src/api'
import LocalDate from '@evaka/lib-common/src/local-date'

import { AbsenceType, CareType } from '../types'
import { PlacementType } from '../types'
import { client } from './client'

export interface DepartureInfoResponse {
  absentFrom: CareType[]
}

export interface AttendanceResponse {
  unit: Unit
  children: AttendanceChild[]
}

export interface Unit {
  id: string
  name: string
  groups: Group[]
}

export interface Group {
  id: string
  name: string
}

export interface AttendanceChild {
  id: string
  firstName: string
  lastName: string
  preferredName: string | null
  groupId: string
  backup: boolean
  status: AttendanceStatus
  placementType: PlacementType
  attendance: Attendance | null
  absences: Absence[]
  entitledToFreeFiveYearsOldDaycare: boolean
  dailyNote: DailyNote | null
}

interface Attendance {
  id: string
  arrived: Date
  departed: Date | null
}

interface Absence {
  careType: CareType
  childId: string
  id: string
}

interface DailyNote {
  id: string
  childId: string | null
  groupId: string | null
  date: LocalDate
  note: string | null
  feedingNote: DaycareDailyNoteLevelInfo | null
  sleepingNote: DaycareDailyNoteLevelInfo | null
  reminders: DaycareDailyNoteReminder[]
  reminderNote: string | null
  modifiedAt: Date | null
  modifiedBy: string
}

type DaycareDailyNoteLevelInfo = 'GOOD' | 'MEDIUM' | 'NONE'

type DaycareDailyNoteReminder = 'DIAPERS' | 'CLOTHES' | 'LAUNDRY'

interface ArrivalInfoResponse {
  absentFromPreschool: boolean
}

export type AttendanceStatus = 'COMING' | 'PRESENT' | 'DEPARTED' | 'ABSENT'

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

export async function childArrivesGET(
  unitId: string,
  childId: string,
  time: string
): Promise<Result<ArrivalInfoResponse>> {
  return client
    .get<JsonOf<ArrivalInfoResponse>>(
      `/attendances/units/${unitId}/children/${childId}/arrival`,
      {
        params: { time }
      }
    )
    .then((res) => res.data)
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

export async function getChildDeparture(
  unitId: string,
  childId: string,
  time: string
): Promise<Result<DepartureInfoResponse>> {
  return client
    .get<JsonOf<DepartureInfoResponse>>(
      `/attendances/units/${unitId}/children/${childId}/departure`,
      {
        params: { time }
      }
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

function compareByProperty(
  a: JsonOf<AttendanceChild>,
  b: JsonOf<AttendanceChild>,
  property: string
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
      unit: data.unit,
      children: data.children
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
                  date: LocalDate.parseIso(attendanceChild.dailyNote.date),
                  modifiedAt: attendanceChild.dailyNote.modifiedAt
                    ? new Date(attendanceChild.dailyNote.modifiedAt)
                    : null
                }
              : null
          }
        })
    }
  }
}
