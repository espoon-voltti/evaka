// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo } from 'react'

import { Loading, Result } from 'lib-common/api'
import {
  ChildAttendanceStatusResponse,
  ChildrenResponse
} from 'lib-common/generated/api-types/attendance'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'

import { attendanceStatusesQuery, childrenQuery } from './queries'

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

interface ChildAttendanceState {
  unitChildren: Result<ChildrenResponse>
  childAttendanceStatuses: Result<ChildAttendanceStatuses>
}

const defaultState: ChildAttendanceState = {
  unitChildren: Loading.of(),
  childAttendanceStatuses: Loading.of()
}

export const ChildAttendanceContext =
  createContext<ChildAttendanceState>(defaultState)

export const ChildAttendanceContextProvider = React.memo(
  function ChildAttendanceContextProvider({
    children
  }: {
    children: JSX.Element
  }) {
    const { unitId } = useNonNullableParams<{ unitId: UUID }>()

    const unitChildren = useQueryResult(childrenQuery(unitId))
    const attendanceStatuses = useQueryResult(attendanceStatusesQuery(unitId), {
      refetchInterval: 5 * 60 * 1000
    })

    const childAttendanceStatuses = useMemo(
      () =>
        attendanceStatuses.map((value) => new ChildAttendanceStatuses(value)),
      [attendanceStatuses]
    )

    const value = useMemo(
      () => ({
        unitChildren,
        childAttendanceStatuses
      }),
      [childAttendanceStatuses, unitChildren]
    )

    return (
      <ChildAttendanceContext.Provider value={value}>
        {children}
      </ChildAttendanceContext.Provider>
    )
  }
)
