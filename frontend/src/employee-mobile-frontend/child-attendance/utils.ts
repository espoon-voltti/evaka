// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useMemo } from 'react'

import { Failure, Result, Success } from 'lib-common/api'
import {
  AttendanceChild,
  ChildAttendanceStatusResponse
} from 'lib-common/generated/api-types/attendance'
import { UUID } from 'lib-common/types'

export function useChild(
  children: Result<AttendanceChild[]>,
  childId: UUID
): Result<AttendanceChild> {
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
  child: AttendanceChild,
  attendanceStatuses: Record<UUID, ChildAttendanceStatusResponse | undefined>
): ChildAttendanceStatusResponse {
  const status = attendanceStatuses[child.id]
  if (status) return status

  if (child.scheduleType === 'TERM_BREAK') {
    return defaultChildAttendanceStatusTermBreak
  }

  return defaultChildAttendanceStatus
}

const defaultChildAttendanceStatus: ChildAttendanceStatusResponse = {
  status: 'COMING',
  attendances: [],
  absences: []
}

const defaultChildAttendanceStatusTermBreak: ChildAttendanceStatusResponse = {
  status: 'ABSENT',
  attendances: [],
  absences: []
}
