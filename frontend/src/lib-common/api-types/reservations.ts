// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import {
  AbsenceType,
  ChildServiceNeedInfo
} from '../generated/api-types/daycare'
import { JsonOf } from '../json'
import LocalDate from '../local-date'

import { DailyServiceTimesValue } from './child/common'

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
  reservation: { startTime: string; endTime: string } | null
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
