// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ExternalStaffMember,
  StaffAttendanceType,
  StaffMember
} from 'lib-common/generated/api-types/attendance'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'

export interface Staff {
  type: 'employee' | 'external'
  name: string
  id: string
  present: boolean
}

function isStaffMember(
  staff: StaffMember | ExternalStaffMember
): staff is StaffMember {
  return 'employeeId' in staff && 'groupIds' in staff
}

export function toStaff(staff: StaffMember | ExternalStaffMember): Staff {
  if (isStaffMember(staff)) {
    return {
      type: 'employee',
      name: [staff.lastName, staff.firstName].join(' '),
      id: staff.employeeId,
      present: !!staff.present
    }
  }
  return {
    type: 'external',
    name: staff.name,
    id: staff.id,
    present: true
  }
}

export function getAttendanceArrivalDifferenceReasons(
  plannedStart: HelsinkiDateTime,
  arrival: HelsinkiDateTime
): StaffAttendanceType[] {
  const ARRIVAL_THRESHOLD_MINUTES = 5
  const arrivedBeforeMinThreshold = arrival.isBefore(
    plannedStart.subMinutes(ARRIVAL_THRESHOLD_MINUTES)
  )
  const arrivedAfterMaxThreshold = arrival.isAfter(
    plannedStart.addMinutes(ARRIVAL_THRESHOLD_MINUTES)
  )

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
    plannedEnd.subMinutes(5)
  )
  const depratedAfterMaxThreshold = departure.isAfter(plannedEnd.addMinutes(5))

  if (departedBeforeMinThreshold) {
    return ['OTHER_WORK', 'TRAINING', 'JUSTIFIED_CHANGE']
  }
  if (depratedAfterMaxThreshold) {
    return ['OVERTIME', 'JUSTIFIED_CHANGE']
  }
  return []
}
