// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { describe, expect, test } from 'vitest'

import { consumeRateLimit } from '../rate-limit.ts'
import { MockRedisClient } from '../test/mock-redis-client.ts'

describe('consumeRateLimit', () => {
  test('allows at most limit attempts even when they arrive simultaneously', async () => {
    const redis = new MockRedisClient()

    const results = await Promise.all(
      Array.from({ length: 100 }, () => consumeRateLimit(redis, 'key', 5, 3600))
    )

    expect(results.filter(({ allowed }) => allowed)).toHaveLength(5)
  })

  test('returns the attempt count of the current window', async () => {
    const redis = new MockRedisClient()

    expect(await consumeRateLimit(redis, 'key', 1, 3600)).toEqual({
      allowed: true,
      count: 1
    })
    expect(await consumeRateLimit(redis, 'key', 1, 3600)).toEqual({
      allowed: false,
      count: 2
    })
  })

  test('window starts at the first attempt and is not extended by later attempts', async () => {
    const redis = new MockRedisClient()

    expect((await consumeRateLimit(redis, 'key', 2, 3600)).allowed).toBe(true)
    redis.advanceTime(3000)
    expect((await consumeRateLimit(redis, 'key', 2, 3600)).allowed).toBe(true)
    redis.advanceTime(500)
    expect((await consumeRateLimit(redis, 'key', 2, 3600)).allowed).toBe(false)
    redis.advanceTime(101)
    expect((await consumeRateLimit(redis, 'key', 2, 3600)).allowed).toBe(true)
  })
})
