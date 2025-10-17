// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { StaffAttendanceType } from './generated/api-types/attendance'

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
