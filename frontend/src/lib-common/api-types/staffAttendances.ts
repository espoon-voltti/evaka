// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export interface StaffAttendanceUpdate {
  groupId: UUID
  date: LocalDate
  count: number
  countOther: number | null
}

export interface GroupStaffAttendance {
  groupId: UUID
  date: LocalDate
  count: number
  countOther: number
  updated: Date
}

export interface UnitStaffAttendance {
  date: LocalDate
  count: number
  countOther: number
  updated: Date | null
  groups: GroupStaffAttendance[]
}

// The `attendances` object only contains days that have staff attendances
export interface GroupStaffAttendanceForDates {
  groupId: UUID
  groupName: string
  startDate: LocalDate
  endDate: LocalDate | null
  attendances: Map<string, GroupStaffAttendance>
}
