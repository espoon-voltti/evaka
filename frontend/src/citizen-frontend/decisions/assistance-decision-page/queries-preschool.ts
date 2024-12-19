// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AssistanceNeedPreschoolDecisionId } from 'lib-common/generated/api-types/shared'
import { mutation, query } from 'lib-common/query'

import {
  getAssistanceNeedPreschoolDecision,
  getAssistanceNeedPreschoolDecisions,
  getAssistanceNeedPreschoolDecisionUnreadCount,
  markAssistanceNeedPreschoolDecisionAsRead
} from '../../generated/api-clients/assistanceneed'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('assistancePreschoolDecisions', {
  all: () => ['all'],
  detail: (id: AssistanceNeedPreschoolDecisionId) => ['preschoolDecisions', id],
  unreadCounts: () => ['unreadCounts']
})

export const assistanceNeedPreschoolDecisionsQuery = query({
  api: getAssistanceNeedPreschoolDecisions,
  queryKey: queryKeys.all
})

export const assistanceNeedPreschoolDecisionQuery = query({
  api: getAssistanceNeedPreschoolDecision,
  queryKey: ({ id }) => queryKeys.detail(id)
})

export const assistanceNeedPreschoolDecisionUnreadCountsQuery = query({
  api: getAssistanceNeedPreschoolDecisionUnreadCount,
  queryKey: queryKeys.unreadCounts
})

export const markAssistanceNeedPreschoolDecisionAsReadMutation = mutation({
  api: markAssistanceNeedPreschoolDecisionAsRead,
  invalidateQueryKeys: () => [
    assistanceNeedPreschoolDecisionUnreadCountsQuery().queryKey
  ]
})
