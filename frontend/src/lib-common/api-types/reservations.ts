// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { type UUID } from 'lib-common/types'

import { type DailyServiceTimesValue } from '../generated/api-types/dailyservicetimes'
import {
  type AbsenceType,
  type ChildServiceNeedInfo
} from '../generated/api-types/daycare'
import { type Reservation } from '../generated/api-types/reservations'
import { type JsonOf } from '../json'
import type LocalDate from '../local-date'

export interface UnitAttendanceReservations {
  unit: string
  operationalDays: OperationalDay[]
  groups: GroupAttendanceReservations[]
  ungrouped: ChildDailyRecords[]
  unitServiceNeedInfo: UnitServiceNeedInfo
}

export interface OperationalDay {
  date: LocalDate
  isHoliday: boolean
  isInOpenHolidayPeriod: boolean
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
  dailyServiceTimes: DailyServiceTimesValue | null
  inOtherUnit: boolean
  isInBackupGroup: boolean
}

interface Group {
  id: string
  name: string
}

export interface Child {
  id: string
  firstName: string
  lastName: string
  preferredName: string | null
  dateOfBirth: LocalDate
}

export interface AttendanceTimes {
  startTime: string
  endTime: string | null
}

export interface GroupServiceNeedInfo {
  childInfos: ChildServiceNeedInfo[]
  groupId: UUID
}

export interface UnitServiceNeedInfo {
  groups: GroupServiceNeedInfo[]
  ungrouped: ChildServiceNeedInfo[]
  unitId: UUID
}
