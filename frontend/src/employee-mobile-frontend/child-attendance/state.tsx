// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo } from 'react'

import { combine, Loading, Result } from 'lib-common/api'
import {
  AttendanceStatus,
  AttendanceTimes,
  Child,
  ChildAbsence
} from 'lib-common/generated/api-types/attendance'
import { GroupNote } from 'lib-common/generated/api-types/note'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'

import { attendanceStatusesQuery, childrenQuery } from './queries'

export interface AttendanceResponseChild extends Child {
  absences: ChildAbsence[]
  attendances: AttendanceTimes[]
  status: AttendanceStatus
}

export interface AttendanceResponse {
  children: AttendanceResponseChild[]
  groupNotes: GroupNote[]
}

interface ChildAttendanceState {
  attendanceResponse: Result<AttendanceResponse>
}

const defaultState: ChildAttendanceState = {
  attendanceResponse: Loading.of()
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

    const attendanceResponse: Result<AttendanceResponse> = useMemo(
      () =>
        combine(unitChildren, attendanceStatuses).map(
          ([childrenResponse, attendanceStatusResponses]) => ({
            ...childrenResponse,
            children: childrenResponse.children.map((child) => ({
              ...child,
              absences: attendanceStatusResponses[child.id]?.absences ?? [],
              attendances:
                attendanceStatusResponses[child.id]?.attendances ?? [],
              status: attendanceStatusResponses[child.id]?.status ?? 'COMING'
            }))
          })
        ),
      [unitChildren, attendanceStatuses]
    )

    const value = useMemo(
      () => ({
        attendanceResponse
      }),
      [attendanceResponse]
    )

    return (
      <ChildAttendanceContext.Provider value={value}>
        {children}
      </ChildAttendanceContext.Provider>
    )
  }
)
