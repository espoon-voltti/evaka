// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { RedisClient } from './redis-client.ts'

// Increment first and check the returned count: reading the count before
// incrementing lets simultaneous requests all see the same value and pass.
// SET NX starts the window at the first attempt without extending it later.
export async function consumeRateLimit(
  redis: RedisClient,
  key: string,
  limit: number,
  windowSeconds: number
): Promise<{ allowed: boolean; count: number }> {
  const [, count] = await redis
    .multi()
    .set(key, '0', { EX: windowSeconds, NX: true })
    .incr(key)
    .exec()
  return { allowed: Number(count) <= limit, count: Number(count) }
}
