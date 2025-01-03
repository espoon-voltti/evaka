// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'

import {
  confirmDraftPayments,
  createPaymentDrafts,
  deleteDraftPayments,
  revertPaymentsToDrafts,
  searchPayments,
  sendPayments
} from '../../generated/api-clients/invoicing'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('payments', {
  search: (args: Arg0<typeof searchPayments>) => ['search', args],
  searchAll: () => ['search']
})

export const searchPaymentsQuery = query({
  api: searchPayments,
  queryKey: queryKeys.search
})

export const createPaymentDraftsMutation = mutation({
  api: createPaymentDrafts,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})

export const sendPaymentsMutation = mutation({
  api: sendPayments,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})

export const confirmDraftPaymentsMutation = mutation({
  api: confirmDraftPayments,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})

export const deleteDraftPaymentsMutation = mutation({
  api: deleteDraftPayments,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})

export const revertPaymentsToDraftsMutation = mutation({
  api: revertPaymentsToDrafts,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})
