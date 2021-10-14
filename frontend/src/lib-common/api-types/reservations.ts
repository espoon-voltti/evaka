// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Reservation } from 'lib-common/generated/api-types/reservations'
import { AbsenceType } from 'lib-common/generated/enums'
import { JsonOf } from 'lib-common/json'
import LocalDate from '../local-date'
import { DailyServiceTimes } from './child/common'

export interface UnitAttendanceReservations {
  unit: string
  operationalDays: OperationalDay[]
  groups: GroupAttendanceReservations[]
  ungrouped: ChildReservations[]
}

export interface OperationalDay {
  date: LocalDate
  isHoliday: boolean
}

interface GroupAttendanceReservations {
  group: Group
  children: ChildReservations[]
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

interface DailyChildData {
  reservations: Reservation[]
  attendance: AttendanceTimes
  absence: { type: AbsenceType } | null
}

interface AttendanceTimes {
  startTime: string
  endTime: string | null
}
