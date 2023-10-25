// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'
import { UUID } from 'lib-common/types'

import { createQueryKeys } from '../query'

import {
  acceptDecision,
  getApplicationNotifications,
  getDecisions,
  getDecisionsOfApplication,
  rejectDecision
} from './api'

const queryKeys = createQueryKeys('applicationDecisions', {
  all: () => ['all'],
  byApplication: (applicationId: UUID) => ['decisions', applicationId],
  notifications: () => ['notifications']
})

export const decisionsQuery = query({
  api: getDecisions,
  queryKey: queryKeys.all
})

export const decisionsOfApplicationQuery = query({
  api: getDecisionsOfApplication,
  queryKey: queryKeys.byApplication
})

export const applicationNotificationsQuery = query({
  api: getApplicationNotifications,
  queryKey: queryKeys.notifications
})

export const acceptDecisionMutation = mutation({
  api: acceptDecision,
  invalidateQueryKeys: ({ applicationId }) => [
    decisionsOfApplicationQuery(applicationId).queryKey,
    applicationNotificationsQuery().queryKey
  ]
})

export const rejectDecisionMutation = mutation({
  api: rejectDecision,
  invalidateQueryKeys: ({ applicationId }) => [
    decisionsOfApplicationQuery(applicationId).queryKey,
    applicationNotificationsQuery().queryKey
  ]
})
