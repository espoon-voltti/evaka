// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  getAssistanceNeedPreschoolDecision,
  getAssistanceNeedPreschoolDecisions,
  getAssistanceNeedPreschoolDecisionUnreadCount,
  markAssistanceNeedPreschoolDecisionAsRead
} from '../../generated/api-clients/assistanceneed'

const q = new Queries()

export const assistanceNeedPreschoolDecisionsQuery = q.query(
  getAssistanceNeedPreschoolDecisions
)

export const assistanceNeedPreschoolDecisionQuery = q.query(
  getAssistanceNeedPreschoolDecision
)

export const assistanceNeedPreschoolDecisionUnreadCountsQuery = q.query(
  getAssistanceNeedPreschoolDecisionUnreadCount
)

export const markAssistanceNeedPreschoolDecisionAsReadMutation = q.mutation(
  markAssistanceNeedPreschoolDecisionAsRead,
  [() => assistanceNeedPreschoolDecisionUnreadCountsQuery()]
)
