// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RedisClient } from 'redis'
import { fromCallback } from './promise-utils'

// A RedisClient wrapper that uses promises instead of callbacks
export default class AsyncRedisClient {
  constructor(private client: RedisClient) {}
  async get(key: string): Promise<string | undefined> {
    return fromCallback<string | undefined>((cb) => this.client.get(key, cb))
  }
  async del(...keys: string[]): Promise<number> {
    return fromCallback<number>((cb) => this.client.del(...keys, cb))
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
