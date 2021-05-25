// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { JsonOf } from 'lib-common/json'
import { Failure, Result, Success } from 'lib-common/api'
import LocalDate from 'lib-common/local-date'
import { DailyServiceTimes } from 'lib-common/api-types/child/common'
import { AbsenceType, CareType } from '../types'
import { PlacementType } from '../types'
import { client } from './client'
import { UUID } from 'lib-common/types'
import {
  Absence,
  deserializeAbsence
} from 'lib-common/api-types/child/Absences'
import FiniteDateRange from 'lib-common/finite-date-range'

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
  staff: Staff[]
}

export interface Staff {
  firstName: string
  lastName: string
  id: string
  pinSet?: boolean
  groups: UUID[]
}

export interface Group {
  id: string
  name: string
  dailyNote: DailyNote | null
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
  absences: AbsenceSummary[]
  dailyServiceTimes: DailyServiceTimes | null
  dailyNote: DailyNote | null
}

interface Attendance {
  id: string
  arrived: Date
  departed: Date | null
}

interface AbsenceSummary {
  careType: CareType
  childId: string
  id: string
}

export interface DailyNote {
  id: string | null
  childId: string | null
  groupId: string | null
  date: LocalDate
  note: string | null
  feedingNote: DaycareDailyNoteLevelInfo | null
  sleepingNote: DaycareDailyNoteLevelInfo | null
  sleepingHours: number | null
  reminders: DaycareDailyNoteReminder[]
  reminderNote: string | null
  modifiedAt: Date | null
  modifiedBy: string
}

export type DaycareDailyNoteLevelInfo = 'GOOD' | 'MEDIUM' | 'NONE'

export type DaycareDailyNoteReminder = 'DIAPERS' | 'CLOTHES' | 'LAUNDRY'

interface ArrivalInfoResponse {
  absentFromPreschool: boolean
}

export type AttendanceStatus = 'COMING' | 'PRESENT' | 'DEPARTED' | 'ABSENT'

export interface Child {
  id: string
  firstName: string
  lastName: string
  preferredName: string | null
  ssn: string | null
  childAddress: string | null
  placementTypes: PlacementType[] | null
  allergies: string | null
  diet: string | null
  medication: string | null
  contacts: ContactInfo[] | null
  backupPickups: ContactInfo[] | null
}

export interface ContactInfo {
  id: string
  firstName: string | null
  lastName: string | null
  phone: string | null
  backupPhone: string | null
  email: string | null
}

type ChildResultStatus = 'SUCCESS' | 'WRONG_PIN' | 'PIN_LOCKED' | 'NOT_FOUND'

export interface ChildResult {
  status: ChildResultStatus
  child: Child | null
}

export async function getChildSensitiveInformation(
  childId: string,
  staffId: string,
  pin: string
): Promise<Result<ChildResult>> {
  return client
    .post<JsonOf<ChildResult>>(`/attendances/child/${childId}`, {
      pin,
      staffId
    })
    .then((res) => res.data)
    .then((v) => Success.of(v))
    .catch((e) => Failure.fromError(e))
}

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

export async function createOrUpdateDaycareDailyNoteForChild(
  childId: string,
  daycareDailyNote: DailyNote
): Promise<Result<void>> {
  const url = `/daycare-daily-note/child/${childId}`
  return (daycareDailyNote.id
    ? client.put(url, daycareDailyNote)
    : client.post(url, daycareDailyNote)
  )
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export async function deleteDaycareDailyNote(
  noteId: string
): Promise<Result<void>> {
  return client
    .delete(`/daycare-daily-note/${noteId}`)
    .then(() => Success.of(undefined))
    .catch((e) => Failure.fromError(e))
}

export async function upsertGroupDaycareDailyNote(
  groupId: string,
  daycareDailyNote: DailyNote
): Promise<Result<void>> {
  const url = `/daycare-daily-note/group/${groupId}`
  return (daycareDailyNote.id
    ? client.put(url, daycareDailyNote)
    : client.post(url, daycareDailyNote)
  )
    .then((res) => Success.of(res.data))
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
  a: JsonOf<AttendanceChild>,
  b: JsonOf<AttendanceChild>,
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
      unit: {
        ...data.unit,
        groups: data.unit.groups.map((group) => {
          return {
            ...group,
            dailyNote: group.dailyNote
              ? {
                  ...group.dailyNote,
                  date: LocalDate.parseIso(group.dailyNote.date),
                  modifiedAt: group.dailyNote.modifiedAt
                    ? new Date(group.dailyNote.modifiedAt)
                    : null
                }
              : null
          }
        })
      },
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
