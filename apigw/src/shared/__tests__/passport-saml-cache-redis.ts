// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { CacheProvider } from 'passport-saml'
import type { RedisClient } from 'redis'
import redis from 'redis-mock'
import redisCacheProvider from '../auth/passport-saml-cache-redis'
import { fromCallback } from '../promise-utils'

const ttlSeconds = 1
let redisClient: RedisClient
let cache: CacheProvider
jest.mock('redis', () => redis)

beforeAll(() => {
  redisClient = redis.createClient()
})

beforeEach(() => {
  cache = redisCacheProvider(redisClient, {
    ttlSeconds,
    keyPrefix: 'test-prefix:'
  })
})

afterEach(async () => {
  await fromCallback((cb) => redisClient.flushall(cb))
})

afterAll(() => {
  redisClient.end()
})

describe('passport-saml-cache-redis', () => {
  describe('constructor', () => {
    test('throws an error if ttlSeconds is not a positive integer', () => {
      expect(() =>
        redisCacheProvider(redisClient, {
          ttlSeconds: -1,
          keyPrefix: 'test-prefix:'
        })
      ).toThrowError('ttlSeconds must be a positive integer')
      expect(() =>
        redisCacheProvider(redisClient, {
          ttlSeconds: 1.5,
          keyPrefix: 'test-prefix:'
        })
      ).toThrowError('ttlSeconds must be a positive integer')
    })

    test('using different keyPrefixes creates separate caches', async () => {
      const firstCache = redisCacheProvider(redisClient, {
        ttlSeconds,
        keyPrefix: 'first-prefix:'
      })
      const secondCache = redisCacheProvider(redisClient, {
        ttlSeconds,
        keyPrefix: 'second-prefix:'
      })

      await firstCache.saveAsync('key', 'first-val')
      await secondCache.saveAsync('key', 'second-val')

      const firstRes = await firstCache.getAsync('key')
      const secondRes = await secondCache.getAsync('key')
      expect(firstRes).toBe('first-val')
      expect(secondRes).toBe('second-val')
    })
  })

  describe('get()', () => {
    test('returns null if key does not exist', async () => {
      const res = await cache.getAsync('key')
      expect(res).toBeNull()
    })

    test('returns the value if key exists', async () => {
      await cache.saveAsync('key', 'val')
      const res = await cache.getAsync('key')
      expect(res).toBe('val')
    })
  })

  describe('save()', () => {
    test('returns the new value & timestamp if key does not exist', async () => {
      const result = await cache.saveAsync('key', 'val')
      expect(result?.createdAt).toBeGreaterThan(0)
      expect(result?.value).toBe('val')
    })

    test('returns null if the key already exists', async () => {
      await cache.saveAsync('key', 'val1')
      const result = await cache.saveAsync('key', 'val2')
      expect(result).toBeNull()
    })
  })

  describe('remove()', () => {
    it('returns null if key does not exist', async () => {
      const result = await cache.removeAsync('key')
      expect(result).toBeNull()
    })

    it('returns the key if it existed', async () => {
      await cache.saveAsync('key', 'val')
      expect(await cache.removeAsync('key')).toBe('key')
      expect(await cache.removeAsync('key')).toBeNull()
    })
  })

  describe('cache expiration', () => {
    test('keys are expired and deleted from cache automatically', async () => {
      await cache.saveAsync('key', 'val')

      expect(await cache.getAsync('key')).toBe('val')

      // Unfortunately there's not currently an reasonably easy way to mock
      // the timer used by redis-mock so actually waiting for the duration of
      // TTL is necessary.
      await new Promise((resolve) =>
        setTimeout(resolve, ttlSeconds * 1.1 * 1000)
      )

      expect(await cache.getAsync('key')).toBeNull()
    })
  })
})
