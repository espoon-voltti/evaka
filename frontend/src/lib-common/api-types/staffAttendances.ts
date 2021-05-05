// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

export interface StaffAttendance {
  groupId: UUID
  date: LocalDate
  count: number | null
  countOther: number | null
}

export interface StaffAttendanceGroup {
  groupId: UUID
  groupName: string
  startDate: LocalDate
  endDate: LocalDate | null
  attendances: { [key: string]: StaffAttendance }
}
