// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { StaffAttendanceType } from 'lib-common/generated/api-types/attendance'
import { UnitStaffAttendance } from 'lib-common/generated/api-types/daycare'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { UUID } from 'lib-common/types'

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

export function getAttendanceArrivalDifferenceReasons(
  plannedStart: HelsinkiDateTime,
  arrival: HelsinkiDateTime
): StaffAttendanceType[] {
  const arrivedBeforeMinThreshold = arrival.isBefore(
    plannedStart.subMinutes(15)
  )
  const arrivedAfterMaxThreshold = arrival.isAfter(plannedStart.addMinutes(15))

  if (arrivedBeforeMinThreshold) {
    return ['OVERTIME', 'JUSTIFIED_CHANGE']
  }
  if (arrivedAfterMaxThreshold) {
    return ['OTHER_WORK', 'TRAINING', 'JUSTIFIED_CHANGE']
  }
  return []
}

export function getAttendanceDepartureDifferenceReasons(
  plannedEnd: HelsinkiDateTime,
  departure: HelsinkiDateTime
): StaffAttendanceType[] {
  const departedBeforeMinThreshold = departure.isBefore(
    plannedEnd.subMinutes(15)
  )
  const depratedAfterMaxThreshold = departure.isAfter(plannedEnd.addMinutes(15))

  if (departedBeforeMinThreshold) {
    return ['OTHER_WORK', 'TRAINING', 'JUSTIFIED_CHANGE']
  }
  if (depratedAfterMaxThreshold) {
    return ['OVERTIME', 'JUSTIFIED_CHANGE']
  }
  return []
}
