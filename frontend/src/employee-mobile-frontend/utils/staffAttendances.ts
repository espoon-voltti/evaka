// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'
import { UnitStaffAttendance } from 'lib-common/api-types/staffAttendances'

export interface AttendanceValues {
  count: number
  countOther: number
  updated: Date | null
}

export function staffAttendanceForGroupOrUnit(
  unitStaffAttendance: UnitStaffAttendance,
  groupId: UUID | undefined
): AttendanceValues {
  if (groupId === undefined) {
    // Return unit's combined attendance
    return {
      count: unitStaffAttendance.count,
      countOther: unitStaffAttendance.countOther,
      updated: unitStaffAttendance.updated
    }
  } else {
    const groupAttendance = unitStaffAttendance.groups.find(
      (group) => group.groupId === groupId
    )
    return groupAttendance
      ? {
          count: groupAttendance.count,
          countOther: groupAttendance.countOther,
          updated: groupAttendance.updated
        }
      : {
          count: 0,
          countOther: 0,
          updated: null
        }
  }
}
