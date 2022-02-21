// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { createContext, useContext, useMemo } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { Loading, Result, Success } from 'lib-common/api'
import { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getHolidayPeriods } from './api'
import { isHolidayFormCurrentlyActive } from './holiday-period'

export interface HolidayPeriodsState {
  holidayPeriods: Result<HolidayPeriod[]>
  activePeriod: Result<HolidayPeriod | undefined>
}

const defaultState: HolidayPeriodsState = {
  holidayPeriods: Loading.of(),
  activePeriod: Loading.of()
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
      () => (user ? getHolidayPeriods() : Promise.resolve(Success.of([]))),
      [user]
    )

    const value = useMemo(
      () => ({
        holidayPeriods,
        activePeriod: holidayPeriods.map((periods) =>
          periods.find(isHolidayFormCurrentlyActive)
        )
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
