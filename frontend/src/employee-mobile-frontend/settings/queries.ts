// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PushSettings } from 'lib-common/generated/api-types/webpush'
import { mutation, query } from 'lib-common/query'

import { createQueryKeys } from '../query'

import { getPushSettings, setPushSettings } from './api'

const queryKeys = createQueryKeys('settings', {
  push: () => ['push']
})

export const pushSettingsQuery = query({
  api: getPushSettings,
  queryKey: queryKeys.push
})

export const pushSettingsMutation = mutation({
  api: (settings: PushSettings) => setPushSettings(settings),
  invalidateQueryKeys: (_) => [queryKeys.push()]
})
