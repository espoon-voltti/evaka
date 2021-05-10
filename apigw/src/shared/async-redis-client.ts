// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RedisClient } from 'redis'
import { fromCallback } from './promise-utils'

// A RedisClient wrapper that uses promises instead of callbacks
export default class AsyncRedisClient {
  constructor(private client: RedisClient) {}
  async get(key: string): Promise<string | null> {
    return fromCallback<string | null>((cb) => this.client.get(key, cb))
  }
  // NOTE: redis-mock currently does not support parsing multiple arguments
  // (see: https://github.com/yeahoffline/redis-mock/pull/178), so AsyncRedisClient
  // should support providing the keys as an array and not use the spread
  // operator until this is fixed upstream.
  async del(keys: string[]): Promise<number>
  async del(...keys: string[]): Promise<number>
  async del(keys: string | string[]) {
    return fromCallback<number>((cb) => this.client.del(keys, cb))
  }
  async expire(key: string, seconds: number): Promise<number> {
    return fromCallback<number>((cb) => this.client.expire(key, seconds, cb))
  }
  async set(
    key: string,
    value: string,
    mode: string,
    duration: number
  ): Promise<'OK' | undefined> {
    return fromCallback<'OK' | undefined>((cb) =>
      this.client.set(key, value, mode, duration, cb)
    )
  }
}
