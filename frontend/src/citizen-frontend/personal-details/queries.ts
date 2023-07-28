// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import { createQueryKeys } from '../query'

import {
  getNotificationSettings,
  updateNotificationSettings,
  updatePersonalData
} from './api'

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
