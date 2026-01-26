// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  acceptDecision,
  getDecisions,
  getGuardianApplicationNotifications,
  getLiableCitizenFinanceDecisions,
  getPendingDecisions,
  rejectDecision
} from '../generated/api-clients/application'
import {
  getFeeDecisionMetadata,
  getVoucherValueDecisionMetadata
} from '../generated/api-clients/caseprocess'
import { receivedMessagesQuery } from '../messages/queries'

const q = new Queries()

export const decisionsQuery = q.query(getDecisions)

export const financeDecisionsQuery = q.query(getLiableCitizenFinanceDecisions)

export const pendingDecisionsQuery = q.query(getPendingDecisions)

export const applicationNotificationsQuery = q.query(
  getGuardianApplicationNotifications
)

export const acceptDecisionMutation = q.mutation(acceptDecision, [
  pendingDecisionsQuery,
  applicationNotificationsQuery,
  receivedMessagesQuery
])

export const rejectDecisionMutation = q.mutation(rejectDecision, [
  pendingDecisionsQuery,
  applicationNotificationsQuery,
  receivedMessagesQuery
])

export const feeDecisionMetadataQuery = q.query(getFeeDecisionMetadata)

export const voucherValueDecisionMetadataQuery = q.query(
  getVoucherValueDecisionMetadata
)
