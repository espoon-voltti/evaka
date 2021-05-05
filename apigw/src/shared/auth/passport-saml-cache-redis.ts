// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CacheProvider } from 'passport-saml'
import type { RedisClient } from 'redis'

export interface ProviderOptions {
  /**
   * Entries older than this are deleted automatically by Redis' expire mechanism
   */
  ttlSeconds?: number
  /**
   * Prefix for cache item keys in Redis.
   * If creating multiple passport-saml strategy instances targeting the same
   * Redis database, you should set a unique keyPrefix per strategy.
   */
  keyPrefix: string
}

/**
 * Custom passport-saml CacheProvider for Redis.
 *
 * This allows the use of validateInResponseTo in multi-instance environments
 * where the instances obviously cannot share access to the same
 * InMemoryCacheProvider. Instead, a shared Redis cache is used.
 *
 * This cache provider also supports using it with multiple passport-saml
 * Strategies simultaneously by allowing keyPrefix configuration.
 */
export default function redisCacheProvider(
  client: RedisClient,
  options: ProviderOptions
): CacheProvider {
  const { ttlSeconds = 60 * 60, keyPrefix } = { ...options }

  if (!Number.isInteger(ttlSeconds) || ttlSeconds <= 0) {
    throw new Error('ttlSeconds must be a positive integer')
  }

  return {
    get(key, callback) {
      return client.get(`${keyPrefix}${key}`, callback)
    },
    save(key, value, callback) {
      client.get(`${keyPrefix}${key}`, (err, reply) => {
        if (err) return callback(err, null)
        if (reply !== null) return callback(null, null)

        const cacheItem = { createdAt: new Date().getTime(), value }
        return client.set(
          `${keyPrefix}${key}`,
          value,
          'EX',
          ttlSeconds,
          (err) => callback(err, cacheItem)
        )
      })
    },
    remove(key, callback) {
      return client.del(`${keyPrefix}${key}`, (err, count) => {
        if (err) return callback(err, null)
        if (count === 0) return callback(null, null)
        return callback(null, key)
      })
    }
  }
}
