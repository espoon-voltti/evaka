// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import {
  getPushSettings,
  setPushSettings
} from '../generated/api-clients/webpush'
import { createQueryKeys } from '../query'

const queryKeys = createQueryKeys('settings', {
  push: () => ['push']
})

export const pushSettingsQuery = query({
  api: getPushSettings,
  queryKey: queryKeys.push
})

export const pushSettingsMutation = mutation({
  api: setPushSettings,
  invalidateQueryKeys: (_) => [queryKeys.push()]
})
