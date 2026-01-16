// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { RedisClient, RedisTransaction } from '../redis-client.ts'

export class MockRedisClient implements RedisClient {
  private time: number
  private db: Record<string, { value: string; expires: number | null }>
  private sets: Record<string, { members: Set<string>; expires: number | null }>

  isReady: boolean

  constructor() {
    this.isReady = true
    this.time = 0
    this.db = {}
    this.sets = {}
  }

  advanceTime(amount: number) {
    this.time += amount
    this.db = Object.fromEntries(
      Object.entries(this.db).filter(([, record]) => {
        return record.expires === null || record.expires >= this.time
      })
    )
    this.sets = Object.fromEntries(
      Object.entries(this.sets).filter(([, record]) => {
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

  expire(key: string, seconds: number): Promise<number> {
    const record = this.db[key]
    if (!record) return Promise.resolve(0)
    record.expires = this.time + seconds
    return Promise.resolve(1)
  }

  incr(key: string): Promise<number> {
    const record = this.db[key] ?? { value: '0', expires: null }
    const value = Number.parseInt(record.value, 10) + 1
    if (Number.isNaN(value)) throw new Error('Not a number')
    this.db[key] = { ...record, value: value.toString() }
    return Promise.resolve(value)
  }

  sAdd(key: string, members: string | string[]): Promise<number> {
    const memberArray = Array.isArray(members) ? members : [members]

    if (!this.sets[key]) {
      this.sets[key] = { members: new Set(), expires: null }
    }

    const set = this.sets[key]
    const initialSize = set.members.size

    memberArray.forEach((member) => set.members.add(member))

    const addedCount = set.members.size - initialSize
    return Promise.resolve(addedCount)
  }

  sMembers(key: string): Promise<string[]> {
    const set = this.sets[key]
    if (!set) return Promise.resolve([])
    return Promise.resolve(Array.from(set.members))
  }

  sRem(key: string, members: string | string[]): Promise<number> {
    const set = this.sets[key]
    if (!set) return Promise.resolve(0)

    const memberArray = Array.isArray(members) ? members : [members]
    let removedCount = 0

    memberArray.forEach((member) => {
      if (set.members.delete(member)) {
        removedCount++
      }
    })

    if (set.members.size === 0) {
      delete this.sets[key]
    }

    return Promise.resolve(removedCount)
  }

  ping(): Promise<string> {
    return Promise.resolve('PONG')
  }

  multi(): RedisTransaction {
    return new MockTransaction(this)
  }
}

class MockTransaction implements RedisTransaction {
  #client: RedisClient
  #queue: (() => Promise<unknown>)[] = []

  constructor(client: RedisClient) {
    this.#client = client
  }
  incr(key: string): RedisTransaction {
    this.#queue.push(async () => {
      await this.#client.incr(key)
    })
    return this
  }
  sAdd(key: string, members: string | string[]): RedisTransaction {
    this.#queue.push(async () => {
      await this.#client.sAdd(key, members)
    })
    return this
  }
  expire(key: string, seconds: number): RedisTransaction {
    this.#queue.push(async () => {
      await this.#client.expire(key, seconds)
    })
    return this
  }
  async exec(): Promise<unknown[]> {
    const returnValues: unknown[] = []
    for (const op of this.#queue) {
      returnValues.push(await op())
    }
    return returnValues
  }
}
