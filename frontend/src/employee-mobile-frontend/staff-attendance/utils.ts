// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'

import type {
  ExternalStaffMember,
  StaffAttendanceType,
  StaffMember
} from 'lib-common/generated/api-types/attendance'
import type HelsinkiDateTime from 'lib-common/helsinki-date-time'

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
  plannedStarts: HelsinkiDateTime[],
  arrival: HelsinkiDateTime
): StaffAttendanceType[] {
  if (plannedStarts.length === 0) return []

  const closestStart = orderBy(plannedStarts, (start) =>
    Math.abs(start.timestamp - arrival.timestamp)
  )[0]

  const ARRIVAL_THRESHOLD_MINUTES = 5
  const arrivedBeforeMinThreshold = arrival.isBefore(
    closestStart.subMinutes(ARRIVAL_THRESHOLD_MINUTES)
  )
  const arrivedAfterMaxThreshold = arrival.isAfter(
    closestStart.addMinutes(ARRIVAL_THRESHOLD_MINUTES)
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
  plannedEnds: HelsinkiDateTime[],
  departure: HelsinkiDateTime
): StaffAttendanceType[] {
  if (plannedEnds.length === 0) return []

  const closestEnd = orderBy(plannedEnds, (start) =>
    Math.abs(start.timestamp - departure.timestamp)
  )[0]

  const departedBeforeMinThreshold = departure.isBefore(
    closestEnd.subMinutes(5)
  )
  const departedAfterMaxThreshold = departure.isAfter(closestEnd.addMinutes(5))

  if (departedBeforeMinThreshold) {
    return ['OTHER_WORK', 'TRAINING', 'JUSTIFIED_CHANGE']
  }
  if (departedAfterMaxThreshold) {
    return ['OVERTIME', 'JUSTIFIED_CHANGE']
  }
  return []
}
