// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, {
  Dispatch,
  SetStateAction,
  useCallback,
  useMemo,
  useState,
  createContext,
  useContext
} from 'react'

import { Loading, Result } from 'lib-common/api'
import {
  DaycareCareArea,
  ProviderType,
  UnitStub
} from 'lib-common/generated/api-types/daycare'
import {
  FeeDecisionStatus,
  VoucherValueDecisionStatus,
  InvoiceStatus,
  InvoiceDistinctiveParams,
  DistinctiveParams,
  PaymentDistinctiveParams,
  PaymentStatus,
  VoucherValueDecisionDistinctiveParams,
  VoucherValueDecisionDifference,
  FeeDecisionDifference
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { useDebounce } from 'lib-common/utils/useDebounce'

import { areaQuery } from '../components/unit/queries'

import { UserContext } from './user'

export type Checked = Record<string, boolean>

interface FeeDecisionSearchFilters {
  area: string[]
  unit?: UUID
  statuses: FeeDecisionStatus[]
  distinctiveDetails: DistinctiveParams[]
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  searchByStartDate: boolean
  financeDecisionHandlerId: UUID | undefined
  difference: FeeDecisionDifference[]
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
  statuses: VoucherValueDecisionStatus[]
  distinctiveDetails: VoucherValueDecisionDistinctiveParams[]
  financeDecisionHandlerId?: UUID
  difference: VoucherValueDecisionDifference[]
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

export interface PaymentSearchFilters {
  area: string[]
  unit: string | null
  distinctions: PaymentDistinctiveParams[]
  status: PaymentStatus
  paymentDateStart: LocalDate | null
  paymentDateEnd: LocalDate | null
}

interface PaymentSearchFilterState {
  searchFilters: PaymentSearchFilters
  setSearchFilters: Dispatch<SetStateAction<PaymentSearchFilters>>
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
  placementValidDate: LocalDate | undefined
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
  units: Result<UnitStub[]>
  setUnits: Dispatch<SetStateAction<Result<UnitStub[]>>>
  financeDecisionHandlers: Result<FinanceDecisionHandlerOption[]>
  setFinanceDecisionHandlers: Dispatch<
    SetStateAction<Result<FinanceDecisionHandlerOption[]>>
  >
  availableAreas: Result<DaycareCareArea[]>
}

interface UiState {
  feeDecisions: FeeDecisionSearchFilterState
  valueDecisions: ValueDecisionSearchFilterState
  invoices: InvoiceSearchFilterState
  payments: PaymentSearchFilterState
  incomeStatements: IncomeStatementSearchFilterState
  shared: SharedState
}

const defaultState: UiState = {
  feeDecisions: {
    searchFilters: {
      distinctiveDetails: [],
      statuses: ['DRAFT'],
      area: [],
      startDate: undefined,
      endDate: LocalDate.todayInSystemTz(),
      searchByStartDate: false,
      financeDecisionHandlerId: undefined,
      difference: []
    },
    setSearchFilters: () => undefined,
    searchTerms: '',
    setSearchTerms: () => undefined,
    debouncedSearchTerms: '',
    clearSearchFilters: () => undefined
  },
  valueDecisions: {
    searchFilters: {
      distinctiveDetails: [],
      statuses: ['DRAFT'],
      area: [],
      difference: [],
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
  payments: {
    searchFilters: {
      area: [],
      unit: null,
      distinctions: [],
      status: 'DRAFT',
      paymentDateStart: null,
      paymentDateEnd: null
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
      sentEndDate: undefined,
      placementValidDate: undefined
    },
    setSearchFilters: () => undefined,
    clearSearchFilters: () => undefined
  },
  shared: {
    units: Loading.of(),
    setUnits: () => undefined,
    financeDecisionHandlers: Loading.of(),
    setFinanceDecisionHandlers: () => undefined,
    availableAreas: Loading.of()
  }
}

export const InvoicingUiContext = createContext<UiState>(defaultState)

export const InvoicingUIContextProvider = React.memo(
  function InvoicingUIContextProvider({
    children
  }: {
    children: React.JSX.Element
  }) {
    const { loggedIn } = useContext(UserContext)

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
      []
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
      []
    )

    const [invoiceSearchFilters, setInvoiceSearchFilters] =
      useState<InvoiceSearchFilters>(defaultState.invoices.searchFilters)
    const [invoiceFreeTextSearch, setInvoiceFreeTextSearch] = useState(
      defaultState.invoices.searchTerms
    )
    const invoiceDebouncedFreeText = useDebounce(invoiceFreeTextSearch, 500)
    const clearInvoiceSearchFilters = useCallback(
      () => setInvoiceSearchFilters(defaultState.invoices.searchFilters),
      []
    )

    const [paymentSearchFilters, setPaymentSearchFilters] =
      useState<PaymentSearchFilters>(defaultState.payments.searchFilters)
    const [paymentFreeTextSearch, setPaymentFreeTextSearch] = useState(
      defaultState.payments.searchTerms
    )
    const paymentDebouncedFreeText = useDebounce(paymentFreeTextSearch, 500)
    const clearPaymentSearchFilters = useCallback(
      () => setPaymentSearchFilters(defaultState.payments.searchFilters),
      []
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

    const [units, setUnits] = useState<Result<UnitStub[]>>(
      defaultState.shared.units
    )
    const [financeDecisionHandlers, setFinanceDecisionHandlers] = useState<
      Result<FinanceDecisionHandlerOption[]>
    >(defaultState.shared.financeDecisionHandlers)
    const availableAreas = useQueryResult(areaQuery(), { enabled: loggedIn })

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
        payments: {
          searchFilters: paymentSearchFilters,
          setSearchFilters: setPaymentSearchFilters,
          searchTerms: paymentFreeTextSearch,
          setSearchTerms: setPaymentFreeTextSearch,
          debouncedSearchTerms: paymentDebouncedFreeText,
          clearSearchFilters: clearPaymentSearchFilters
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
          availableAreas
        }
      }),
      [
        feeDecisionSearchFilters,
        feeDecisionFreeTextSearch,
        feeDecisionDebouncedFreeText,
        clearFeeDecisionSearchFilters,
        valueDecisionSearchFilters,
        valueDecisionFreeTextSearch,
        valueDecisionDebouncedFreeText,
        clearValueDecisionSearchFilters,
        invoiceSearchFilters,
        invoiceFreeTextSearch,
        invoiceDebouncedFreeText,
        clearInvoiceSearchFilters,
        paymentSearchFilters,
        paymentFreeTextSearch,
        paymentDebouncedFreeText,
        clearPaymentSearchFilters,
        incomeStatementSearchFilters,
        clearIncomeStatementSearchFilters,
        units,
        financeDecisionHandlers,
        availableAreas
      ]
    )

    return (
      <InvoicingUiContext.Provider value={value}>
        {children}
      </InvoicingUiContext.Provider>
    )
  }
)
