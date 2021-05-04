// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { CacheItem, CacheProvider } from 'passport-saml'
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

      await fromCallback<CacheItem | null>((cb) =>
        firstCache.save('key', 'first-val', cb)
      )
      await fromCallback<CacheItem | null>((cb) =>
        secondCache.save('key', 'second-val', cb)
      )

      const firstRes = await fromCallback<string | null>((cb) =>
        firstCache.get('key', cb)
      )
      const secondRes = await fromCallback<string | null>((cb) =>
        secondCache.get('key', cb)
      )
      expect(firstRes).toBe('first-val')
      expect(secondRes).toBe('second-val')
    })
  })

  describe('get()', () => {
    test('returns null if key does not exist', async () => {
      const res = await fromCallback<string | null>((cb) =>
        cache.get('key', cb)
      )
      expect(res).toBeNull()
    })

    test('returns the value if key exists', async () => {
      await fromCallback<CacheItem | null>((cb) => cache.save('key', 'val', cb))
      const res = await fromCallback<string | null>((cb) =>
        cache.get('key', cb)
      )
      expect(res).toBe('val')
    })
  })

  describe('save()', () => {
    test('returns the new value & timestamp if key does not exist', async () => {
      const result = await fromCallback<CacheItem | null>((cb) =>
        cache.save('key', 'val', cb)
      )
      // TODO: Enable once upstream typings are fixed
      // expect(result?.createdAt).toBeGreaterThan(0)
      // TODO: and remove this one
      expect(result?.createdAt).toBeInstanceOf(Date)
      expect(result?.value).toBe('val')
    })

    test('returns null if the key already exists', async () => {
      await fromCallback<CacheItem | null>((cb) =>
        cache.save('key', 'val1', cb)
      )
      const result = await fromCallback<CacheItem | null>((cb) =>
        cache.save('key', 'val2', cb)
      )
      // TODO: Enable once upstream typings are fixed
      // expect(result).toBeNull()
      // TODO: and remove this one
      expect(result?.value).toBeNull()
    })
  })

  describe('remove()', () => {
    it('returns null if key does not exist', async () => {
      const result = await fromCallback<string | null>((cb) =>
        cache.remove('key', cb)
      )
      // TODO: Enable once upstream typings are fixed
      // expect(result).toBeNull()
      // TODO: and remove this one
      expect(result).toBe('')
    })

    it('returns the key if it existed', async () => {
      await fromCallback<CacheItem | null>((cb) => cache.save('key', 'val', cb))
      expect(
        await fromCallback<string | null>((cb) => cache.remove('key', cb))
      ).toBe('key')
      // TODO: Enable once upstream typings are fixed
      // expect(
      //   await fromCallback<string | null>((cb) => cache.remove('key', cb))
      // ).toBeNull()
      // TODO: and remove this one
      expect(
        await fromCallback<string | null>((cb) => cache.remove('key', cb))
      ).toBe('')
    })
  })

  describe('cache expiration', () => {
    test('keys are expired and deleted from cache automatically', async () => {
      await fromCallback<CacheItem | null>((cb) => cache.save('key', 'val', cb))

      expect(
        await fromCallback<string | null>((cb) => cache.get('key', cb))
      ).toBe('val')

      // Unfortunately there's not currently an reasonably easy way to mock
      // the timer used by redis-mock so actually waiting for the duration of
      // TTL is necessary.
      await new Promise((resolve) =>
        setTimeout(resolve, ttlSeconds * 1.1 * 1000)
      )

      expect(
        await fromCallback<string | null>((cb) => cache.get('key', cb))
      ).toBeNull()
    })
  })
})
