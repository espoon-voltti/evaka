// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  useCallback,
  useEffect,
  useMemo,
  useState,
  createContext
} from 'react'

import { Result, Success, wrapResult } from 'lib-common/api'
import { PersonSummary } from 'lib-common/generated/api-types/pis'
import { useDebounce } from 'lib-common/utils/useDebounce'
import { useRestApi } from 'lib-common/utils/useRestApi'

import { searchPerson } from '../generated/api-clients/pis'
import { SearchOrder } from '../types'
import { SearchColumn } from '../types/person'

const searchPersonResult = wrapResult(searchPerson)

export interface CustomersState {
  customers: Result<PersonSummary[]>
  setCustomers: (request: Result<PersonSummary[]>) => void
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
  function CustomersContextProvider({
    children
  }: {
    children: React.JSX.Element
  }) {
    const [customers, setCustomers] = useState<Result<PersonSummary[]>>(
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

    const sortToggle = useCallback(
      (column: SearchColumn) => () => {
        if (sortColumn === column) {
          setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
        }
        setSortColumn(column)
      },
      [sortColumn, sortDirection, setSortColumn, setSortDirection]
    )

    const value = useMemo(() => {
      const useCustomerSearch = () => {
        const searchCustomers = useRestApi(searchPersonResult, setCustomers)
        useEffect(() => {
          void searchCustomers({
            body: { searchTerm, orderBy: sortColumn, sortDirection }
          })
        }, [debouncedSearchTerm, sortColumn, sortDirection]) // eslint-disable-line react-hooks/exhaustive-deps
      }

      return {
        customers,
        setCustomers,
        useCustomerSearch,
        searchTerm,
        setSearchTerm,
        debouncedSearchTerm,
        sortColumn,
        sortDirection,
        sortToggle
      }
    }, [
      customers,
      setCustomers,
      searchTerm,
      setSearchTerm,
      debouncedSearchTerm,
      sortColumn,
      sortDirection,
      sortToggle
    ])

    return (
      <CustomersContext.Provider value={value}>
        {children}
      </CustomersContext.Provider>
    )
  }
)
