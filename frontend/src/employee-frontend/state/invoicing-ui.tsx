// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Dispatch, SetStateAction } from 'react'
import React, {
  useCallback,
  useMemo,
  useState,
  createContext,
  useContext
} from 'react'

import type { Result } from 'lib-common/api'
import { Loading } from 'lib-common/api'
import type {
  DaycareCareArea,
  ProviderType,
  UnitStub
} from 'lib-common/generated/api-types/daycare'
import type {
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
import type {
  DaycareId,
  EmployeeId
} from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'

import { areasQuery, unitsQuery } from '../queries'

import { UserContext } from './user'

interface FeeDecisionSearchFilters {
  searchTerms: string
  area: string[]
  unit?: DaycareId
  statuses: FeeDecisionStatus[]
  distinctiveDetails: DistinctiveParams[]
  startDate: LocalDate | null
  endDate: LocalDate | null
  searchByStartDate: boolean
  financeDecisionHandlerId: EmployeeId | undefined
  difference: FeeDecisionDifference[]
}

interface FeeDecisionSearchFilterState {
  page: number
  setPage: (p: number) => void
  searchFilters: FeeDecisionSearchFilters
  setSearchFilters: Dispatch<SetStateAction<FeeDecisionSearchFilters>>
  confirmedSearchFilters: FeeDecisionSearchFilters | undefined
  confirmSearchFilters: () => void
  clearSearchFilters: () => void
}

interface ValueDecisionSearchFilters {
  searchTerms: string
  area: string[]
  unit?: DaycareId
  statuses: VoucherValueDecisionStatus[]
  distinctiveDetails: VoucherValueDecisionDistinctiveParams[]
  financeDecisionHandlerId?: EmployeeId
  difference: VoucherValueDecisionDifference[]
  startDate: LocalDate | null
  endDate: LocalDate | null
  searchByStartDate: boolean
}

interface ValueDecisionSearchFilterState {
  page: number
  setPage: (p: number) => void
  searchFilters: ValueDecisionSearchFilters
  setSearchFilters: Dispatch<SetStateAction<ValueDecisionSearchFilters>>
  confirmedSearchFilters: ValueDecisionSearchFilters | undefined
  confirmSearchFilters: () => void
  clearSearchFilters: () => void
}

export interface InvoiceSearchFilters {
  searchTerms: string
  area: string[]
  unit?: DaycareId
  status: InvoiceStatus
  distinctiveDetails: InvoiceDistinctiveParams[]
  startDate: LocalDate | null
  endDate: LocalDate | null
  useCustomDatesForInvoiceSending: boolean
}

interface InvoiceSearchFilterState {
  page: number
  setPage: (p: number) => void
  searchFilters: InvoiceSearchFilters
  setSearchFilters: Dispatch<SetStateAction<InvoiceSearchFilters>>
  confirmedSearchFilters: InvoiceSearchFilters | undefined
  confirmSearchFilters: () => void
  clearSearchFilters: () => void
}

export interface PaymentSearchFilters {
  searchTerms: string
  area: string[]
  unit: DaycareId | null
  distinctions: PaymentDistinctiveParams[]
  status: PaymentStatus
  paymentDateStart: LocalDate | null
  paymentDateEnd: LocalDate | null
}

interface PaymentSearchFilterState {
  page: number
  setPage: (p: number) => void
  searchFilters: PaymentSearchFilters
  setSearchFilters: Dispatch<SetStateAction<PaymentSearchFilters>>
  confirmedSearchFilters: PaymentSearchFilters | undefined
  confirmSearchFilters: () => void
  clearSearchFilters: () => void
}

export interface IncomeStatementSearchFilters {
  area: string[]
  unit: DaycareId | undefined
  providerTypes: ProviderType[]
  sentStartDate: LocalDate | null
  sentEndDate: LocalDate | null
  placementValidDate: LocalDate | null
}

interface IncomeStatementSearchFilterState {
  page: number
  setPage: (p: number) => void
  searchFilters: IncomeStatementSearchFilters
  setSearchFilters: Dispatch<SetStateAction<IncomeStatementSearchFilters>>
  confirmedSearchFilters: IncomeStatementSearchFilters | undefined
  confirmSearchFilters: () => void
  clearSearchFilters: () => void
}

export interface FinanceDecisionHandlerOption {
  value: EmployeeId
  label: string
}

interface SharedState {
  availableAreas: Result<DaycareCareArea[]>
  allDaycareUnits: Result<UnitStub[]>
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
    page: 1,
    setPage: () => undefined,
    searchFilters: {
      searchTerms: '',
      distinctiveDetails: [],
      statuses: ['DRAFT'],
      area: [],
      startDate: null,
      endDate: LocalDate.todayInSystemTz(),
      searchByStartDate: false,
      financeDecisionHandlerId: undefined,
      difference: []
    },
    setSearchFilters: () => undefined,
    confirmedSearchFilters: undefined,
    confirmSearchFilters: () => undefined,
    clearSearchFilters: () => undefined
  },
  valueDecisions: {
    page: 1,
    setPage: () => undefined,
    searchFilters: {
      searchTerms: '',
      distinctiveDetails: [],
      statuses: ['DRAFT'],
      area: [],
      difference: [],
      startDate: null,
      endDate: LocalDate.todayInSystemTz(),
      searchByStartDate: false
    },
    setSearchFilters: () => undefined,
    confirmedSearchFilters: undefined,
    confirmSearchFilters: () => undefined,
    clearSearchFilters: () => undefined
  },
  invoices: {
    page: 1,
    setPage: () => undefined,
    searchFilters: {
      searchTerms: '',
      distinctiveDetails: [],
      area: [],
      status: 'DRAFT',
      startDate: null,
      endDate: null,
      useCustomDatesForInvoiceSending: false
    },
    setSearchFilters: () => undefined,
    confirmedSearchFilters: undefined,
    confirmSearchFilters: () => undefined,
    clearSearchFilters: () => undefined
  },
  payments: {
    page: 1,
    setPage: () => undefined,
    searchFilters: {
      searchTerms: '',
      area: [],
      unit: null,
      distinctions: [],
      status: 'DRAFT',
      paymentDateStart: null,
      paymentDateEnd: null
    },
    setSearchFilters: () => undefined,
    confirmedSearchFilters: undefined,
    confirmSearchFilters: () => undefined,
    clearSearchFilters: () => undefined
  },
  incomeStatements: {
    page: 1,
    setPage: () => undefined,
    searchFilters: {
      area: [],
      unit: undefined,
      providerTypes: [],
      sentStartDate: null,
      sentEndDate: null,
      placementValidDate: null
    },
    setSearchFilters: () => undefined,
    confirmedSearchFilters: undefined,
    clearSearchFilters: () => undefined,
    confirmSearchFilters: () => undefined
  },
  shared: {
    availableAreas: Loading.of(),
    allDaycareUnits: Loading.of()
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

    const [feeDecisionPage, setFeeDecisionPage] = useState<number>(
      defaultState.feeDecisions.page
    )
    const [
      confirmedFeeDecisionSearchFilters,
      setConfirmedFeeDecisionSearchFilters
    ] = useState<FeeDecisionSearchFilters | undefined>(
      defaultState.feeDecisions.confirmedSearchFilters
    )
    const [feeDecisionSearchFilters, _setFeeDecisionSearchFilters] =
      useState<FeeDecisionSearchFilters>(
        defaultState.feeDecisions.searchFilters
      )
    const setFeeDecisionSearchFilters = useCallback(
      (value: React.SetStateAction<FeeDecisionSearchFilters>) => {
        _setFeeDecisionSearchFilters(value)
        setConfirmedFeeDecisionSearchFilters(undefined)
      },
      []
    )
    const confirmFeeDecisionSearchFilters = useCallback(() => {
      setConfirmedFeeDecisionSearchFilters(feeDecisionSearchFilters)
      setFeeDecisionPage(defaultState.feeDecisions.page)
    }, [feeDecisionSearchFilters])
    const clearFeeDecisionSearchFilters = useCallback(
      () =>
        setFeeDecisionSearchFilters(defaultState.feeDecisions.searchFilters),
      [setFeeDecisionSearchFilters]
    )

    const [valueDecisionPage, setValueDecisionPage] = useState<number>(
      defaultState.valueDecisions.page
    )
    const [
      confirmedValueDecisionSearchFilters,
      setConfirmedValueDecisionSearchFilters
    ] = useState<ValueDecisionSearchFilters | undefined>(
      defaultState.valueDecisions.confirmedSearchFilters
    )
    const [valueDecisionSearchFilters, _setValueDecisionSearchFilters] =
      useState<ValueDecisionSearchFilters>(
        defaultState.valueDecisions.searchFilters
      )
    const setValueDecisionSearchFilters = useCallback(
      (value: React.SetStateAction<ValueDecisionSearchFilters>) => {
        _setValueDecisionSearchFilters(value)
        setConfirmedValueDecisionSearchFilters(undefined)
      },
      []
    )
    const confirmValueDecisionSearchFilters = useCallback(() => {
      setConfirmedValueDecisionSearchFilters(valueDecisionSearchFilters)
      setValueDecisionPage(defaultState.valueDecisions.page)
    }, [valueDecisionSearchFilters])
    const clearValueDecisionSearchFilters = useCallback(
      () =>
        setValueDecisionSearchFilters(
          defaultState.valueDecisions.searchFilters
        ),
      [setValueDecisionSearchFilters]
    )

    const [invoicePage, setInvoicePage] = useState<number>(
      defaultState.invoices.page
    )
    const [confirmedInvoiceSearchFilters, setConfirmedInvoiceSearchFilters] =
      useState<InvoiceSearchFilters | undefined>(
        defaultState.invoices.confirmedSearchFilters
      )
    const [invoiceSearchFilters, _setInvoiceSearchFilters] =
      useState<InvoiceSearchFilters>(defaultState.invoices.searchFilters)
    const setInvoiceSearchFilters = useCallback(
      (value: React.SetStateAction<InvoiceSearchFilters>) => {
        _setInvoiceSearchFilters(value)
        setConfirmedInvoiceSearchFilters(undefined)
      },
      []
    )
    const confirmInvoiceSearchFilters = useCallback(() => {
      setConfirmedInvoiceSearchFilters(invoiceSearchFilters)
      setInvoicePage(defaultState.invoices.page)
    }, [invoiceSearchFilters])
    const clearInvoiceSearchFilters = useCallback(
      () => setInvoiceSearchFilters(defaultState.invoices.searchFilters),
      [setInvoiceSearchFilters]
    )

    const [paymentPage, setPaymentPage] = useState<number>(
      defaultState.payments.page
    )
    const [confirmedPaymentSearchFilters, setConfirmedPaymentSearchFilters] =
      useState<PaymentSearchFilters | undefined>(
        defaultState.payments.confirmedSearchFilters
      )
    const [paymentSearchFilters, _setPaymentSearchFilters] =
      useState<PaymentSearchFilters>(defaultState.payments.searchFilters)
    const setPaymentSearchFilters = useCallback(
      (value: React.SetStateAction<PaymentSearchFilters>) => {
        _setPaymentSearchFilters(value)
        setConfirmedPaymentSearchFilters(undefined)
      },
      []
    )
    const confirmPaymentSearchFilters = useCallback(() => {
      setConfirmedPaymentSearchFilters(paymentSearchFilters)
      setPaymentPage(defaultState.payments.page)
    }, [paymentSearchFilters])
    const clearPaymentSearchFilters = useCallback(
      () => setPaymentSearchFilters(defaultState.payments.searchFilters),
      [setPaymentSearchFilters]
    )

    const [incomeStatementPage, setIncomeStatementPage] = useState<number>(
      defaultState.incomeStatements.page
    )
    const [
      confirmedIncomeStatementSearchFilters,
      setConfirmedIncomeStatementSearchFilters
    ] = useState<IncomeStatementSearchFilters | undefined>(
      defaultState.incomeStatements.confirmedSearchFilters
    )
    const [incomeStatementSearchFilters, _setIncomeStatementSearchFilters] =
      useState<IncomeStatementSearchFilters>(
        defaultState.incomeStatements.searchFilters
      )
    const setIncomeStatementSearchFilters = useCallback(
      (value: React.SetStateAction<IncomeStatementSearchFilters>) => {
        _setIncomeStatementSearchFilters(value)
        setConfirmedIncomeStatementSearchFilters(undefined)
      },
      []
    )
    const confirmIncomeStatementSearchFilters = useCallback(() => {
      setConfirmedIncomeStatementSearchFilters(incomeStatementSearchFilters)
      setIncomeStatementPage(defaultState.incomeStatements.page)
    }, [incomeStatementSearchFilters])
    const clearIncomeStatementSearchFilters = useCallback(() => {
      setIncomeStatementSearchFilters(
        defaultState.incomeStatements.searchFilters
      )
    }, [setIncomeStatementSearchFilters])

    const availableAreas = useQueryResult(areasQuery(), { enabled: loggedIn })
    const allDaycareUnits = useQueryResult(
      unitsQuery({ areaIds: null, type: ['DAYCARE'], from: null }),
      { enabled: loggedIn }
    )

    const value = useMemo(
      () => ({
        feeDecisions: {
          page: feeDecisionPage,
          setPage: setFeeDecisionPage,
          searchFilters: feeDecisionSearchFilters,
          confirmedSearchFilters: confirmedFeeDecisionSearchFilters,
          setSearchFilters: setFeeDecisionSearchFilters,
          confirmSearchFilters: confirmFeeDecisionSearchFilters,
          clearSearchFilters: clearFeeDecisionSearchFilters
        },
        valueDecisions: {
          page: valueDecisionPage,
          setPage: setValueDecisionPage,
          searchFilters: valueDecisionSearchFilters,
          confirmedSearchFilters: confirmedValueDecisionSearchFilters,
          setSearchFilters: setValueDecisionSearchFilters,
          confirmSearchFilters: confirmValueDecisionSearchFilters,
          clearSearchFilters: clearValueDecisionSearchFilters
        },
        invoices: {
          page: invoicePage,
          setPage: setInvoicePage,
          searchFilters: invoiceSearchFilters,
          confirmedSearchFilters: confirmedInvoiceSearchFilters,
          setSearchFilters: setInvoiceSearchFilters,
          confirmSearchFilters: confirmInvoiceSearchFilters,
          clearSearchFilters: clearInvoiceSearchFilters
        },
        payments: {
          page: paymentPage,
          setPage: setPaymentPage,
          searchFilters: paymentSearchFilters,
          confirmedSearchFilters: confirmedPaymentSearchFilters,
          setSearchFilters: setPaymentSearchFilters,
          confirmSearchFilters: confirmPaymentSearchFilters,
          clearSearchFilters: clearPaymentSearchFilters
        },
        incomeStatements: {
          page: incomeStatementPage,
          setPage: setIncomeStatementPage,
          searchFilters: incomeStatementSearchFilters,
          setSearchFilters: setIncomeStatementSearchFilters,
          confirmedSearchFilters: confirmedIncomeStatementSearchFilters,
          clearSearchFilters: clearIncomeStatementSearchFilters,
          confirmSearchFilters: confirmIncomeStatementSearchFilters
        },
        shared: {
          availableAreas,
          allDaycareUnits
        }
      }),
      [
        feeDecisionPage,
        feeDecisionSearchFilters,
        confirmedFeeDecisionSearchFilters,
        setFeeDecisionSearchFilters,
        confirmFeeDecisionSearchFilters,
        clearFeeDecisionSearchFilters,
        valueDecisionPage,
        valueDecisionSearchFilters,
        confirmedValueDecisionSearchFilters,
        setValueDecisionSearchFilters,
        confirmValueDecisionSearchFilters,
        clearValueDecisionSearchFilters,
        invoicePage,
        invoiceSearchFilters,
        confirmedInvoiceSearchFilters,
        setInvoiceSearchFilters,
        confirmInvoiceSearchFilters,
        clearInvoiceSearchFilters,
        paymentPage,
        paymentSearchFilters,
        confirmedPaymentSearchFilters,
        setPaymentSearchFilters,
        confirmPaymentSearchFilters,
        clearPaymentSearchFilters,
        incomeStatementPage,
        incomeStatementSearchFilters,
        confirmedIncomeStatementSearchFilters,
        setIncomeStatementSearchFilters,
        clearIncomeStatementSearchFilters,
        confirmIncomeStatementSearchFilters,
        availableAreas,
        allDaycareUnits
      ]
    )

    return (
      <InvoicingUiContext.Provider value={value}>
        {children}
      </InvoicingUiContext.Provider>
    )
  }
)
