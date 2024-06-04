// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { query } from 'lib-common/query'

import { getCurrentSystemNotificationCitizen } from '../generated/api-clients/systemnotifications'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('login', {
  systemNotifications: () => ['systemNotifications']
})

export const systemNotificationsQuery = query({
  api: getCurrentSystemNotificationCitizen,
  queryKey: queryKeys.systemNotifications
})
