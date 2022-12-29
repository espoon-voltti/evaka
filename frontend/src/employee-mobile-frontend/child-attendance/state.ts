// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

import { Failure, Result, Success } from 'lib-common/api'
import {
  Child,
  ChildAttendanceStatusResponse
} from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { attendanceStatusesQuery, childrenQuery } from './queries'

const STALE_TIME = 5 * 60 * 1000

export function useChildren(unitId: UUID): Result<Child[]> {
  return useQueryResult(childrenQuery(unitId), {
    staleTime: STALE_TIME
  })
}

export function useChild(unitId: UUID, childId: UUID): Result<Child> {
  const unitChildren = useChildren(unitId)
  return useMemo(
    () =>
      unitChildren
        .map((children) => children.find((child) => child.id === childId))
        .chain((child) =>
          child ? Success.of(child) : Failure.of({ message: 'Child not found' })
        ),
    [unitChildren, childId]
  )
}

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
  const attendanceStatuses = useQueryResult(attendanceStatusesQuery(unitId), {
    staleTime: STALE_TIME
  })
  return useMemo(
    () => attendanceStatuses.map((value) => new ChildAttendanceStatuses(value)),
    [attendanceStatuses]
  )
}
