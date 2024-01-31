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

import { Loading, Result } from 'lib-common/api'
import {
  PagedPayments,
  Payment,
  PaymentSortParam,
  SortDirection
} from 'lib-common/generated/api-types/invoicing'
import LocalDate from 'lib-common/local-date'
import { useRestApi } from 'lib-common/utils/useRestApi'

import {
  createPaymentDrafts,
  getPayments,
  sendPayments
} from '../../api/invoicing'
import { InvoicingUiContext } from '../../state/invoicing-ui'

const pageSize = 200

type State = {
  page: number
  pages: number
  total: number
  sortBy: PaymentSortParam
  sortDirection: SortDirection
  payments: Result<Payment[]>
  checkedPayments: Record<string, true>
  showModal: boolean
}

const initialState: State = {
  page: 1,
  pages: 0,
  total: 0,
  sortBy: 'UNIT',
  sortDirection: 'ASC',
  payments: Loading.of(),
  checkedPayments: {},
  showModal: false
}

const useActions = (setState: Dispatch<SetStateAction<State>>) =>
  useMemo(
    () => ({
      setPage: (page: number) => setState((s) => ({ ...s, page })),
      setSortBy: (sortBy: PaymentSortParam) =>
        setState((s) => ({ ...s, sortBy })),
      setSortDirection: (sortDirection: SortDirection) =>
        setState((s) => ({ ...s, sortDirection })),
      openModal: () => setState((s) => ({ ...s, showModal: true })),
      closeModal: () => setState((s) => ({ ...s, showModal: false })),
      toggleChecked: (payment: string) =>
        setState((s) => {
          if (s.checkedPayments[payment]) {
            const { [payment]: _, ...rest } = s.checkedPayments
            return {
              ...s,
              checkedPayments: rest
            }
          } else {
            return {
              ...s,
              checkedPayments: {
                ...s.checkedPayments,
                [payment]: true
              }
            }
          }
        }),
      clearChecked: () =>
        setState((s) => ({
          ...s,
          checkedPayments: {}
        })),
      toggleCheckAll: () =>
        setState((s) => {
          const paymentCount = s.payments
            .map((payments) => payments.length)
            .getOrElse(0)
          const checkedCount = Object.keys(s.checkedPayments).length
          if (checkedCount === paymentCount) {
            return {
              ...s,
              checkedPayments: {}
            }
          }
          const checked: Record<string, true> = s.payments
            .map((payments) =>
              Object.fromEntries(
                payments.map((payment) => [payment.id, true] as const)
              )
            )
            .getOrElse({})
          return {
            ...s,
            checkedPayments: {
              ...s.checkedPayments,
              ...checked
            }
          }
        })
    }),
    [setState]
  )

export type PaymentsActions = ReturnType<typeof useActions>

export function usePaymentsState() {
  const {
    payments: { searchFilters, debouncedSearchTerms }
  } = useContext(InvoicingUiContext)
  const [state, setState] = useState(initialState)
  const actions = useActions(setState)

  const setPaymentsResult = useCallback(
    (result: Result<PagedPayments>) => {
      setState((previousState) => ({
        ...previousState,
        payments: result.map((r) => r.data),
        checkedPayments: {},
        pages: result.map((r) => r.pages).getOrElse(0),
        total: result.map((r) => r.total).getOrElse(0)
      }))
    },
    [setState, state.page] // eslint-disable-line react-hooks/exhaustive-deps
  )

  const loadPayments = useRestApi(getPayments, setPaymentsResult)
  const reloadPayments = useCallback(() => {
    const { paymentDateStart, paymentDateEnd } = searchFilters
    if (
      paymentDateStart &&
      paymentDateEnd &&
      paymentDateStart.isAfter(paymentDateEnd)
    ) {
      return
    }

    void loadPayments({
      ...searchFilters,
      searchTerms: debouncedSearchTerms,
      page: state.page,
      pageSize,
      sortBy: state.sortBy,
      sortDirection: state.sortDirection
    })
  }, [
    state.page,
    state.sortBy,
    state.sortDirection,
    searchFilters,
    debouncedSearchTerms,
    loadPayments
  ])

  const createPayments = useCallback(async () => {
    await createPaymentDrafts()
    reloadPayments()
  }, [reloadPayments])

  const send = useCallback(
    ({
      paymentDate,
      dueDate
    }: {
      paymentDate: LocalDate
      dueDate: LocalDate
    }) =>
      sendPayments(Object.keys(state.checkedPayments), paymentDate, dueDate),
    [state.checkedPayments]
  )

  const onSendSuccess = useCallback(() => {
    setState((s) => ({ ...s, showModal: false, checkedPayments: {} }))
    reloadPayments()
  }, [reloadPayments])

  useEffect(() => {
    reloadPayments()
  }, [reloadPayments])

  return {
    actions,
    state,
    searchFilters,
    createPayments,
    reloadPayments,
    sendPayments: send,
    onSendSuccess
  }
}
