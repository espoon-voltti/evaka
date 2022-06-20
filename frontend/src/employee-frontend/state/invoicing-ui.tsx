// SPDX-FileCopyrightText: 2017-2022 City of Espoo
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

import { Loading, Result } from 'lib-common/api'
import {
  DaycareCareArea,
  ProviderType
} from 'lib-common/generated/api-types/daycare'
import {
  FeeDecisionStatus,
  VoucherValueDecisionStatus,
  InvoiceStatus,
  InvoiceDistinctiveParams,
  DistinctiveParams
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { useDebounce } from 'lib-common/utils/useDebounce'

import { Unit } from '../api/daycare'

export interface Checked {
  [id: string]: boolean
}

interface FeeDecisionSearchFilters {
  area: string[]
  unit?: UUID
  status: FeeDecisionStatus
  distinctiveDetails: DistinctiveParams[]
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  searchByStartDate: boolean
  financeDecisionHandlerId: UUID | undefined
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
  unit?: UUID
  status: VoucherValueDecisionStatus
  financeDecisionHandlerId?: UUID
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  searchByStartDate: boolean
}

interface ValueDecisionSearchFilterState {
  searchFilters: ValueDecisionSearchFilters
  setSearchFilters: Dispatch<SetStateAction<ValueDecisionSearchFilters>>
  searchTerms: string
  setSearchTerms: (s: string) => void
  debouncedSearchTerms: string
  clearSearchFilters: () => void
}

export interface InvoiceSearchFilters {
  area: string[]
  unit?: string
  status: InvoiceStatus
  distinctiveDetails: InvoiceDistinctiveParams[]
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

export interface IncomeStatementSearchFilters {
  area: string[]
  providerTypes: ProviderType[]
  sentStartDate: LocalDate | undefined
  sentEndDate: LocalDate | undefined
}

interface IncomeStatementSearchFilterState {
  searchFilters: IncomeStatementSearchFilters
  setSearchFilters: Dispatch<SetStateAction<IncomeStatementSearchFilters>>
  clearSearchFilters: () => void
}

export interface FinanceDecisionHandlerOption {
  value: UUID
  label: string
}

interface SharedState {
  units: Result<Unit[]>
  setUnits: Dispatch<SetStateAction<Result<Unit[]>>>
  financeDecisionHandlers: Result<FinanceDecisionHandlerOption[]>
  setFinanceDecisionHandlers: Dispatch<
    SetStateAction<Result<FinanceDecisionHandlerOption[]>>
  >
  availableAreas: Result<DaycareCareArea[]>
  setAvailableAreas: Dispatch<SetStateAction<Result<DaycareCareArea[]>>>
}

interface UiState {
  feeDecisions: FeeDecisionSearchFilterState
  valueDecisions: ValueDecisionSearchFilterState
  invoices: InvoiceSearchFilterState
  incomeStatements: IncomeStatementSearchFilterState
  shared: SharedState
}

const defaultState: UiState = {
  feeDecisions: {
    searchFilters: {
      distinctiveDetails: [],
      status: 'DRAFT',
      area: [],
      startDate: undefined,
      endDate: LocalDate.todayInSystemTz(),
      searchByStartDate: false,
      financeDecisionHandlerId: undefined
    },
    setSearchFilters: () => undefined,
    searchTerms: '',
    setSearchTerms: () => undefined,
    debouncedSearchTerms: '',
    clearSearchFilters: () => undefined
  },
  valueDecisions: {
    searchFilters: {
      status: 'DRAFT',
      area: [],
      startDate: undefined,
      endDate: LocalDate.todayInSystemTz(),
      searchByStartDate: false
    },
    setSearchFilters: () => undefined,
    searchTerms: '',
    setSearchTerms: () => undefined,
    debouncedSearchTerms: '',
    clearSearchFilters: () => undefined
  },
  invoices: {
    searchFilters: {
      distinctiveDetails: [],
      area: [],
      status: 'DRAFT',
      startDate: undefined,
      endDate: undefined,
      useCustomDatesForInvoiceSending: false
    },
    setSearchFilters: () => undefined,
    searchTerms: '',
    setSearchTerms: () => undefined,
    debouncedSearchTerms: '',
    clearSearchFilters: () => undefined
  },
  incomeStatements: {
    searchFilters: {
      area: [],
      providerTypes: [],
      sentStartDate: undefined,
      sentEndDate: undefined
    },
    setSearchFilters: () => undefined,
    clearSearchFilters: () => undefined
  },
  shared: {
    units: Loading.of(),
    setUnits: () => undefined,
    financeDecisionHandlers: Loading.of(),
    setFinanceDecisionHandlers: () => undefined,
    availableAreas: Loading.of(),
    setAvailableAreas: () => undefined
  }
}

export const InvoicingUiContext = createContext<UiState>(defaultState)

export const InvoicingUIContextProvider = React.memo(
  function InvoicingUIContextProvider({ children }: { children: JSX.Element }) {
    const [feeDecisionSearchFilters, setFeeDecisionSearchFilters] =
      useState<FeeDecisionSearchFilters>(
        defaultState.feeDecisions.searchFilters
      )
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

    const [valueDecisionSearchFilters, setValueDecisionSearchFilters] =
      useState<ValueDecisionSearchFilters>(
        defaultState.valueDecisions.searchFilters
      )
    const [valueDecisionFreeTextSearch, setValueDecisionFreeTextSearch] =
      useState(defaultState.valueDecisions.searchTerms)
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

    const [invoiceSearchFilters, setInvoiceSearchFilters] =
      useState<InvoiceSearchFilters>(defaultState.invoices.searchFilters)
    const [invoiceFreeTextSearch, setInvoiceFreeTextSearch] = useState(
      defaultState.invoices.searchTerms
    )
    const invoiceDebouncedFreeText = useDebounce(invoiceFreeTextSearch, 500)
    const clearInvoiceSearchFilters = useCallback(
      () => setInvoiceSearchFilters(defaultState.invoices.searchFilters),
      [setInvoiceSearchFilters]
    )

    const [incomeStatementSearchFilters, setIncomeStatementSearchFilters] =
      useState<IncomeStatementSearchFilters>(
        defaultState.incomeStatements.searchFilters
      )
    const clearIncomeStatementSearchFilters = useCallback(
      () =>
        setIncomeStatementSearchFilters(
          defaultState.incomeStatements.searchFilters
        ),
      [setIncomeStatementSearchFilters]
    )

    const [units, setUnits] = useState<Result<Unit[]>>(
      defaultState.shared.units
    )
    const [financeDecisionHandlers, setFinanceDecisionHandlers] = useState<
      Result<FinanceDecisionHandlerOption[]>
    >(defaultState.shared.financeDecisionHandlers)
    const [availableAreas, setAvailableAreas] = useState<
      Result<DaycareCareArea[]>
    >(defaultState.shared.availableAreas)

    const value = useMemo(
      () => ({
        feeDecisions: {
          searchFilters: feeDecisionSearchFilters,
          setSearchFilters: setFeeDecisionSearchFilters,
          searchTerms: feeDecisionFreeTextSearch,
          setSearchTerms: setFeeDecisionFreeTextSearch,
          debouncedSearchTerms: feeDecisionDebouncedFreeText,
          clearSearchFilters: clearFeeDecisionSearchFilters
        },
        valueDecisions: {
          searchFilters: valueDecisionSearchFilters,
          setSearchFilters: setValueDecisionSearchFilters,
          searchTerms: valueDecisionFreeTextSearch,
          setSearchTerms: setValueDecisionFreeTextSearch,
          debouncedSearchTerms: valueDecisionDebouncedFreeText,
          clearSearchFilters: clearValueDecisionSearchFilters
        },
        invoices: {
          searchFilters: invoiceSearchFilters,
          setSearchFilters: setInvoiceSearchFilters,
          searchTerms: invoiceFreeTextSearch,
          setSearchTerms: setInvoiceFreeTextSearch,
          debouncedSearchTerms: invoiceDebouncedFreeText,
          clearSearchFilters: clearInvoiceSearchFilters
        },
        incomeStatements: {
          searchFilters: incomeStatementSearchFilters,
          setSearchFilters: setIncomeStatementSearchFilters,
          clearSearchFilters: clearIncomeStatementSearchFilters
        },
        shared: {
          units,
          setUnits,
          financeDecisionHandlers,
          setFinanceDecisionHandlers,
          availableAreas,
          setAvailableAreas
        }
      }),
      // eslint-disable-next-line react-hooks/exhaustive-deps
      [
        feeDecisionSearchFilters,
        setFeeDecisionSearchFilters,
        feeDecisionFreeTextSearch,
        setFeeDecisionFreeTextSearch,
        feeDecisionDebouncedFreeText,
        clearFeeDecisionSearchFilters,
        valueDecisionSearchFilters,
        setValueDecisionSearchFilters,
        valueDecisionFreeTextSearch,
        setValueDecisionFreeTextSearch,
        valueDecisionDebouncedFreeText,
        clearValueDecisionSearchFilters,
        invoiceSearchFilters,
        setInvoiceSearchFilters,
        invoiceFreeTextSearch,
        setInvoiceFreeTextSearch,
        invoiceDebouncedFreeText,
        clearInvoiceSearchFilters,
        incomeStatementSearchFilters,
        setIncomeStatementSearchFilters,
        clearIncomeStatementSearchFilters,
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
