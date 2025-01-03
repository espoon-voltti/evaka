// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'

import {
  ignoreVoucherValueDecisionDrafts,
  searchVoucherValueDecisions,
  sendVoucherValueDecisionDrafts,
  unignoreVoucherValueDecisionDrafts
} from '../../generated/api-clients/invoicing'
import { getVoucherValueDecisionMetadata } from '../../generated/api-clients/process'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('voucherValueDecisions', {
  searchAll: () => ['search'],
  search: (args: Arg0<typeof searchVoucherValueDecisions>) => ['search', args],
  voucherValueDecisionMetadata: (
    args: Arg0<typeof getVoucherValueDecisionMetadata>
  ) => ['voucherValueDecisionMetadata', args]
})

export const searchVoucherValueDecisionsQuery = query({
  api: searchVoucherValueDecisions,
  queryKey: queryKeys.search
})

export const voucherValueDecisionMetadataQuery = query({
  api: getVoucherValueDecisionMetadata,
  queryKey: queryKeys.voucherValueDecisionMetadata
})

export const sendVoucherValueDecisionDraftsMutation = mutation({
  api: sendVoucherValueDecisionDrafts,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})

export const ignoreVoucherValueDecisionDraftsMutation = mutation({
  api: ignoreVoucherValueDecisionDrafts,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})

export const unignoreVoucherValueDecisionDraftsMutation = mutation({
  api: unignoreVoucherValueDecisionDrafts,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})
