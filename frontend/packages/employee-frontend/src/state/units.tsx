// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  createContext,
  Dispatch,
  SetStateAction,
  useMemo,
  useState
} from 'react'
import { Loading, Result } from '@evaka/lib-common/src/api'
import { Unit } from '../types/unit'
import { SearchOrder } from '../types'

export interface UnitsState {
  units: Result<Unit[]>
  setUnits: (request: Result<Unit[]>) => void
  filter: string
  setFilter: (text: string) => void
  sortColumn: string
  setSortColumn: (text: string) => void
  sortDirection: SearchOrder
  setSortDirection: (text: SearchOrder) => void
  includeClosed: boolean
  setIncludeClosed: Dispatch<SetStateAction<boolean>>
}

const defaultState: UnitsState = {
  units: Loading.of(),
  setUnits: () => undefined,
  filter: '',
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
  children: JSX.Element
}) {
  const [units, setUnits] = useState<Result<Unit[]>>(defaultState.units)
  const [filter, setFilter] = useState<string>(defaultState.filter)
  const [sortColumn, setSortColumn] = useState<string>(defaultState.sortColumn)
  const [sortDirection, setSortDirection] = useState<SearchOrder>(
    defaultState.sortDirection
  )
  const [includeClosed, setIncludeClosed] = useState(defaultState.includeClosed)

  const value = useMemo(
    () => ({
      units,
      setUnits,
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
      units,
      setUnits,
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
