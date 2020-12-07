// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useCallback, useContext, useEffect, useMemo, useState } from 'react'
import LocalDate from '@evaka/lib-common/src/local-date'
import { Result } from '~api'
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

type State = {
  page: number
  sortBy: SortByInvoices
  sortDirection: SearchOrder
  invoices: Record<number, Result<InvoiceSummary[]>>
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

const useActions = (setState: React.Dispatch<React.SetStateAction<State>>) =>
  useMemo(
    () => ({
      setPage: (page: number) => setState((s) => ({ ...s, page })),
      setSortBy: (sortBy: SortByInvoices) =>
        setState((s) => ({ ...s, sortBy })),
      setSortDirection: (sortDirection: SearchOrder) =>
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
                page.map((invoice) => [invoice.id, true as const])
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
    []
  )

export type InvoicesActions = ReturnType<typeof useActions>

export function useInvoicesState() {
  const {
    invoices: { searchFilters, debouncedSearchTerms }
  } = useContext(InvoicingUiContext)
  const [state, setState] = useState(initialState)
  const actions = useActions(setState)

  const setInvoicesResult = useCallback(
    (result: Result<InvoiceSearchResult>) => {
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
    [setState, state.page]
  )

  const loadInvoices = useRestApi(getInvoices, setInvoicesResult)
  const reloadInvoices = useCallback(() => {
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
    loadInvoices(
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
    searchFilters.area,
    searchFilters.status,
    searchFilters.distinctiveDetails,
    searchFilters.unit,
    searchFilters.startDate,
    searchFilters.endDate,
    debouncedSearchTerms,
    loadInvoices
  ])

  const refreshInvoices = useCallback(async () => {
    await createInvoices()
    reloadInvoices()
  }, [reloadInvoices])

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
