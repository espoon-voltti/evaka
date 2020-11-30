// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useContext, useEffect, useReducer } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { isSuccess, mapResult, Result } from '~api'
import {
  createInvoices,
  getInvoices,
  InvoiceSearchParams,
  SortByInvoices,
  sendInvoices,
  sendInvoicesByDate
} from '~api/invoicing'
import { InvoicingUiContext } from '~state/invoicing-ui'
import { SearchOrder } from '~types'
import { InvoiceSearchResult, InvoiceSummary } from '~types/invoicing'
import { useRestApi } from '~utils/useRestApi'

const pageSize = 200

export type PagedInvoices = {
  [k: number]: Result<InvoiceSummary[]>
}

type State = {
  page: number
  sortBy: SortByInvoices
  sortDirection: SearchOrder
  invoices: PagedInvoices
  invoiceTotals?: { total: number; pages: number }
  needsReload: boolean
  checkedInvoices: Record<string, true>
  allInvoicesToggle: boolean
  showModal: boolean
}

export type InvoicesAction =
  | { type: 'SET_PAGE'; payload: number }
  | { type: 'SET_SORT_BY'; payload: SortByInvoices }
  | { type: 'SET_SORT_DIRECTION'; payload: SearchOrder }
  | {
      type: 'SET_INVOICES'
      payload: { page: number; result: Result<InvoiceSearchResult> }
    }
  | { type: 'RELOAD_INVOICES' }
  | { type: 'INVOICE_RELOAD_DONE' }
  | { type: 'TOGGLE_CHECKED'; payload: string }
  | { type: 'CLEAR_CHECKED' }
  | { type: 'CHECK_ALL' }
  | { type: 'ALL_INVOICES_TOGGLE' }
  | { type: 'OPEN_MODAL' }
  | { type: 'CLOSE_MODAL' }

function reducer(state: State, action: InvoicesAction): State {
  switch (action.type) {
    case 'SET_PAGE':
      return { ...state, page: action.payload }
    case 'SET_SORT_BY':
      return { ...state, sortBy: action.payload }
    case 'SET_SORT_DIRECTION':
      return { ...state, sortDirection: action.payload }
    case 'SET_INVOICES': {
      const { page, result } = action.payload
      return {
        ...state,
        invoices: {
          ...state.invoices,
          [page]: mapResult(result, (r) => r.data)
        },
        invoiceTotals: isSuccess(result)
          ? { total: result.data.total, pages: result.data.pages }
          : state.invoiceTotals
      }
    }
    case 'RELOAD_INVOICES':
      return { ...state, needsReload: true }
    case 'INVOICE_RELOAD_DONE':
      return { ...state, needsReload: false }
    case 'TOGGLE_CHECKED': {
      if (state.checkedInvoices[action.payload]) {
        const { [action.payload]: _, ...rest } = state.checkedInvoices
        return {
          ...state,
          checkedInvoices: rest
        }
      } else {
        return {
          ...state,
          checkedInvoices: {
            ...state.checkedInvoices,
            [action.payload]: true
          }
        }
      }
    }
    case 'CLEAR_CHECKED':
      return {
        ...state,
        checkedInvoices: {},
        allInvoicesToggle: false
      }
    case 'CHECK_ALL': {
      const currentPage = state.invoices[state.page]
      const checked: Record<string, true> = isSuccess(currentPage)
        ? Object.fromEntries(
            currentPage.data.map((invoice) => [invoice.id, true])
          )
        : {}
      return {
        ...state,
        checkedInvoices: {
          ...state.checkedInvoices,
          ...checked
        }
      }
    }
    case 'ALL_INVOICES_TOGGLE':
      return { ...state, allInvoicesToggle: !state.allInvoicesToggle }
    case 'OPEN_MODAL':
      return { ...state, showModal: true }
    case 'CLOSE_MODAL':
      return { ...state, showModal: false }
  }
  return state
}

const initialState: State = {
  page: 1,
  sortBy: 'HEAD_OF_FAMILY',
  sortDirection: 'ASC',
  invoices: {},
  needsReload: false,
  checkedInvoices: {},
  allInvoicesToggle: false,
  showModal: false
}

export function useInvoicesState() {
  const [state, dispatch] = useReducer(reducer, initialState)

  const setInvoicesResult = useCallback(
    (result: Result<InvoiceSearchResult>) => {
      dispatch({
        type: 'SET_INVOICES',
        payload: { page: state.page, result }
      })
    },
    [dispatch, state.page]
  )

  const {
    invoices: { searchFilters, debouncedSearchTerms }
  } = useContext(InvoicingUiContext)

  const reloadInvoices = useRestApi(getInvoices, setInvoicesResult)

  const loadInvoices = useCallback(() => {
    const area = searchFilters.area.join(',')
    const status = searchFilters.status
    const distinctiveDetails = searchFilters.distinctiveDetails.join(',')
    const params: InvoiceSearchParams = {
      area: area.length > 0 ? area : undefined,
      unit: searchFilters.unit ? searchFilters.unit : undefined,
      status: status.length > 0 ? status : undefined,
      distinctions: distinctiveDetails ? distinctiveDetails : undefined,
      searchTerms: debouncedSearchTerms ? debouncedSearchTerms : undefined,
      periodStart: searchFilters.startDate?.formatIso(),
      periodEnd: searchFilters.endDate?.formatIso()
    }
    reloadInvoices(
      state.page,
      pageSize,
      state.sortBy,
      state.sortDirection,
      params
    )
  }, [
    state.page,
    state.sortBy,
    state.sortDirection,
    pageSize,
    searchFilters,
    debouncedSearchTerms,
    reloadInvoices
  ])

  const refreshInvoices = useCallback(
    () => createInvoices().then(() => dispatch({ type: 'RELOAD_INVOICES' })),
    [dispatch]
  )

  const send = useCallback(
    ({
      invoiceDate,
      dueDate
    }: {
      invoiceDate: LocalDate
      dueDate: LocalDate
    }) => {
      const request = state.allInvoicesToggle
        ? sendInvoicesByDate(
            invoiceDate,
            dueDate,
            searchFilters.area,
            searchFilters.startDate,
            searchFilters.endDate,
            searchFilters.useCustomDatesForInvoiceSending
          )
        : sendInvoices(Object.keys(state.checkedInvoices), invoiceDate, dueDate)
      return request
    },
    [
      dispatch,
      state.checkedInvoices,
      state.allInvoicesToggle,
      searchFilters.area,
      searchFilters.startDate,
      searchFilters.endDate,
      searchFilters.useCustomDatesForInvoiceSending
    ]
  )

  useEffect(() => {
    loadInvoices()
  }, [loadInvoices])

  useEffect(() => {
    if (state.needsReload) {
      dispatch({ type: 'INVOICE_RELOAD_DONE' })
      loadInvoices()
    }
  }, [state.needsReload, loadInvoices])

  return {
    dispatch,
    page: state.page,
    invoices: state.invoices,
    pages: state.invoiceTotals?.pages,
    total: state.invoiceTotals?.total,
    sortBy: state.sortBy,
    sortDirection: state.sortDirection,
    checkedInvoices: state.checkedInvoices,
    allInvoicesToggle: state.allInvoicesToggle,
    showModal: state.showModal,
    searchFilters,
    refreshInvoices,
    sendInvoices: send
  }
}
