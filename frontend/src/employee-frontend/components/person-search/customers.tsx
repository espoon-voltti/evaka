// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo, useState, createContext } from 'react'

import { useDebounce } from 'lib-common/utils/useDebounce'

import type { SearchOrder } from '../../types'

export type SearchColumn =
  | 'last_name,first_name'
  | 'date_of_birth'
  | 'street_address'
  | 'social_security_number'

export interface CustomersState {
  searchTerm: string
  setSearchTerm: (term: string) => void
  sortColumn: SearchColumn
  sortDirection: SearchOrder
  sortToggle: (column: SearchColumn) => () => void
  personSearchParams: {
    body: {
      searchTerm: string
      orderBy: SearchColumn
      sortDirection: SearchOrder
    }
  }
}

const defaultState: CustomersState = {
  searchTerm: '',
  setSearchTerm: () => undefined,
  sortColumn: 'last_name,first_name',
  sortDirection: 'ASC',
  sortToggle: () => () => undefined,
  personSearchParams: {
    body: {
      searchTerm: '',
      orderBy: 'last_name,first_name',
      sortDirection: 'ASC'
    }
  }
}

export const CustomersContext = createContext<CustomersState>(defaultState)

export const CustomersContextProvider = React.memo(
  function CustomersContextProvider({
    children
  }: {
    children: React.JSX.Element
  }) {
    const [searchTerm, setSearchTerm] = useState<string>(
      defaultState.searchTerm
    )
    const [sortColumn, setSortColumn] = useState<SearchColumn>(
      defaultState.sortColumn
    )
    const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')
    const debouncedSearchTerm = useDebounce(searchTerm, 500)

    const personSearchParams = useMemo(
      () => ({
        body: {
          searchTerm: debouncedSearchTerm,
          orderBy: sortColumn,
          sortDirection
        }
      }),
      [debouncedSearchTerm, sortColumn, sortDirection]
    )

    const sortToggle = useCallback(
      (column: SearchColumn) => () => {
        if (sortColumn === column) {
          setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
        }
        setSortColumn(column)
      },
      [sortColumn, sortDirection, setSortColumn, setSortDirection]
    )

    const value = useMemo(
      () => ({
        searchTerm,
        setSearchTerm,
        sortColumn,
        sortDirection,
        sortToggle,
        personSearchParams
      }),
      [personSearchParams, searchTerm, sortColumn, sortDirection, sortToggle]
    )

    return (
      <CustomersContext.Provider value={value}>
        {children}
      </CustomersContext.Provider>
    )
  }
)
