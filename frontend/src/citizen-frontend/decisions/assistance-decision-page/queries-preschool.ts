// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../../query'

import {
  getAssistanceNeedPreschoolDecision,
  getAssistanceNeedPreschoolDecisions,
  getAssistanceNeedPreschoolDecisionUnreadCounts,
  markAssistanceNeedPreschoolDecisionAsRead
} from './api-preschool'

const queryKeys = createQueryKeys('assistancePreschoolDecisions', {
  all: () => ['all'],
  detail: (id: UUID) => ['preschoolDecisions', id],
  unreadCounts: () => ['unreadCounts']
})

export const assistanceNeedPreschoolDecisionsQuery = query({
  api: getAssistanceNeedPreschoolDecisions,
  queryKey: queryKeys.all
})

export const assistanceNeedPreschoolDecisionQuery = query({
  api: getAssistanceNeedPreschoolDecision,
  queryKey: queryKeys.detail
})

export const assistanceNeedPreschoolDecisionUnreadCountsQuery = query({
  api: getAssistanceNeedPreschoolDecisionUnreadCounts,
  queryKey: queryKeys.unreadCounts
})

export const markAssistanceNeedPreschoolDecisionAsReadMutation = mutation({
  api: markAssistanceNeedPreschoolDecisionAsRead,
  invalidateQueryKeys: () => [
    assistanceNeedPreschoolDecisionUnreadCountsQuery().queryKey
  ]
})
