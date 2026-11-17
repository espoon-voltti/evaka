// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { StaffAttendanceType } from './generated/api-types/attendance'
import type HelsinkiDateTime from './helsinki-date-time'

const presentInGroupByType: Record<StaffAttendanceType, boolean> = {
  PRESENT: true,
  OTHER_WORK: false,
  TRAINING: false,
  OVERTIME: true,
  JUSTIFIED_CHANGE: true,
  SICKNESS: false,
  CHILD_SICKNESS: false
}

export const presentInGroup = (type: StaffAttendanceType): boolean =>
  presentInGroupByType[type]

interface AttendanceTimes {
  arrived: HelsinkiDateTime
  departed: HelsinkiDateTime | null
}

export const attendancesOverlap = (a: AttendanceTimes, b: AttendanceTimes) =>
  (a.departed === null || b.arrived.isBefore(a.departed)) &&
  (b.departed === null || a.arrived.isBefore(b.departed))

export const isDuringAttendance = (
  time: HelsinkiDateTime,
  attendance: AttendanceTimes
) =>
  !time.isBefore(attendance.arrived) &&
  (attendance.departed === null || time.isBefore(attendance.departed))
