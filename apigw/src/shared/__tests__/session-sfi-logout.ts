// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { sign } from 'cookie-signature'
import type express from 'express'
import { describe, expect, test } from 'vitest'

import { createLogoutToken } from '../auth/index.ts'
import type { SamlSession } from '../saml/index.ts'
import { sessionSupport } from '../session.ts'
import { MockRedisClient } from '../test/mock-redis-client.ts'

const sessionConfig = {
  useSecureCookies: false,
  cookieSecret: 'test-secret',
  sessionTimeoutMinutes: 32
}

const samlSession: SamlSession = {
  issuer: 'idp',
  nameID: 'name-id',
  nameIDFormat: 'urn:oasis:names:tc:SAML:2.0:nameid-format:transient',
  sessionIndex: '_new-session-index'
}

const primaryReq = (id: string, createdAt: number) =>
  ({
    session: { id, evaka: { user: { authType: 'sfi', createdAt } } }
  }) as express.Request

const storeSfiSession = (redis: MockRedisClient, id: string) =>
  redis.set(
    `sess:${id}`,
    JSON.stringify({ passport: { user: { authType: 'sfi' } } })
  )

describe('saveRejectedSfiLogin', () => {
  test('links a session to a newer rejected login, returning only the samlSession', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    const req = primaryReq('p-ovr-1', 1000) // primary createdAt far in the past
    await storeSfiSession(redis, 'p-ovr-1')

    await sessions.saveRejectedSfiLogin(['p-ovr-1'], samlSession)

    const secondary = await sessions.getSecondaryUserIfNewer(req)
    expect(secondary).toBeDefined()
    // non-privileged: a bare samlSession, no user identity/roles
    expect((secondary as { id?: string }).id).toBeUndefined()
    expect((secondary as { userType?: string }).userType).toBeUndefined()
    expect((secondary as { sessionIndex?: string }).sessionIndex).toBe(
      '_new-session-index'
    )
  })

  test('a rejected login older-or-equal than the primary session is ignored (strict >)', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    // Write the rejected login record directly so createdAt is deterministic.
    await redis.set(
      'sfilogin:p-ovr-2',
      JSON.stringify({ samlSession, createdAt: 5000 })
    )
    // equal
    expect(
      await sessions.getSecondaryUserIfNewer(primaryReq('p-ovr-2', 5000))
    ).toBeUndefined()
    // older
    expect(
      await sessions.getSecondaryUserIfNewer(primaryReq('p-ovr-2', 6000))
    ).toBeUndefined()
  })

  test('no-op for empty ids and for unset maxSessionTimeoutMinutes', async () => {
    const redis = new MockRedisClient()
    const withTimeout = sessionSupport('citizen', redis, sessionConfig, 32)
    await withTimeout.saveRejectedSfiLogin([], samlSession)
    expect(
      await withTimeout.getSecondaryUserIfNewer(primaryReq('p-ovr-3', 1000))
    ).toBeUndefined()

    const noTimeout = sessionSupport('citizen', redis, sessionConfig)
    await noTimeout.saveRejectedSfiLogin(['p-ovr-4'], samlSession)
    expect(
      await noTimeout.getSecondaryUserIfNewer(primaryReq('p-ovr-4', 1000))
    ).toBeUndefined()
  })

  test('a rejected login expires after its TTL', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await storeSfiSession(redis, 'p-ovr-5')
    await sessions.saveRejectedSfiLogin(['p-ovr-5'], samlSession)
    expect(await redis.get('sfilogin:p-ovr-5')).not.toBeNull()

    redis.advanceTime(32 * 60 + 1)

    expect(
      await sessions.getSecondaryUserIfNewer(primaryReq('p-ovr-5', 1000))
    ).toBeUndefined()
  })

  test('prefers the rejected login when it is newer than the real secondary session', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await redis.set('sfisess:dual-1', 'secondary-1')
    await redis.set(
      'sess:secondary-1',
      JSON.stringify({
        evaka: {
          user: {
            authType: 'sfi',
            createdAt: 2000,
            sessionIndex: '_real-secondary'
          }
        }
      })
    )
    await redis.set(
      'sfilogin:dual-1',
      JSON.stringify({ samlSession, createdAt: 3000 })
    )

    const secondary = await sessions.getSecondaryUserIfNewer(
      primaryReq('dual-1', 1000)
    )
    expect((secondary as { sessionIndex?: string }).sessionIndex).toBe(
      '_new-session-index'
    )
  })

  test('prefers the real secondary session when it is newer than the rejected login', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await redis.set('sfisess:dual-2', 'secondary-2')
    await redis.set(
      'sess:secondary-2',
      JSON.stringify({
        evaka: {
          user: {
            authType: 'sfi',
            createdAt: 3000,
            sessionIndex: '_real-secondary'
          }
        }
      })
    )
    await redis.set(
      'sfilogin:dual-2',
      JSON.stringify({ samlSession, createdAt: 2000 })
    )

    const secondary = await sessions.getSecondaryUserIfNewer(
      primaryReq('dual-2', 1000)
    )
    expect((secondary as { sessionIndex?: string }).sessionIndex).toBe(
      '_real-secondary'
    )
  })

  test('ignores both candidates when the primary session is newer than both', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await redis.set('sfisess:tri-1', 'secondary-3')
    await redis.set(
      'sess:secondary-3',
      JSON.stringify({
        evaka: {
          user: { authType: 'sfi', createdAt: 2000, sessionIndex: '_sec' }
        }
      })
    )
    await redis.set(
      'sfilogin:tri-1',
      JSON.stringify({ samlSession, createdAt: 1500 })
    )
    // The newest candidate is the real secondary (2000), but the primary (3000)
    // is newer than both it and the rejected login (1500) -> nothing supersedes it.
    expect(
      await sessions.getSecondaryUserIfNewer(primaryReq('tri-1', 3000))
    ).toBeUndefined()
  })
})

describe('destroy clears the rejected sfi login', () => {
  test('deletes the rejected login key for the session being destroyed', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await storeSfiSession(redis, 'destroy-1')
    await sessions.saveRejectedSfiLogin(['destroy-1'], samlSession)
    expect(await redis.get('sfilogin:destroy-1')).not.toBeNull()

    const req = {
      session: { id: 'destroy-1', destroy: (cb: () => void) => cb() },
      user: { authType: 'sfi' }
    } as express.Request
    const res = {
      clearCookie: () => undefined
    } as unknown as express.Response

    await sessions.destroy(req, res)

    expect(await redis.get('sfilogin:destroy-1')).toBeNull()
  })
})

describe('logoutWithToken via a rejected sfi login', () => {
  const rejectedLoginToken = createLogoutToken(samlSession)

  const storeSession = async (
    redis: MockRedisClient,
    id: string,
    authType: string,
    logoutToken: string
  ) => {
    await redis.set(
      `sess:${id}`,
      JSON.stringify({
        passport: { user: { authType } },
        logoutToken: { value: logoutToken }
      })
    )
    await redis.set(`slo:${logoutToken}`, id)
  }

  test('tears down a linked sfi session and reports the logged out samlSession', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await storeSession(redis, 'ovr-c', 'sfi', 'stale-token')
    await sessions.saveRejectedSfiLogin(['ovr-c'], samlSession)

    const user = await sessions.logoutWithToken(rejectedLoginToken)

    expect(user).toEqual(samlSession)
    expect(await redis.get('sess:ovr-c')).toBeNull()
    // the session's own stale logout token must not outlive it
    expect(await redis.get('slo:stale-token')).toBeNull()
    expect(await redis.get('sfilogin:ovr-c')).toBeNull()
    expect(await redis.get(`sfislo:${rejectedLoginToken}`)).toBeNull()
  })

  test('also tears down the co-resident session cross-linked to it', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await storeSession(redis, 'ovr-c', 'sfi', 'citizen-token')
    await storeSession(redis, 'ovr-e', 'sfi', 'employee-token')
    await redis.set('sfisess:ovr-c', 'ovr-e')
    await redis.set('sfisess:ovr-e', 'ovr-c')
    await sessions.saveRejectedSfiLogin(['ovr-c'], samlSession)

    await sessions.logoutWithToken(rejectedLoginToken)

    expect(await redis.get('sess:ovr-e')).toBeNull()
    expect(await redis.get('slo:employee-token')).toBeNull()
    expect(await redis.get('sfisess:ovr-c')).toBeNull()
    expect(await redis.get('sfisess:ovr-e')).toBeNull()
  })

  test('reports nothing logged out when the linked session is already gone', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await storeSession(redis, 'ovr-gone', 'sfi', 'gone-token')
    await sessions.saveRejectedSfiLogin(['ovr-gone'], samlSession)
    // the session logs out normally before the IdP's logout request arrives
    await redis.del('sess:ovr-gone')

    expect(await sessions.logoutWithToken(rejectedLoginToken)).toBeUndefined()
    expect(await redis.get('sfilogin:ovr-gone')).toBeNull()
  })

  test('the sfislo index expires with the rejected login', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await storeSession(redis, 'ovr-ttl', 'sfi', 'ttl-token')
    await sessions.saveRejectedSfiLogin(['ovr-ttl'], samlSession)

    redis.advanceTime(32 * 60 + 1)

    expect(await sessions.logoutWithToken(rejectedLoginToken)).toBeUndefined()
    expect(await redis.get('sess:ovr-ttl')).not.toBeNull()
  })
})

describe('login correlation', () => {
  test('round-trips and is consumed exactly once', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)

    await sessions.saveLoginCorrelation('tok-1', {
      ownSid: 'own-1',
      secondarySid: 'sec-1'
    })

    expect(await sessions.consumeLoginCorrelation('tok-1')).toEqual({
      ownSid: 'own-1',
      secondarySid: 'sec-1'
    })
    // second read is empty (deleted on consume)
    expect(await sessions.consumeLoginCorrelation('tok-1')).toEqual({})
  })

  test('missing token yields an empty record', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    expect(await sessions.consumeLoginCorrelation('nope')).toEqual({})
  })

  test('expires after the correlation TTL', async () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    await sessions.saveLoginCorrelation('tok-2', { ownSid: 'own-2' })

    redis.advanceTime(15 * 60 + 1)

    expect(await sessions.consumeLoginCorrelation('tok-2')).toEqual({})
  })
})

describe('sessionIdFromCookie', () => {
  test('returns the id for a validly-signed own cookie', () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    const cookie = 's:' + sign('sid-123', sessionConfig.cookieSecret)
    expect(sessions.sessionIdFromCookie(cookie)).toBe('sid-123')
  })

  test('returns undefined for a tampered or missing cookie', () => {
    const redis = new MockRedisClient()
    const sessions = sessionSupport('citizen', redis, sessionConfig, 32)
    const tampered = 's:sid-123.deadbeefsignature'
    expect(sessions.sessionIdFromCookie(tampered)).toBeUndefined()
    expect(sessions.sessionIdFromCookie(undefined)).toBeUndefined()
  })
})
