// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getAssistanceNeedDecision,
  getAssistanceNeedDecisions,
  getAssistanceNeedDecisionUnreadCount,
  markAssistanceNeedDecisionAsRead
} from '../../generated/api-clients/assistanceneed'

const q = new Queries()

export const assistanceDecisionsQuery = q.query(getAssistanceNeedDecisions)

export const assistanceDecisionQuery = q.query(getAssistanceNeedDecision)

export const assistanceDecisionUnreadCountsQuery = q.query(
  getAssistanceNeedDecisionUnreadCount
)

export const markAssistanceNeedDecisionAsReadMutation = q.mutation(
  markAssistanceNeedDecisionAsRead,
  [() => assistanceDecisionUnreadCountsQuery()]
)
