// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext, useCallback } from 'react'

import { Loading, Result } from 'lib-common/api'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { useParams } from 'react-router-dom'
import { UUID } from 'lib-common/types'
import { StaffAttendanceResponse } from 'lib-common/generated/api-types/attendance'
import { getUnitStaffAttendances } from '../api/staffAttendances'

interface StaffAttendanceState {
  staffAttendanceResponse: Result<StaffAttendanceResponse>
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
    const { unitId } = useParams<{ unitId: UUID }>()

    const [staffAttendanceResponse, setStaffAttendanceResponse] = useState<
      Result<StaffAttendanceResponse>
    >(Loading.of())

    const loadStaffAttendance = useRestApi(
      getUnitStaffAttendances,
      setStaffAttendanceResponse
    )
    const reloadStaffAttendance = useCallback(
      () => loadStaffAttendance(unitId),
      [loadStaffAttendance, unitId]
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
