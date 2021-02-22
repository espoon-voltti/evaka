// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  createContext
} from 'react'
import { PersonDetails, SearchColumn } from '~/types/person'
import { SearchOrder } from '~types'
import { Result, Success } from '@evaka/lib-common/src/api'
import { findByNameOrAddress } from '~api/person'
import { useDebounce } from '@evaka/lib-common/src/utils/useDebounce'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'

export interface CustomersState {
  customers: Result<PersonDetails[]>
  setCustomers: (request: Result<PersonDetails[]>) => void
  useCustomerSearch: () => void
  searchTerm: string
  setSearchTerm: (term: string) => void
  debouncedSearchTerm: string
  sortColumn: SearchColumn
  sortDirection: SearchOrder
  sortToggle: (column: SearchColumn) => () => void
}

const defaultState: CustomersState = {
  customers: Success.of([]),
  setCustomers: () => undefined,
  useCustomerSearch: () => undefined,
  searchTerm: '',
  setSearchTerm: () => undefined,
  debouncedSearchTerm: '',
  sortColumn: 'last_name,first_name',
  sortDirection: 'ASC',
  sortToggle: () => () => undefined
}

export const CustomersContext = createContext<CustomersState>(defaultState)

export const CustomersContextProvider = React.memo(
  function CustomersContextProvider({ children }: { children: JSX.Element }) {
    const [customers, setCustomers] = useState<Result<PersonDetails[]>>(
      defaultState.customers
    )
    const [searchTerm, setSearchTerm] = useState<string>(
      defaultState.searchTerm
    )
    const [sortColumn, setSortColumn] = useState<SearchColumn>(
      defaultState.sortColumn
    )
    const [sortDirection, setSortDirection] = useState<SearchOrder>('ASC')
    const debouncedSearchTerm = useDebounce(searchTerm, 500)

    const useCustomerSearch = () => {
      const searchCustomers = useRestApi(findByNameOrAddress, setCustomers)
      useEffect(() => {
        void searchCustomers(searchTerm, sortColumn, sortDirection)
      }, [searchCustomers, debouncedSearchTerm, sortColumn, sortDirection])
    }

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
        customers,
        setCustomers,
        useCustomerSearch,
        searchTerm,
        setSearchTerm,
        debouncedSearchTerm,
        sortColumn,
        sortDirection,
        sortToggle
      }),
      [
        customers,
        setCustomers,
        useCustomerSearch,
        searchTerm,
        setSearchTerm,
        debouncedSearchTerm,
        sortColumn,
        sortDirection,
        sortToggle
      ]
    )

    return (
      <CustomersContext.Provider value={value}>
        {children}
      </CustomersContext.Provider>
    )
  }
)
