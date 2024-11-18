// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CacheProvider } from '@node-saml/node-saml'

import { RedisClient } from '../redis-client.js'

export interface ProviderOptions {
  /**
   * Entries older than this are deleted automatically by Redis' expire mechanism
   */
  ttlSeconds?: number
  /**
   * Prefix for cache item keys in Redis.
   * If creating multiple saml instances targeting the same
   * Redis database, you should set a unique keyPrefix per strategy.
   */
  keyPrefix: string
}

/**
 * Custom node-saml CacheProvider for Redis.
 *
 * This allows the use of validateInResponseTo in multi-instance environments
 * where the instances obviously cannot share access to the same
 * InMemoryCacheProvider. Instead, a shared Redis cache is used.
 *
 * This cache provider also supports using it with multiple saml instances
 * simultaneously by allowing keyPrefix configuration.
 */
export default function redisCacheProvider(
  client: RedisClient,
  options: ProviderOptions
): CacheProvider {
  const { ttlSeconds = 60 * 60, keyPrefix } = options

  if (!Number.isInteger(ttlSeconds) || ttlSeconds <= 0) {
    throw new Error('ttlSeconds must be a positive integer')
  }

  return {
    getAsync: (key: string) => client.get(`${keyPrefix}${key}`),
    saveAsync: async (key: string, value: string) => {
      const reply = await client.get(`${keyPrefix}${key}`)
      if (reply !== null) return null
      await client.set(`${keyPrefix}${key}`, value, { EX: ttlSeconds })
      return { createdAt: new Date().getTime(), value }
    },
    removeAsync: async (key: string) => {
      const count = await client.del(`${keyPrefix}${key}`)
      if (count === 0) return null
      return key
    }
  }
}
