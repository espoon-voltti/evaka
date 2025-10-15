// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { StaffAttendanceType } from './generated/api-types/attendance'

export function presentInGroup(type: StaffAttendanceType) {
  return !(
    type === 'TRAINING' ||
    type === 'OTHER_WORK' ||
    type === 'SICKNESS' ||
    type === 'CHILD_SICKNESS'
  )
}
