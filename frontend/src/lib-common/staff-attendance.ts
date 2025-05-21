// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { StaffAttendanceType } from './generated/api-types/attendance'

export function presentInGroup(type: StaffAttendanceType) {
  if (type === 'TRAINING' || type === 'OTHER_WORK') {
    return false
  }

  return true
}
