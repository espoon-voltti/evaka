// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Reservation } from 'lib-common/generated/api-types/reservations'
import { JsonOf } from 'lib-common/json'
import { AbsenceType } from '../generated/api-types/daycare'
import LocalDate from '../local-date'
import { DailyServiceTimes } from './child/common'

export interface UnitAttendanceReservations {
  unit: string
  operationalDays: OperationalDay[]
  groups: GroupAttendanceReservations[]
  ungrouped: ChildDailyRecords[]
}

export interface OperationalDay {
  date: LocalDate
  isHoliday: boolean
}

interface GroupAttendanceReservations {
  group: Group
  children: ChildDailyRecords[]
}

export interface ChildDailyRecords {
  child: Child
  dailyData: Record<JsonOf<LocalDate>, ChildRecordOfDay>[]
}

export interface ChildRecordOfDay {
  reservation: Reservation | null
  attendance: AttendanceTimes | null
  absence: { type: AbsenceType } | null
}

interface Group {
  id: string
  name: string
}

export interface ChildReservations {
  child: Child
  dailyData: Record<JsonOf<LocalDate>, DailyChildData>
}

export interface Child {
  id: string
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
  dailyServiceTimes: DailyServiceTimes | null
}

export interface DailyChildData {
  reservations: Reservation[]
  attendance: AttendanceTimes | null
  absence: { type: AbsenceType } | null
}

interface AttendanceTimes {
  startTime: string
  endTime: string | null
}
