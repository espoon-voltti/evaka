// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PushSettings } from 'lib-common/generated/api-types/webpush'
import { JsonOf } from 'lib-common/json'

import { client } from '../client'

export async function getPushSettings(): Promise<PushSettings> {
  const response = await client.get<JsonOf<PushSettings>>(
    '/mobile-devices/push-settings'
  )
  return response.data
}

export async function setPushSettings(settings: PushSettings): Promise<void> {
  const body: JsonOf<PushSettings> = settings
  await client.put('/mobile-devices/push-settings', body)
}
