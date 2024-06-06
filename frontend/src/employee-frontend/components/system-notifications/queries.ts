// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import {
  deleteSystemNotification,
  getAllSystemNotifications,
  putSystemNotification
} from '../../generated/api-clients/systemnotifications'
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('systemNotifications', {
  all: () => ['all']
})

export const allSystemNotificationsQuery = query({
  api: getAllSystemNotifications,
  queryKey: queryKeys.all
})

export const putSystemNotificationMutation = mutation({
  api: putSystemNotification,
  invalidateQueryKeys: () => [queryKeys.all()]
})

export const deleteSystemNotificationMutation = mutation({
  api: deleteSystemNotification,
  invalidateQueryKeys: () => [queryKeys.all()]
})
