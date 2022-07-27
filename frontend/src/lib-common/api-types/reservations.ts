// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { AbsenceType } from '../generated/api-types/daycare'
import type { TimeRange } from '../generated/api-types/reservations'
import type { JsonOf } from '../json'
import type LocalDate from '../local-date'

import type { DailyServiceTimes } from './child/common'

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
  reservation: TimeRange | null
  attendance: AttendanceTimes | null
  absence: { type: AbsenceType } | null
  dailyServiceTimes: DailyServiceTimes | null
  inOtherUnit: boolean
}

interface Group {
  id: string
  name: string
}

export interface Child {
  id: string
  firstName: string
  lastName: string
  dateOfBirth: LocalDate
}

export interface AttendanceTimes {
  startTime: string
  endTime: string | null
}
