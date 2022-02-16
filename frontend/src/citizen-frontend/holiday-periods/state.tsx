// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useMemo, useContext } from 'react'
import { useUser } from 'citizen-frontend/auth/state'
import { Loading, Result, Success } from 'lib-common/api'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { useApiState } from 'lib-common/utils/useRestApi'
import { getActionRequiringHolidayPeriods } from './api'

export interface HolidayPeriodsState {
  holidayPeriods: Result<HolidayPeriod[]>
}

const defaultState: HolidayPeriodsState = {
  holidayPeriods: Loading.of()
}

export const HolidayPeriodsContext =
  createContext<HolidayPeriodsState>(defaultState)

export const HolidayPeriodsContextProvider = React.memo(
  function HolidayPeriodsContextContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const user = useUser()
    const [holidayPeriods] = useApiState(
      () =>
        user
          ? getActionRequiringHolidayPeriods()
          : Promise.resolve(Success.of([])),
      [user]
    )

    const value = useMemo(
      () => ({
        holidayPeriods
      }),
      [holidayPeriods]
    )

    return (
      <HolidayPeriodsContext.Provider value={value}>
        {children}
      </HolidayPeriodsContext.Provider>
    )
  }
)

export const useHolidayPeriods = () => useContext(HolidayPeriodsContext)
