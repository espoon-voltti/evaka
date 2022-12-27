// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useCallback, useEffect, useMemo } from 'react'

import { combine, Loading, Result } from 'lib-common/api'
import {
  AttendanceStatus,
  AttendanceTimes,
  Child,
  ChildAbsence
} from 'lib-common/generated/api-types/attendance'
import { GroupNote } from 'lib-common/generated/api-types/note'
import { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { useApiState } from 'lib-common/utils/useRestApi'

import { client } from '../client'

import { getUnitAttendanceStatuses, getUnitChildren } from './api'

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
  reloadAttendances: () => void
}

const defaultState: ChildAttendanceState = {
  attendanceResponse: Loading.of(),
  reloadAttendances: () => undefined
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

    const [unitChildren, reloadChildren] = useApiState(
      () => getUnitChildren(unitId),
      [unitId]
    )

    const [attendanceStatuses, reloadAttendanceStatuses] = useApiState(
      () => getUnitAttendanceStatuses(unitId),
      [unitId]
    )

    const reloadAttendances = useCallback(() => {
      void reloadChildren()
      void reloadAttendanceStatuses()
    }, [reloadChildren, reloadAttendanceStatuses])

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

    useEffect(
      () => idleTracker(client, reloadAttendances, { thresholdInMinutes: 5 }),
      [reloadAttendances]
    )

    const value = useMemo(
      () => ({
        attendanceResponse,
        reloadAttendances
      }),
      [attendanceResponse, reloadAttendances]
    )

    return (
      <ChildAttendanceContext.Provider value={value}>
        {children}
      </ChildAttendanceContext.Provider>
    )
  }
)
