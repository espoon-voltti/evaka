// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  ExternalStaffMember,
  StaffMember
} from 'lib-common/generated/api-types/attendance'

export interface Staff {
  type: 'employee' | 'external'
  name: string
  id: string
  present: boolean
}

export function isStaffMember(
  staff: StaffMember | ExternalStaffMember
): staff is StaffMember {
  return 'employeeId' in staff && 'groupIds' in staff
}

export function isExternalStaffMember(
  staff: StaffMember | ExternalStaffMember
): staff is ExternalStaffMember {
  return 'name' in staff && 'id' in staff && 'groupId' in staff
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
