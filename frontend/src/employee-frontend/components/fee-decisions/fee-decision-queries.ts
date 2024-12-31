// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { Arg0 } from 'lib-common/types'

import {
  confirmFeeDecisionDrafts,
  ignoreFeeDecisionDrafts,
  searchFeeDecisions,
  unignoreFeeDecisionDrafts
} from '../../generated/api-clients/invoicing'
import { getFeeDecisionMetadata } from '../../generated/api-clients/process'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('feeDecisions', {
  searchAll: () => ['search'],
  search: (args: Arg0<typeof searchFeeDecisions>) => ['search', args],
  feeDecisionMetadata: (args: Arg0<typeof getFeeDecisionMetadata>) => [
    'feeDecisionMetadata',
    args
  ]
})

export const searchFeeDecisionsQuery = query({
  api: searchFeeDecisions,
  queryKey: queryKeys.search
})

export const feeDecisionMetadataQuery = query({
  api: getFeeDecisionMetadata,
  queryKey: queryKeys.feeDecisionMetadata
})

export const confirmFeeDecisionDraftsMutation = mutation({
  api: confirmFeeDecisionDrafts,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})

export const ignoreFeeDecisionDraftsMutation = mutation({
  api: ignoreFeeDecisionDrafts,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})

export const unignoreFeeDecisionDraftsMutation = mutation({
  api: unignoreFeeDecisionDrafts,
  invalidateQueryKeys: () => [queryKeys.searchAll()]
})
