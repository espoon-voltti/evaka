// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { randomBytes } from 'node:crypto'

import { createSha256Hash } from './crypto.ts'
import type { RedisClient } from './redis-client.ts'

export interface MobileSession {
  userId: string
  authLevel: 'WEAK'
  createdAt: number
  expiresAt: number
}

const TTL_SECONDS = 30 * 24 * 60 * 60 // 30 days

const sessionKey = (token: string) => `mobile-session:${token}`
const userIndexKey = (userId: string) => `usess:${createSha256Hash(userId)}`

function newToken(): string {
  return randomBytes(32).toString('base64url')
}

export async function createMobileSession(
  redis: RedisClient,
  userId: string
): Promise<{ token: string; expiresAt: number }> {
  const token = newToken()
  const now = Date.now()
  const session: MobileSession = {
    userId,
    authLevel: 'WEAK',
    createdAt: now,
    expiresAt: now + TTL_SECONDS * 1000
  }
  await redis.set(sessionKey(token), JSON.stringify(session), { EX: TTL_SECONDS })
  await redis.sAdd(userIndexKey(userId), token)
  await redis.expire(userIndexKey(userId), TTL_SECONDS)
  return { token, expiresAt: session.expiresAt }
}

export async function getMobileSession(
  redis: RedisClient,
  token: string
): Promise<MobileSession | null> {
  const raw = await redis.get(sessionKey(token))
  if (!raw) return null
  await redis.expire(sessionKey(token), TTL_SECONDS)
  return JSON.parse(raw) as MobileSession
}

export async function deleteMobileSession(
  redis: RedisClient,
  token: string
): Promise<void> {
  const raw = await redis.get(sessionKey(token))
  if (raw) {
    const parsed = JSON.parse(raw) as MobileSession
    await redis.sRem(userIndexKey(parsed.userId), token)
  }
  await redis.del(sessionKey(token))
}
