// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import {
  getNotificationSettings,
  updateNotificationSettings,
  updatePersonalData
} from '../generated/api-clients/pis'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('personalDetails', {
  notificationSettings: () => ['notificationSettings']
})

export const updatePersonalDetailsMutation = mutation({
  api: updatePersonalData
})

export const notificationSettingsQuery = query({
  api: getNotificationSettings,
  queryKey: queryKeys.notificationSettings
})

export const updateNotificationSettingsMutation = mutation({
  api: updateNotificationSettings,
  invalidateQueryKeys: () => [queryKeys.notificationSettings()]
})
