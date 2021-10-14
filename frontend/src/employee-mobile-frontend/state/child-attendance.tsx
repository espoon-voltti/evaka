// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useMemo,
  useState,
  createContext,
  useCallback,
  useEffect
} from 'react'
import { Loading, Result } from 'lib-common/api'
import { getDaycareAttendances } from '../api/attendances'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { useParams } from 'react-router-dom'
import { UUID } from 'lib-common/types'
import { AttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { client } from '../api/client'

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
    const { unitId } = useParams<{ unitId: UUID }>()

    const [attendanceResponse, setAttendanceResponse] = useState<
      Result<AttendanceResponse>
    >(Loading.of())

    const loadAttendances = useRestApi(
      getDaycareAttendances,
      setAttendanceResponse
    )

    const reloadAttendances = useCallback(
      () => loadAttendances(unitId),
      [loadAttendances, unitId]
    )

    useEffect(reloadAttendances, [reloadAttendances])

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
