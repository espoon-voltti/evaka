// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AssistanceNeedDecisionId } from 'lib-common/generated/api-types/shared'
import { mutation, query } from 'lib-common/query'

import {
  getAssistanceNeedDecision,
  getAssistanceNeedDecisions,
  getAssistanceNeedDecisionUnreadCount,
  markAssistanceNeedDecisionAsRead
} from '../../generated/api-clients/assistanceneed'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('assistanceDecisions', {
  all: () => ['all'],
  detail: (id: AssistanceNeedDecisionId) => ['decisions', id],
  unreadCounts: () => ['unreadCounts']
})

export const assistanceDecisionsQuery = query({
  api: getAssistanceNeedDecisions,
  queryKey: queryKeys.all
})

export const assistanceDecisionQuery = query({
  api: getAssistanceNeedDecision,
  queryKey: ({ id }) => queryKeys.detail(id)
})

export const assistanceDecisionUnreadCountsQuery = query({
  api: getAssistanceNeedDecisionUnreadCount,
  queryKey: queryKeys.unreadCounts
})

export const markAssistanceNeedDecisionAsReadMutation = mutation({
  api: markAssistanceNeedDecisionAsRead,
  invalidateQueryKeys: () => [assistanceDecisionUnreadCountsQuery().queryKey]
})
