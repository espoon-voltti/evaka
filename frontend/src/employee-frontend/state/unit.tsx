// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback } from 'react'
import React, { createContext, useMemo, useState } from 'react'
import { useSearchParams } from 'wouter'

import LocalDate from 'lib-common/local-date'

import { isFilterTimePeriod, UnitFilters } from '../utils/UnitFilters'

export interface UnitState {
  filters: UnitFilters
  setFilters: (filters: UnitFilters) => void
}

const defaultState: UnitState = {
  filters: new UnitFilters(LocalDate.todayInSystemTz(), '1 day'),
  setFilters: () => undefined
}

export const UnitContext = createContext<UnitState>(defaultState)

export const UnitContextProvider = React.memo(function UnitContextProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const [params, setParams] = useSearchParams()
  const startDateParam = params.get('startDate')
  const startDate = startDateParam
    ? (LocalDate.tryParseIso(startDateParam) ?? LocalDate.todayInSystemTz())
    : LocalDate.todayInSystemTz()
  const periodParam = params.get('period')
  const period =
    periodParam && isFilterTimePeriod(periodParam) ? periodParam : '1 day'

  const [filters, _setFilters] = useState(new UnitFilters(startDate, period))

  const setFilters = useCallback(
    (newFilters: UnitFilters) => {
      _setFilters(newFilters)
      setParams({
        startDate: newFilters.startDate.formatIso(),
        period: newFilters.period
      })
    },
    [setParams]
  )

  const value = useMemo(
    () => ({
      filters,
      setFilters
    }),
    [filters, setFilters]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
