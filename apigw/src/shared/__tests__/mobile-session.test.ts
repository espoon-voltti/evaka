// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { createHash } from 'node:crypto'
import { describe, expect, it } from 'vitest'

import type { RedisClient } from '../redis-client.ts'
import {
  createMobileSession,
  deleteMobileSession,
  getMobileSession
} from '../mobile-session.ts'

interface FakeRedis extends RedisClient {
  store: Map<string, string>
  sets: Map<string, Set<string>>
  ttls: Map<string, number>
}

function fakeRedis(): FakeRedis {
  const store = new Map<string, string>()
  const sets = new Map<string, Set<string>>()
  const ttls = new Map<string, number>()
  const client = {
    isReady: true,
    async get(k: string) {
      return store.get(k) ?? null
    },
    async set(k: string, v: string, opts?: { EX?: number }) {
      store.set(k, v)
      if (opts?.EX) ttls.set(k, opts.EX)
      return 'OK'
    },
    async del(k: string | string[]) {
      const keys = Array.isArray(k) ? k : [k]
      keys.forEach((key) => store.delete(key))
      return keys.length
    },
    async expire(k: string, s: number) {
      ttls.set(k, s)
      return 1
    },
    async incr(_k: string) {
      return 0
    },
    async ping() {
      return 'PONG'
    },
    multi() {
      throw new Error('not implemented')
    },
    async sAdd(k: string, v: string | string[]) {
      const set = sets.get(k) ?? new Set<string>()
      ;(Array.isArray(v) ? v : [v]).forEach((m) => set.add(m))
      sets.set(k, set)
      return 1
    },
    async sMembers(k: string) {
      return Array.from(sets.get(k) ?? [])
    },
    async sRem(k: string, v: string | string[]) {
      const set = sets.get(k) ?? new Set<string>()
      ;(Array.isArray(v) ? v : [v]).forEach((m) => set.delete(m))
      return 1
    },
    store,
    sets,
    ttls
  }
  return client as unknown as FakeRedis
}

const hashOf = (id: string) => createHash('sha256').update(id).digest('hex')
const sessionKey = (token: string) => `mobile-session:${token}`
const indexKey = (id: string) => `usess:${hashOf(id)}`

describe('mobile-session', () => {
  it('creates a session stored under mobile-session:<token> with a long TTL', async () => {
    const redis = fakeRedis()
    const { token, expiresAt } = await createMobileSession(redis, 'citizen-id')
    expect(redis.store.has(sessionKey(token))).toBe(true)
    expect(redis.ttls.get(sessionKey(token))).toBeGreaterThan(60 * 60 * 24) // > 1 day
    expect(expiresAt).toBeGreaterThan(Date.now())
  })

  it('registers the session in the usess:<hash> index so credential updates invalidate it', async () => {
    const redis = fakeRedis()
    const { token } = await createMobileSession(redis, 'citizen-id')
    expect(redis.sets.get(indexKey('citizen-id'))?.has(token)).toBe(true)
  })

  it('getMobileSession returns the session payload and refreshes TTL', async () => {
    const redis = fakeRedis()
    const { token } = await createMobileSession(redis, 'citizen-id')
    redis.ttls.set(sessionKey(token), 1)
    const session = await getMobileSession(redis, token)
    expect(session?.userId).toBe('citizen-id')
    expect(redis.ttls.get(sessionKey(token))).toBeGreaterThan(60)
  })

  it('getMobileSession returns null for unknown token', async () => {
    const redis = fakeRedis()
    expect(await getMobileSession(redis, 'nope')).toBeNull()
  })

  it('deleteMobileSession removes the session and its index entry', async () => {
    const redis = fakeRedis()
    const { token } = await createMobileSession(redis, 'citizen-id')
    await deleteMobileSession(redis, token)
    expect(redis.store.has(sessionKey(token))).toBe(false)
    expect(redis.sets.get(indexKey('citizen-id'))?.has(token)).toBeFalsy()
  })
})
