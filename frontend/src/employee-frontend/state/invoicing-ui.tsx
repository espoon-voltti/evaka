// SPDX-FileCopyrightText: 2017-2024 City of Espoo
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
import { Employee } from 'lib-common/generated/api-types/pis'
import { DaycareId, EmployeeId } from 'lib-common/generated/api-types/shared'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'

import { areaQuery } from '../components/unit/queries'

import { UserContext } from './user'

export interface FeeDecisionSearchFilters {
  searchTerms: string
  area: string[]
  unit?: DaycareId
  statuses: FeeDecisionStatus[]
  distinctiveDetails: DistinctiveParams[]
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  searchByStartDate: boolean
  financeDecisionHandlerId: EmployeeId | undefined
  difference: FeeDecisionDifference[]
}

interface FeeDecisionSearchFilterState {
  page: number
  setPage: (p: number) => void
  confirmedSearchFilters: FeeDecisionSearchFilters | undefined
  setConfirmedSearchFilters: (
    filters: FeeDecisionSearchFilters | undefined
  ) => void
}

export interface ValueDecisionSearchFilters {
  searchTerms: string
  area: string[]
  unit?: DaycareId
  statuses: VoucherValueDecisionStatus[]
  distinctiveDetails: VoucherValueDecisionDistinctiveParams[]
  financeDecisionHandlerId?: EmployeeId
  difference: VoucherValueDecisionDifference[]
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  searchByStartDate: boolean
}

interface ValueDecisionSearchFilterState {
  page: number
  setPage: (p: number) => void
  confirmedSearchFilters: ValueDecisionSearchFilters | undefined
  setConfirmedSearchFilters: (
    filters: ValueDecisionSearchFilters | undefined
  ) => void
}

export interface InvoiceSearchFilters {
  searchTerms: string
  area: string[]
  unit?: DaycareId
  status: InvoiceStatus
  distinctiveDetails: InvoiceDistinctiveParams[]
  startDate: LocalDate | undefined
  endDate: LocalDate | undefined
  useCustomDatesForInvoiceSending: boolean
}

interface InvoiceSearchFilterState {
  page: number
  setPage: (p: number) => void
  confirmedSearchFilters: InvoiceSearchFilters | undefined
  setConfirmedSearchFilters: (filters: InvoiceSearchFilters | undefined) => void
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
  confirmedSearchFilters: PaymentSearchFilters | undefined
  setConfirmedSearchFilters: (filters: PaymentSearchFilters | undefined) => void
}

export interface IncomeStatementSearchFilters {
  area: string[]
  unit: DaycareId | undefined
  providerTypes: ProviderType[]
  sentStartDate: LocalDate | undefined
  sentEndDate: LocalDate | undefined
  placementValidDate: LocalDate | undefined
}

interface IncomeStatementSearchFilterState {
  page: number
  setPage: (p: number) => void
  confirmedSearchFilters: IncomeStatementSearchFilters | undefined
  setConfirmedSearchFilters: (
    filters: IncomeStatementSearchFilters | undefined
  ) => void
}

export interface FinanceDecisionHandlerOption {
  value: EmployeeId
  label: string
}

interface SharedState {
  units: Result<UnitStub[]>
  setUnits: Dispatch<SetStateAction<Result<UnitStub[]>>>
  financeDecisionHandlers: Result<FinanceDecisionHandlerOption[]>
  setFinanceDecisionHandlers: (handlers: Result<Employee[]>) => void
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
    page: 1,
    setPage: () => undefined,
    confirmedSearchFilters: undefined,
    setConfirmedSearchFilters: () => undefined
  },
  valueDecisions: {
    page: 1,
    setPage: () => undefined,
    confirmedSearchFilters: undefined,
    setConfirmedSearchFilters: () => undefined
  },
  invoices: {
    page: 1,
    setPage: () => undefined,
    confirmedSearchFilters: undefined,
    setConfirmedSearchFilters: () => undefined
  },
  payments: {
    page: 1,
    setPage: () => undefined,
    confirmedSearchFilters: undefined,
    setConfirmedSearchFilters: () => undefined
  },
  incomeStatements: {
    page: 1,
    setPage: () => undefined,
    confirmedSearchFilters: undefined,
    setConfirmedSearchFilters: () => undefined
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

    const [feeDecisionPage, setFeeDecisionPage] = useState<number>(
      defaultState.feeDecisions.page
    )
    const [
      confirmedFeeDecisionSearchFilters,
      setConfirmedFeeDecisionSearchFilters
    ] = useState<FeeDecisionSearchFilters | undefined>(
      defaultState.feeDecisions.confirmedSearchFilters
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

    const [invoicePage, setInvoicePage] = useState<number>(
      defaultState.invoices.page
    )
    const [confirmedInvoiceSearchFilters, setConfirmedInvoiceSearchFilters] =
      useState<InvoiceSearchFilters | undefined>(
        defaultState.invoices.confirmedSearchFilters
      )

    const [paymentPage, setPaymentPage] = useState<number>(
      defaultState.payments.page
    )
    const [confirmedPaymentSearchFilters, setConfirmedPaymentSearchFilters] =
      useState<PaymentSearchFilters | undefined>(
        defaultState.payments.confirmedSearchFilters
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

    const [units, setUnits] = useState<Result<UnitStub[]>>(
      defaultState.shared.units
    )
    const [financeDecisionHandlers, setFinanceDecisionHandlers] = useState<
      Result<FinanceDecisionHandlerOption[]>
    >(defaultState.shared.financeDecisionHandlers)
    const availableAreas = useQueryResult(areaQuery(), { enabled: loggedIn })

    const setFinanceDecisionHandlersFromResult = useCallback(
      (employeesResult: Result<Employee[]>) =>
        setFinanceDecisionHandlers(
          employeesResult.map((employees) =>
            employees.map((e) => ({
              value: e.id,
              label: [e.firstName, e.lastName].join(' ')
            }))
          )
        ),
      [setFinanceDecisionHandlers]
    )

    const value = useMemo(
      () => ({
        feeDecisions: {
          page: feeDecisionPage,
          setPage: setFeeDecisionPage,
          confirmedSearchFilters: confirmedFeeDecisionSearchFilters,
          setConfirmedSearchFilters: setConfirmedFeeDecisionSearchFilters
        },
        valueDecisions: {
          page: valueDecisionPage,
          setPage: setValueDecisionPage,
          confirmedSearchFilters: confirmedValueDecisionSearchFilters,
          setConfirmedSearchFilters: setConfirmedValueDecisionSearchFilters
        },
        invoices: {
          page: invoicePage,
          setPage: setInvoicePage,
          confirmedSearchFilters: confirmedInvoiceSearchFilters,
          setConfirmedSearchFilters: setConfirmedInvoiceSearchFilters
        },
        payments: {
          page: paymentPage,
          setPage: setPaymentPage,
          confirmedSearchFilters: confirmedPaymentSearchFilters,
          setConfirmedSearchFilters: setConfirmedPaymentSearchFilters
        },
        incomeStatements: {
          page: incomeStatementPage,
          setPage: setIncomeStatementPage,
          confirmedSearchFilters: confirmedIncomeStatementSearchFilters,
          setConfirmedSearchFilters: setConfirmedIncomeStatementSearchFilters
        },
        shared: {
          units,
          setUnits,
          financeDecisionHandlers,
          setFinanceDecisionHandlers: setFinanceDecisionHandlersFromResult,
          availableAreas
        }
      }),
      [
        feeDecisionPage,
        confirmedFeeDecisionSearchFilters,
        valueDecisionPage,
        confirmedValueDecisionSearchFilters,
        invoicePage,
        confirmedInvoiceSearchFilters,
        paymentPage,
        confirmedPaymentSearchFilters,
        incomeStatementPage,
        confirmedIncomeStatementSearchFilters,
        units,
        financeDecisionHandlers,
        availableAreas,
        setFinanceDecisionHandlersFromResult
      ]
    )

    return (
      <InvoicingUiContext.Provider value={value}>
        {children}
      </InvoicingUiContext.Provider>
    )
  }
)
