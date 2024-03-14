// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import {
  acceptDecision,
  getApplicationDecisions,
  getDecisions,
  getGuardianApplicationNotifications,
  getLiableCitizenFinanceDecisions,
  rejectDecision
} from '../generated/api-clients/application'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('applicationDecisions', {
  all: () => ['all'],
  byApplication: (applicationId: UUID) => ['decisions', applicationId],
  notifications: () => ['notifications'],
  financeDecisions: () => ['financeDecisions']
})

export const decisionsQuery = query({
  api: getDecisions,
  queryKey: queryKeys.all
})

export const financeDecisionsQuery = query({
  api: getLiableCitizenFinanceDecisions,
  queryKey: queryKeys.financeDecisions
})

export const decisionsOfApplicationQuery = query({
  api: getApplicationDecisions,
  queryKey: ({ applicationId }) => queryKeys.byApplication(applicationId)
})

export const applicationNotificationsQuery = query({
  api: getGuardianApplicationNotifications,
  queryKey: queryKeys.notifications
})

export const acceptDecisionMutation = mutation({
  api: acceptDecision,
  invalidateQueryKeys: ({ applicationId }) => [
    decisionsOfApplicationQuery({ applicationId }).queryKey,
    applicationNotificationsQuery().queryKey
  ]
})

export const rejectDecisionMutation = mutation({
  api: rejectDecision,
  invalidateQueryKeys: ({ applicationId }) => [
    decisionsOfApplicationQuery({ applicationId }).queryKey,
    applicationNotificationsQuery().queryKey
  ]
})
