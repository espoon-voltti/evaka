// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../../query'

import {
  getAssistanceDecisions,
  getAssistanceDecisionUnreadCounts,
  getAssitanceDecision,
  markAssistanceNeedDecisionAsRead
} from './api'

const queryKeys = createQueryKeys('assistanceDecisions', {
  all: () => ['all'],
  detail: (id: UUID) => ['decisions', id],
  unreadCounts: () => ['unreadCounts']
})

export const assistanceDecisionsQuery = query({
  api: getAssistanceDecisions,
  queryKey: queryKeys.all
})

export const assistanceDecisionQuery = query({
  api: getAssitanceDecision,
  queryKey: queryKeys.detail
})

export const assistanceDecisionUnreadCountsQuery = query({
  api: getAssistanceDecisionUnreadCounts,
  queryKey: queryKeys.unreadCounts
})

export const markAssistanceNeedDecisionAsReadMutation = mutation({
  api: markAssistanceNeedDecisionAsRead,
  invalidateQueryKeys: () => [assistanceDecisionUnreadCountsQuery().queryKey]
})
