// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

import { Result } from 'lib-common/api'
import { ChildAttendanceStatusResponse } from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { attendanceStatusesQuery } from './queries'

export class ChildAttendanceStatuses {
  constructor(
    private readonly statuses: Record<
      UUID,
      ChildAttendanceStatusResponse | undefined
    >
  ) {}

  forChild(childId: UUID): ChildAttendanceStatusResponse {
    return this.statuses[childId] ?? defaultChildAttendanceStatus
  }
}

const defaultChildAttendanceStatus: ChildAttendanceStatusResponse = {
  status: 'COMING',
  attendances: [],
  absences: []
}

export function useAttendanceStatuses(
  unitId: UUID
): Result<ChildAttendanceStatuses> {
  const attendanceStatuses = useQueryResult(attendanceStatusesQuery(unitId))
  return useMemo(
    () => attendanceStatuses.map((value) => new ChildAttendanceStatuses(value)),
    [attendanceStatuses]
  )
}
