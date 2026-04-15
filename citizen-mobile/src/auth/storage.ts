// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as SecureStore from 'expo-secure-store'

const TOKEN_KEY = 'mobile-session-token'

export const tokenStorage = {
  async get(): Promise<string | null> {
    return SecureStore.getItemAsync(TOKEN_KEY)
  },
  async set(token: string): Promise<void> {
    await SecureStore.setItemAsync(TOKEN_KEY, token)
  },
  async clear(): Promise<void> {
    await SecureStore.deleteItemAsync(TOKEN_KEY)
  }
}
