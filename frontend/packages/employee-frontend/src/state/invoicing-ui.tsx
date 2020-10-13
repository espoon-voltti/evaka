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
  VoucherValueDecisionStatus,
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

interface ValueDecisionSearchFilters {
  area: string[]
  unit?: string
  status: VoucherValueDecisionStatus
}

interface ValueDecisionSearchFilterState {
  searchFilters: ValueDecisionSearchFilters
  setSearchFilters: Dispatch<SetStateAction<ValueDecisionSearchFilters>>
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
  feeDecisions: PageState & FeeDecisionSearchFilterState
  valueDecisions: PageState & ValueDecisionSearchFilterState
  invoices: PageState & InvoiceSearchFilterState
  shared: SharedState
}

const defaultState = {
  feeDecisions: {
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
  valueDecisions: {
    searchFilters: {
      status: 'DRAFT' as const,
      area: []
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
    const [feeDecisionSearchFilters, setFeeDecisionSearchFilters] = useState<
      FeeDecisionSearchFilters
    >(defaultState.feeDecisions.searchFilters)
    const [feeDecisionFreeTextSearch, setFeeDecisionFreeTextSearch] = useState(
      defaultState.feeDecisions.searchTerms
    )
    const feeDecisionDebouncedFreeText = useDebounce(
      feeDecisionFreeTextSearch,
      500
    )
    const clearFeeDecisionSearchFilters = useCallback(
      () =>
        setFeeDecisionSearchFilters(defaultState.feeDecisions.searchFilters),
      [setFeeDecisionSearchFilters]
    )
    const [feeDecisionChecked, setFeeDecisionChecked] = useState<Checked>({})
    const toggleFeeDecisionChecked = (id: string) =>
      setFeeDecisionChecked({
        ...feeDecisionChecked,
        [id]: !feeDecisionChecked[id]
      })
    const checkFeeDecisionIds = (ids: string[]) => {
      const idsChecked = ids.map((id) => ({ [id]: true }))
      setFeeDecisionChecked({
        ...feeDecisionChecked,
        ...Object.assign({}, ...idsChecked)
      })
    }
    const clearFeeDecisionChecked = () => setFeeDecisionChecked({})

    const [
      valueDecisionSearchFilters,
      setValueDecisionSearchFilters
    ] = useState<ValueDecisionSearchFilters>(
      defaultState.valueDecisions.searchFilters
    )
    const [
      valueDecisionFreeTextSearch,
      setValueDecisionFreeTextSearch
    ] = useState(defaultState.valueDecisions.searchTerms)
    const valueDecisionDebouncedFreeText = useDebounce(
      valueDecisionFreeTextSearch,
      500
    )
    const clearValueDecisionSearchFilters = useCallback(
      () =>
        setValueDecisionSearchFilters(
          defaultState.valueDecisions.searchFilters
        ),
      [setValueDecisionSearchFilters]
    )
    const [valueDecisionChecked, setValueDecisionChecked] = useState<Checked>(
      {}
    )
    const toggleValueDecisionChecked = (id: string) =>
      setValueDecisionChecked({
        ...valueDecisionChecked,
        [id]: !valueDecisionChecked[id]
      })
    const checkValueDecisionIds = (ids: string[]) => {
      const idsChecked = ids.map((id) => ({ [id]: true }))
      setValueDecisionChecked({
        ...valueDecisionChecked,
        ...Object.assign({}, ...idsChecked)
      })
    }
    const clearValueDecisionChecked = () => setValueDecisionChecked({})

    const [invoiceSearchFilters, setInvoiceSearchFilters] = useState<
      InvoiceSearchFilters
    >(defaultState.invoices.searchFilters)
    const [invoiceFreeTextSearch, setInvoiceFreeTextSearch] = useState(
      defaultState.invoices.searchTerms
    )
    const invoiceDebouncedFreeText = useDebounce(invoiceFreeTextSearch, 500)
    const clearInvoiceSearchFilters = useCallback(
      () => setInvoiceSearchFilters(defaultState.invoices.searchFilters),
      [setInvoiceSearchFilters]
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
        feeDecisions: {
          searchFilters: feeDecisionSearchFilters,
          setSearchFilters: setFeeDecisionSearchFilters,
          searchTerms: feeDecisionFreeTextSearch,
          setSearchTerms: setFeeDecisionFreeTextSearch,
          debouncedSearchTerms: feeDecisionDebouncedFreeText,
          clearSearchFilters: clearFeeDecisionSearchFilters,
          checked: feeDecisionChecked,
          setChecked: setFeeDecisionChecked,
          toggleChecked: toggleFeeDecisionChecked,
          checkIds: checkFeeDecisionIds,
          clearChecked: clearFeeDecisionChecked
        },
        valueDecisions: {
          searchFilters: valueDecisionSearchFilters,
          setSearchFilters: setValueDecisionSearchFilters,
          searchTerms: valueDecisionFreeTextSearch,
          setSearchTerms: setValueDecisionFreeTextSearch,
          debouncedSearchTerms: valueDecisionDebouncedFreeText,
          clearSearchFilters: clearValueDecisionSearchFilters,
          checked: valueDecisionChecked,
          setChecked: setValueDecisionChecked,
          toggleChecked: toggleValueDecisionChecked,
          checkIds: checkValueDecisionIds,
          clearChecked: clearValueDecisionChecked
        },
        invoices: {
          searchFilters: invoiceSearchFilters,
          setSearchFilters: setInvoiceSearchFilters,
          searchTerms: invoiceFreeTextSearch,
          setSearchTerms: setInvoiceFreeTextSearch,
          debouncedSearchTerms: invoiceDebouncedFreeText,
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
        feeDecisionSearchFilters,
        setFeeDecisionSearchFilters,
        feeDecisionFreeTextSearch,
        setFeeDecisionFreeTextSearch,
        feeDecisionDebouncedFreeText,
        clearFeeDecisionSearchFilters,
        feeDecisionChecked,
        setFeeDecisionChecked,
        feeDecisionChecked,
        setFeeDecisionChecked,
        valueDecisionSearchFilters,
        setValueDecisionSearchFilters,
        valueDecisionFreeTextSearch,
        setValueDecisionFreeTextSearch,
        valueDecisionDebouncedFreeText,
        clearValueDecisionSearchFilters,
        valueDecisionChecked,
        setValueDecisionChecked,
        valueDecisionChecked,
        setValueDecisionChecked,
        invoiceSearchFilters,
        setInvoiceSearchFilters,
        invoiceFreeTextSearch,
        setInvoiceFreeTextSearch,
        invoiceDebouncedFreeText,
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
