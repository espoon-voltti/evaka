// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  Dispatch,
  SetStateAction,
  useMemo,
  useState
} from 'react'

import LocalDate from 'lib-common/local-date'

import { UnitFilters } from '../utils/UnitFilters'

export interface UnitState {
  filters: UnitFilters
  setFilters: Dispatch<SetStateAction<UnitFilters>>
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
  const [filters, setFilters] = useState(defaultState.filters)

  const value = useMemo(
    () => ({
      filters,
      setFilters
    }),
    [filters]
  )

  return <UnitContext.Provider value={value}>{children}</UnitContext.Provider>
})
