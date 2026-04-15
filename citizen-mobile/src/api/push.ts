// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { api } from './client'

export const upsertPushSubscription = (
  token: string,
  deviceId: string,
  expoPushToken: string
) =>
  api<void>('/citizen-mobile/push-subscriptions/v1', {
    method: 'POST',
    token,
    body: { deviceId, expoPushToken }
  })

export const deletePushSubscription = (token: string, deviceId: string) =>
  api<void>(
    `/citizen-mobile/push-subscriptions/v1?deviceId=${encodeURIComponent(deviceId)}`,
    {
      method: 'DELETE',
      token
    }
  )
