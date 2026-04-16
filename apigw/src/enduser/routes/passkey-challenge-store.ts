// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import crypto from 'node:crypto'

import type { RedisClient } from '../../shared/redis-client.ts'

const TTL_SECONDS = 300
const PREFIX = 'passkey-challenge:'

export interface PasskeyChallengeEntry {
  challenge: string
  flow: 'register' | 'login'
  personId?: string
}

export async function putChallenge(
  redis: RedisClient,
  token: string,
  entry: PasskeyChallengeEntry
): Promise<void> {
  await redis.set(`${PREFIX}${token}`, JSON.stringify(entry), {
    EX: TTL_SECONDS
  })
}

export async function takeChallenge(
  redis: RedisClient,
  token: string
): Promise<PasskeyChallengeEntry | null> {
  const key = `${PREFIX}${token}`
  const raw = await redis.get(key)
  if (raw === null) return null
  await redis.del(key)
  try {
    return JSON.parse(raw) as PasskeyChallengeEntry
  } catch {
    return null
  }
}

export function generateToken(): string {
  return crypto.randomBytes(16).toString('hex')
}
