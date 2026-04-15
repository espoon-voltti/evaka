// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { randomUUID } from 'expo-crypto'
import * as SecureStore from 'expo-secure-store'

const KEY = 'mobile-device-id'

export async function getOrCreateDeviceId(): Promise<string> {
  const existing = await SecureStore.getItemAsync(KEY)
  if (existing) return existing
  const next = randomUUID()
  await SecureStore.setItemAsync(KEY, next)
  return next
}
