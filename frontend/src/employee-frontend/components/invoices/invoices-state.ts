// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  Dispatch,
  SetStateAction,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react'

import { Result, wrapResult } from 'lib-common/api'
import {
  InvoiceSortParam,
  InvoiceSummaryResponse,
  PagedInvoiceSummaryResponses,
  SearchInvoicesRequest,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { useRestApi } from 'lib-common/utils/useRestApi'

import {
  createDraftInvoices,
  searchInvoices,
  sendInvoices,
  sendInvoicesByDate
} from '../../generated/api-clients/invoicing'
import { InvoicingUiContext } from '../../state/invoicing-ui'

const createDraftInvoicesResult = wrapResult(createDraftInvoices)
const searchInvoicesResult = wrapResult(searchInvoices)
const sendInvoicesResult = wrapResult(sendInvoices)
const sendInvoicesByDateResult = wrapResult(sendInvoicesByDate)

const pageSize = 200

type State = {
  page: number
  sortBy: InvoiceSortParam
  sortDirection: SortDirection
  invoices: Record<number, Result<InvoiceSummaryResponse[]>>
  invoiceTotals?: { total: number; pages: number }
  checkedInvoices: Record<string, true>
  allInvoicesToggle: boolean
  showModal: boolean
}

const initialState: State = {
  page: 1,
  sortBy: 'HEAD_OF_FAMILY',
  sortDirection: 'ASC',
  invoices: {},
  checkedInvoices: {},
  allInvoicesToggle: false,
  showModal: false
}

const useActions = (setState: Dispatch<SetStateAction<State>>) =>
  useMemo(
    () => ({
      setPage: (page: number) => setState((s) => ({ ...s, page })),
      setSortBy: (sortBy: InvoiceSortParam) =>
        setState((s) => ({ ...s, sortBy })),
      setSortDirection: (sortDirection: SortDirection) =>
        setState((s) => ({ ...s, sortDirection })),
      openModal: () => setState((s) => ({ ...s, showModal: true })),
      closeModal: () => setState((s) => ({ ...s, showModal: false })),
      toggleChecked: (invoice: string) =>
        setState((s) => {
          if (s.checkedInvoices[invoice]) {
            const { [invoice]: _, ...rest } = s.checkedInvoices
            return {
              ...s,
              checkedInvoices: rest
            }
          } else {
            return {
              ...s,
              checkedInvoices: {
                ...s.checkedInvoices,
                [invoice]: true
              }
            }
          }
        }),
      clearChecked: () =>
        setState((s) => ({
          ...s,
          checkedInvoices: {},
          allInvoicesToggle: false
        })),
      checkAll: () =>
        setState((s) => {
          const currentPage = s.invoices[s.page]
          const checked: Record<string, true> = currentPage
            .map((page) =>
              Object.fromEntries(
                page.map((invoice) => [invoice.data.id, true as const])
              )
            )
            .getOrElse({})
          return {
            ...s,
            checkedInvoices: {
              ...s.checkedInvoices,
              ...checked
            }
          }
        }),
      allInvoicesToggle: () =>
        setState((s) => ({ ...s, allInvoicesToggle: !s.allInvoicesToggle }))
    }),
    [setState]
  )

export type InvoicesActions = ReturnType<typeof useActions>

export function useInvoicesState() {
  const {
    invoices: { searchFilters, debouncedSearchTerms }
  } = useContext(InvoicingUiContext)
  const [state, setState] = useState(initialState)
  const actions = useActions(setState)

  const setInvoicesResult = useCallback(
    (result: Result<PagedInvoiceSummaryResponses>) => {
      setState((previousState) => ({
        ...previousState,
        invoices: {
          ...state.invoices,
          [state.page]: result.map((r) => r.data)
        },
        invoiceTotals: result
          .map((r) => ({ total: r.total, pages: r.pages }))
          .getOrElse(previousState.invoiceTotals)
      }))
    },
    [setState, state.page] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const loadInvoices = useRestApi(searchInvoicesResult, setInvoicesResult)
  const reloadInvoices = useCallback(() => {
    const { startDate, endDate } = searchFilters
    if (startDate && endDate && startDate.isAfter(endDate)) {
      return
    }

    const status = searchFilters.status
    const params: SearchInvoicesRequest = {
      page: state.page,
      pageSize,
      sortBy: state.sortBy,
      sortDirection: state.sortDirection,
      area: searchFilters.area,
      unit: searchFilters.unit ?? null,
      status: status.length > 0 ? [status] : null,
      distinctions: searchFilters.distinctiveDetails,
      searchTerms: debouncedSearchTerms ? debouncedSearchTerms : null,
      periodStart: startDate ?? null,
      periodEnd: endDate ?? null
    }
    void loadInvoices({ body: params })
  }, [
    state.page,
    state.sortBy,
    state.sortDirection,
    searchFilters,
    debouncedSearchTerms,
    loadInvoices
  ])

  const refreshInvoices = useCallback(async () => {
    await createDraftInvoicesResult()
    reloadInvoices()
  }, [reloadInvoices])

  const send = useCallback(
    ({
      invoiceDate,
      dueDate
    }: {
      invoiceDate: LocalDate
      dueDate: LocalDate
    }) =>
      state.allInvoicesToggle
        ? sendInvoicesByDateResult({
            body: {
              invoiceDate,
              dueDate,
              areas: searchFilters.area,
              from:
                searchFilters.startDate &&
                searchFilters.useCustomDatesForInvoiceSending
                  ? searchFilters.startDate
                  : LocalDate.todayInSystemTz().withDate(1),
              to:
                searchFilters.endDate &&
                searchFilters.useCustomDatesForInvoiceSending
                  ? searchFilters.endDate
                  : LocalDate.todayInSystemTz().lastDayOfMonth()
            }
          })
        : sendInvoicesResult({
            invoiceDate,
            dueDate,
            body: Object.keys(state.checkedInvoices)
          }),
    [
      state.checkedInvoices,
      state.allInvoicesToggle,
      searchFilters.area,
      searchFilters.startDate,
      searchFilters.endDate,
      searchFilters.useCustomDatesForInvoiceSending
    ]
  )

  const onSendSuccess = useCallback(() => {
    setState((s) => ({ ...s, showModal: false, checkedInvoices: {} }))
    reloadInvoices()
  }, [reloadInvoices])

  useEffect(() => {
    reloadInvoices()
  }, [reloadInvoices])

  return {
    actions,
    state,
    searchFilters,
    reloadInvoices,
    refreshInvoices,
    sendInvoices: send,
    onSendSuccess
  }
}
