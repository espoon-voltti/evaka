// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import tracer from '../../tracer.js'
import jwt from 'jsonwebtoken'
import { readFileSync } from 'node:fs'
import { jwtKid, jwtPrivateKey } from '../config.js'

const privateKey = readFileSync(jwtPrivateKey)

interface JwtPayload {
  sub: string
  scope?: string
  evaka_employee_id?: string
  evaka_type:
    | 'citizen'
    | 'citizen_weak'
    | 'employee'
    | 'mobile'
    | 'system'
    | 'integration'
}

export function createJwt(payload: JwtPayload): string {
  return tracer.trace('createJwt', (span) => {
    const now = Date.now()
    const key = cacheKey(payload)
    const cached = cache.get(key)
    if (cached && now - cached.createdAt <= JWT_LIFETIME) {
      span?.addTags({ cacheHit: true, cacheSize: cache.size })
      return cached.token
    } else {
      const token = jwt.sign(payload, privateKey, {
        algorithm: 'RS256',
        expiresIn: '48h',
        keyid: jwtKid
      })
      cache.set(key, { token, createdAt: now })
      span?.addTags({ cacheHit: false, cacheSize: cache.size })

      if (
        cache.size > MAX_CACHE_SIZE ||
        now - lastCacheCleanup > CACHE_CLEANUP_INTERVAL
      ) {
        for (const [key, { createdAt }] of cache.entries()) {
          if (now - createdAt > JWT_LIFETIME) {
            cache.delete(key)
          }
        }
        lastCacheCleanup = now
        span?.addTags({ cacheCleanup: true, cacheSizeAfterCleanup: cache.size })
      } else {
        span?.addTags({ cacheCleanup: false })
      }

      return token
    }
  })
}

const MAX_CACHE_SIZE = 10000 // Each entry takes ~1 KB of memory, so 10k entries is ~10 MB
const JWT_LIFETIME = 10 * 60 * 1000 // 10 minutes
const CACHE_CLEANUP_INTERVAL = 60 * 1000 // 1 minute

interface CacheEntry {
  token: string
  createdAt: number
}

const cache = new Map<string, CacheEntry>()
let lastCacheCleanup = Date.now()

function cacheKey(payload: JwtPayload): string {
  return (
    payload.evaka_type +
    '|' +
    payload.sub +
    '|' +
    (payload.evaka_employee_id ?? '') +
    '|' +
    (payload.scope ?? '')
  )
}
