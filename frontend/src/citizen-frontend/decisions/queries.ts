// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  acceptDecision,
  getApplicationDecisions,
  getDecisions,
  getGuardianApplicationNotifications,
  getLiableCitizenFinanceDecisions,
  rejectDecision
} from '../generated/api-clients/application'
import { getFeeDecisionMetadata } from '../generated/api-clients/caseprocess'
import { receivedMessagesQuery } from '../messages/queries'

const q = new Queries()

export const decisionsQuery = q.query(getDecisions)

export const financeDecisionsQuery = q.query(getLiableCitizenFinanceDecisions)

export const decisionsOfApplicationQuery = q.query(getApplicationDecisions)

export const applicationNotificationsQuery = q.query(
  getGuardianApplicationNotifications
)

export const acceptDecisionMutation = q.mutation(acceptDecision, [
  ({ applicationId }) => decisionsOfApplicationQuery({ applicationId }),
  applicationNotificationsQuery,
  receivedMessagesQuery
])

export const rejectDecisionMutation = q.mutation(rejectDecision, [
  ({ applicationId }) => decisionsOfApplicationQuery({ applicationId }),
  applicationNotificationsQuery,
  receivedMessagesQuery
])

export const feeDecisionMetadataQuery = q.query(getFeeDecisionMetadata)
