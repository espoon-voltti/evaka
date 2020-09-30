// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Dispatch,
  SetStateAction,
  useCallback,
  useMemo,
  useState,
  createContext
} from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { useDebounce } from '../utils/useDebounce'
import {
  DecisionDistinctiveDetails,
  FeeDecisionStatus,
  InvoiceStatus,
  Unit,
  InvoiceDistinctiveDetails
} from '../types/invoicing'
import { Loading, Result } from '../api'
import { CareArea } from '~types/unit'

interface Checked {
  [id: string]: boolean
}

interface PageState {
  checked: Checked
  toggleChecked: (id: string) => void
  checkIds: (ids: string[]) => void
  clearChecked: () => void
}

interface FeeDecisionSearchFilters {
  area: string[]
  unit?: string
  status: FeeDecisionStatus
  distinctiveDetails: DecisionDistinctiveDetails[]
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  searchByStartDate: boolean
}

interface FeeDecisionSearchFilterState {
  searchFilters: FeeDecisionSearchFilters
  setSearchFilters: Dispatch<SetStateAction<FeeDecisionSearchFilters>>
  searchTerms: string
  setSearchTerms: (s: string) => void
  debouncedSearchTerms: string
  clearSearchFilters: () => void
}

interface InvoiceSearchFilters {
  area: string[]
  unit?: string
  status: InvoiceStatus
  distinctiveDetails: InvoiceDistinctiveDetails[]
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  useCustomDatesForInvoiceSending: boolean
}

interface InvoiceSearchFilterState {
  searchFilters: InvoiceSearchFilters
  setSearchFilters: Dispatch<SetStateAction<InvoiceSearchFilters>>
  searchTerms: string
  setSearchTerms: (s: string) => void
  debouncedSearchTerms: string
  clearSearchFilters: () => void
}

interface SharedState {
  units: Result<Unit[]>
  setUnits: Dispatch<SetStateAction<Result<Unit[]>>>
  availableAreas: Result<CareArea[]>
  setAvailableAreas: Dispatch<SetStateAction<Result<CareArea[]>>>
}

interface UiState {
  decisions: PageState & FeeDecisionSearchFilterState
  invoices: PageState & InvoiceSearchFilterState
  shared: SharedState
}

const defaultState = {
  decisions: {
    searchFilters: {
      distinctiveDetails: [],
      status: 'DRAFT' as const,
      area: [],
      startDate: LocalDate.today().withDate(1),
      endDate: LocalDate.today(),
      searchByStartDate: false
    },
    setSearchFilters: () => undefined,
    searchTerms: '',
    setSearchTerms: () => undefined,
    debouncedSearchTerms: '',
    clearSearchFilters: () => undefined,
    checked: {},
    toggleChecked: () => undefined,
    checkIds: () => undefined,
    clearChecked: () => undefined
  },
  invoices: {
    searchFilters: {
      distinctiveDetails: [],
      area: [],
      status: 'DRAFT' as const,
      startDate: undefined,
      endDate: undefined,
      useCustomDatesForInvoiceSending: false
    },
    setSearchFilters: () => undefined,
    searchTerms: '',
    setSearchTerms: () => undefined,
    debouncedSearchTerms: '',
    clearSearchFilters: () => undefined,
    checked: {},
    toggleChecked: () => undefined,
    checkIds: () => undefined,
    clearChecked: () => undefined
  },
  shared: {
    units: Loading(),
    setUnits: () => undefined,
    availableAreas: Loading(),
    setAvailableAreas: () => undefined
  }
}

export const InvoicingUiContext = createContext<UiState>(defaultState)

export const InvoicingUIContextProvider = React.memo(
  function InvoicingUIContextProvider({ children }: { children: JSX.Element }) {
    const [decisionSearchFilters, setDecisionSearchFilters] = useState<
      FeeDecisionSearchFilters
    >(defaultState.decisions.searchFilters)
    const [decisionFreeTextSearch, setDecisionFreeTextSearch] = useState(
      defaultState.decisions.searchTerms
    )
    const decisionDebouncedFeeText = useDebounce(decisionFreeTextSearch, 500)
    const clearDecisionSearchFilters = useCallback(
      () => setDecisionSearchFilters(defaultState.decisions.searchFilters),
      [setDecisionSearchFilters]
    )
    const [decisionChecked, setDecisionChecked] = useState<Checked>({})
    const toggleDecisionChecked = (id: string) =>
      setDecisionChecked({ ...decisionChecked, [id]: !decisionChecked[id] })
    const checkDecisionIds = (ids: string[]) => {
      const idsChecked = ids.map((id) => ({ [id]: true }))
      setDecisionChecked({
        ...decisionChecked,
        ...Object.assign({}, ...idsChecked)
      })
    }
    const clearDecisionChecked = () => setDecisionChecked({})

    const [invoiceSearchFilters, setInvoiceSearchFilters] = useState<
      InvoiceSearchFilters
    >(defaultState.invoices.searchFilters)
    const [invoiceFreeTextSearch, setInvoiceFreeTextSearch] = useState(
      defaultState.invoices.searchTerms
    )
    const invoiceDebouncedFeeText = useDebounce(invoiceFreeTextSearch, 500)
    const clearInvoiceSearchFilters = useCallback(
      () => setInvoiceSearchFilters(defaultState.invoices.searchFilters),
      [setDecisionSearchFilters]
    )
    const [invoiceChecked, setInvoiceChecked] = useState<Checked>({})
    const toggleInvoiceChecked = (id: string) =>
      setInvoiceChecked({ ...invoiceChecked, [id]: !invoiceChecked[id] })
    const checkInvoiceIds = (ids: string[]) => {
      const idsChecked = ids.map((id) => ({ [id]: true }))
      setInvoiceChecked({
        ...invoiceChecked,
        ...Object.assign({}, ...idsChecked)
      })
    }
    const clearInvoiceChecked = () => setInvoiceChecked({})

    const [units, setUnits] = useState<Result<Unit[]>>(
      defaultState.shared.units
    )
    const [availableAreas, setAvailableAreas] = useState<Result<CareArea[]>>(
      defaultState.shared.availableAreas
    )

    const value = useMemo(
      () => ({
        decisions: {
          searchFilters: decisionSearchFilters,
          setSearchFilters: setDecisionSearchFilters,
          searchTerms: decisionFreeTextSearch,
          setSearchTerms: setDecisionFreeTextSearch,
          debouncedSearchTerms: decisionDebouncedFeeText,
          clearSearchFilters: clearDecisionSearchFilters,
          checked: decisionChecked,
          setChecked: setDecisionChecked,
          toggleChecked: toggleDecisionChecked,
          checkIds: checkDecisionIds,
          clearChecked: clearDecisionChecked
        },
        invoices: {
          searchFilters: invoiceSearchFilters,
          setSearchFilters: setInvoiceSearchFilters,
          searchTerms: invoiceFreeTextSearch,
          setSearchTerms: setInvoiceFreeTextSearch,
          debouncedSearchTerms: invoiceDebouncedFeeText,
          clearSearchFilters: clearInvoiceSearchFilters,
          checked: invoiceChecked,
          setChecked: setInvoiceChecked,
          toggleChecked: toggleInvoiceChecked,
          checkIds: checkInvoiceIds,
          clearChecked: clearInvoiceChecked
        },
        shared: {
          units,
          setUnits,
          availableAreas,
          setAvailableAreas
        }
      }),
      [
        decisionSearchFilters,
        setDecisionSearchFilters,
        decisionFreeTextSearch,
        setDecisionFreeTextSearch,
        decisionDebouncedFeeText,
        clearDecisionSearchFilters,
        decisionChecked,
        setDecisionChecked,
        decisionChecked,
        setDecisionChecked,
        invoiceSearchFilters,
        setInvoiceSearchFilters,
        invoiceFreeTextSearch,
        setInvoiceFreeTextSearch,
        invoiceDebouncedFeeText,
        clearInvoiceSearchFilters,
        invoiceChecked,
        setInvoiceChecked,
        units,
        setUnits,
        availableAreas,
        setAvailableAreas
      ]
    )

    return (
      <InvoicingUiContext.Provider value={value}>
        {children}
      </InvoicingUiContext.Provider>
    )
  }
)
