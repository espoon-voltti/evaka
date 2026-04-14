// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { client } from '../api-client'

export type CitizenPushCategory = 'URGENT_MESSAGE' | 'MESSAGE' | 'BULLETIN'

export interface SubscribeRequest {
  endpoint: string
  ecdhKey: number[]
  authSecret: number[]
  enabledCategories: CitizenPushCategory[]
  userAgent: string | null
}

export interface SubscribeResponse {
  sentTest: boolean
}

export async function getVapidKey(): Promise<string | null> {
  try {
    const { data } = await client.get<{ publicKey: string }>('/citizen/web-push/vapid-key')
    return data.publicKey
  } catch (err) {
    if (
      err &&
      typeof err === 'object' &&
      'response' in err &&
      (err as { response?: { status?: number } }).response?.status === 503
    ) {
      return null
    }
    throw err
  }
}

export async function putSubscription(
  body: SubscribeRequest
): Promise<SubscribeResponse> {
  const { data } = await client.put<SubscribeResponse>('/citizen/web-push/subscription', body)
  return data
}

export async function deleteSubscription(endpoint: string): Promise<void> {
  await client.delete('/citizen/web-push/subscription', { data: { endpoint } })
}

export async function postTest(): Promise<void> {
  await client.post('/citizen/web-push/test')
}
