// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useEffect, useMemo } from 'react'

import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import type { AttendanceResponse } from 'lib-common/generated/api-types/attendance'
import type { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getDaycareAttendances } from '../api/attendances'
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
    const { unitId } = useNonNullableParams<{ unitId: UUID }>()

    const [attendanceResponse, reloadAttendances] = useApiState(
      () => getDaycareAttendances(unitId),
      [unitId]
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
