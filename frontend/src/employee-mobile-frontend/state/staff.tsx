// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo, useState, createContext, useCallback } from 'react'

import { Loading, Result } from 'lib-common/api'
import { UnitStaffAttendance } from 'lib-common/generated/api-types/daycare'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { getUnitStaffAttendances } from '../api/staffAttendances'
import { useParams } from 'react-router-dom'
import { UUID } from 'lib-common/types'

interface StaffState {
  staffResponse: Result<UnitStaffAttendance>
  reloadStaff: () => void
}

const defaultState: StaffState = {
  staffResponse: Loading.of(),
  reloadStaff: () => undefined
}

export const StaffContext = createContext<StaffState>(defaultState)

export const StaffContextProvider = React.memo(function StaffContextProvider({
  children
}: {
  children: JSX.Element
}) {
  const { unitId } = useParams<{ unitId: UUID }>()

  const [staffResponse, setStaffResponse] = useState<
    Result<UnitStaffAttendance>
  >(Loading.of())

  const loadStaff = useRestApi(getUnitStaffAttendances, setStaffResponse)
  const reloadStaff = useCallback(() => loadStaff(unitId), [loadStaff, unitId])

  const value = useMemo(
    () => ({
      staffResponse,
      reloadStaff
    }),
    [staffResponse, reloadStaff]
  )

  return <StaffContext.Provider value={value}>{children}</StaffContext.Provider>
})
