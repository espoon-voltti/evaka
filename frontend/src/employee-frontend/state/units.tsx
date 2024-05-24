// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  Dispatch,
  SetStateAction,
  useMemo,
  useState
} from 'react'

import { CareType, ProviderType } from 'lib-common/generated/api-types/daycare'

import { SearchOrder } from '../types'

type UnitFilter = {
  text: string
  providerTypes: ProviderType[]
  careTypes: CareType[]
}

export interface UnitsState {
  filter: UnitFilter
  setFilter: (filter: UnitFilter) => void
  sortColumn: string
  setSortColumn: (text: string) => void
  sortDirection: SearchOrder
  setSortDirection: (text: SearchOrder) => void
  includeClosed: boolean
  setIncludeClosed: Dispatch<SetStateAction<boolean>>
}

const defaultState: UnitsState = {
  filter: { text: '', providerTypes: [], careTypes: [] },
  setFilter: () => undefined,
  sortColumn: 'name',
  setSortColumn: () => undefined,
  sortDirection: 'ASC',
  setSortDirection: () => undefined,
  includeClosed: false,
  setIncludeClosed: () => undefined
}

export const UnitsContext = createContext<UnitsState>(defaultState)

export type SearchColumn = 'name' | 'area.name' | 'address' | 'type'

export const UnitsContextProvider = React.memo(function UnitsContextProvider({
  children
}: {
  children: React.JSX.Element
}) {
  const [filter, setFilter] = useState<UnitFilter>(defaultState.filter)
  const [sortColumn, setSortColumn] = useState<string>(defaultState.sortColumn)
  const [sortDirection, setSortDirection] = useState<SearchOrder>(
    defaultState.sortDirection
  )
  const [includeClosed, setIncludeClosed] = useState(defaultState.includeClosed)

  const value = useMemo(
    () => ({
      filter,
      setFilter,
      sortColumn,
      setSortColumn,
      sortDirection,
      setSortDirection,
      includeClosed,
      setIncludeClosed
    }),
    [
      filter,
      setFilter,
      sortColumn,
      setSortColumn,
      sortDirection,
      setSortDirection,
      includeClosed,
      setIncludeClosed
    ]
  )

  return <UnitsContext.Provider value={value}>{children}</UnitsContext.Provider>
})
