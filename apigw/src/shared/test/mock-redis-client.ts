// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { RedisClient } from '../redis-client'

export class MockRedisClient implements RedisClient {
  private time: number
  private db: Record<string, { value: string; expires: number | null }>

  isReady: boolean

  constructor() {
    this.isReady = true
    this.time = 0
    this.db = {}
  }

  advanceTime(amount: number) {
    this.time += amount
    this.db = Object.fromEntries(
      Object.entries(this.db).filter(([, record]) => {
        return record.expires === null || record.expires >= this.time
      })
    )
  }

  get(key: string): Promise<string | null> {
    const record = this.db[key]
    if (!record) return Promise.resolve(null)
    return Promise.resolve(record.value)
  }

  set(
    key: string,
    value: string,
    options?: { EX: number }
  ): Promise<string | null> {
    this.db[key] = {
      value,
      expires: options?.EX ? this.time + options.EX : null
    }
    return Promise.resolve(value)
  }

  del(key: string | string[]): Promise<number> {
    const keys = typeof key === 'string' ? [key] : key
    let count = 0
    keys.forEach((k) => {
      if (k in this.db) count++
      delete this.db[k]
    })
    return Promise.resolve(count)
  }

  expire(key: string, seconds: number): Promise<boolean> {
    const record = this.db[key]
    if (!record) return Promise.resolve(false)
    record.expires = this.time + seconds
    return Promise.resolve(true)
  }

  ping(): Promise<string> {
    return Promise.resolve('PONG')
  }
}
