// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

import type { Result } from 'lib-common/api'
import { Failure, Success } from 'lib-common/api'
import type {
  Child,
  ChildAttendanceStatusResponse
} from 'lib-common/generated/api-types/attendance'
import type { UUID } from 'lib-common/types'

export function useChild(
  children: Result<Child[]>,
  childId: UUID
): Result<Child> {
  return useMemo(
    () =>
      children
        .map((children) => children.find((child) => child.id === childId))
        .chain((child) =>
          child ? Success.of(child) : Failure.of({ message: 'Child not found' })
        ),
    [children, childId]
  )
}

export type AttendanceStatuses = Record<
  UUID,
  ChildAttendanceStatusResponse | undefined
>

export function childAttendanceStatus(
  attendanceStatuses: Record<UUID, ChildAttendanceStatusResponse | undefined>,
  childId: UUID
): ChildAttendanceStatusResponse {
  return attendanceStatuses[childId] ?? defaultChildAttendanceStatus
}

const defaultChildAttendanceStatus: ChildAttendanceStatusResponse = {
  status: 'COMING',
  attendances: [],
  absences: []
}
