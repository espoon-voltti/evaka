// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useEffect, useMemo } from 'react'

import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import type { CurrentDayStaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import type { UUID } from 'lib-common/types'
import useNonNullableParams from 'lib-common/useNonNullableParams'
import { idleTracker } from 'lib-common/utils/idleTracker'
import { useApiState } from 'lib-common/utils/useRestApi'

import { client } from '../api/client'
import { getUnitStaffAttendances } from '../api/realtimeStaffAttendances'

interface StaffAttendanceState {
  staffAttendanceResponse: Result<CurrentDayStaffAttendanceResponse>
  reloadStaffAttendance: () => void
}

const defaultState: StaffAttendanceState = {
  staffAttendanceResponse: Loading.of(),
  reloadStaffAttendance: () => undefined
}

export const StaffAttendanceContext =
  createContext<StaffAttendanceState>(defaultState)

export const StaffAttendanceContextProvider = React.memo(
  function StaffAttendanceContextProvider({
    children
  }: {
    children: JSX.Element
  }) {
    const { unitId } = useNonNullableParams<{ unitId: UUID }>()

    const [staffAttendanceResponse, reloadStaffAttendance] = useApiState(
      () => getUnitStaffAttendances(unitId),
      [unitId]
    )

    useEffect(
      () =>
        idleTracker(client, reloadStaffAttendance, { thresholdInMinutes: 5 }),
      [reloadStaffAttendance]
    )

    const value = useMemo(
      () => ({
        staffAttendanceResponse,
        reloadStaffAttendance
      }),
      [staffAttendanceResponse, reloadStaffAttendance]
    )

    return (
      <StaffAttendanceContext.Provider value={value}>
        {children}
      </StaffAttendanceContext.Provider>
    )
  }
)
