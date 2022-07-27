// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { UUID } from 'lib-common/types'

import type { GroupStaffAttendance } from '../generated/api-types/daycare'
import type LocalDate from '../local-date'

// The `attendances` object only contains days that have staff attendances
export interface GroupStaffAttendanceForDates {
  groupId: UUID
  groupName: string
  startDate: LocalDate
  endDate: LocalDate | null
  attendances: Map<string, GroupStaffAttendance>
}
