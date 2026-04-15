// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import * as simplewebauthn from '@simplewebauthn/server'
import express from 'express'
import request from 'supertest'
import { beforeEach, expect, it, vi } from 'vitest'

import type { Config } from '../../shared/config.ts'
import type { RedisClient } from '../../shared/redis-client.ts'
import * as service from '../../shared/service-client-passkey.ts'
import type { Sessions } from '../../shared/session.ts'

import { passkeyAuthRoutes } from './passkey-auth.ts'

// ---------------------------------------------------------------------------
// Mock @simplewebauthn/server
// ---------------------------------------------------------------------------
vi.mock('@simplewebauthn/server', () => ({
  generateRegistrationOptions: vi.fn(),
  verifyRegistrationResponse: vi.fn(),
  generateAuthenticationOptions: vi.fn(),
  verifyAuthenticationResponse: vi.fn()
}))

// ---------------------------------------------------------------------------
// Mock the service client
// ---------------------------------------------------------------------------
vi.mock('../../shared/service-client-passkey.ts', () => ({
  listCitizenPasskeyCredentials: vi.fn(),
  upsertCitizenPasskeyCredential: vi.fn(),
  touchCitizenPasskeyCredential: vi.fn()
}))

vi.mock('../../shared/service-client.ts', () => ({
  getCitizenDetails: vi.fn(() =>
    Promise.resolve({
      details: {
        firstName: 'Test',
        lastName: 'Citizen',
        preferredName: ''
      }
    })
  )
}))

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function fakeRedis(): RedisClient {
  const store = new Map<string, string>()
  return {
    isReady: true,
    get: vi.fn((k: string) => Promise.resolve(store.get(k) ?? null)),
    set: vi.fn((k: string, v: string, _opts: { EX: number }) => {
      store.set(k, v)
      return Promise.resolve(null)
    }),
    del: vi.fn((k: string | string[]) => {
      const keys = Array.isArray(k) ? k : [k]
      for (const key of keys) store.delete(key)
      return Promise.resolve(keys.length)
    }),
    expire: vi.fn(() => Promise.resolve(1)),
    incr: vi.fn(() => Promise.resolve(0)),
    ping: vi.fn(() => Promise.resolve('PONG')),
    multi: vi.fn(() => ({
      incr: vi.fn().mockReturnThis(),
      sAdd: vi.fn().mockReturnThis(),
      expire: vi.fn().mockReturnThis(),
      exec: vi.fn(() => Promise.resolve([]))
    })),
    sAdd: vi.fn(() => Promise.resolve(1)),
    sMembers: vi.fn(() => Promise.resolve([])),
    sRem: vi.fn(() => Promise.resolve(1))
  } as unknown as RedisClient
}

const config = {
  passkey: {
    rpId: 'localhost',
    origin: 'http://localhost:9099',
    rpName: 'eVaka'
  }
} as unknown as Config

const sessionsMock = {
  login: vi.fn(() => Promise.resolve()),
  destroy: vi.fn(() => Promise.resolve()),
  save: vi.fn(() => Promise.resolve()),
  saveLogoutToken: vi.fn(() => Promise.resolve()),
  logoutWithToken: vi.fn(() => Promise.resolve()),
  updateUser: vi.fn(() => Promise.resolve()),
  getUser: vi.fn(() => undefined),
  getSecondaryUserIfNewer: vi.fn(() => Promise.resolve(undefined)),
  getUserHeader: vi.fn(() => undefined),
  isAuthenticated: vi.fn(() => false),
  sessionType: 'citizen' as const,
  cookieName: 'evaka.eugw.session',
  middleware: vi.fn((_req: unknown, _res: unknown, next: () => void) => next()),
  requireAuthentication: vi.fn(
    (_req: unknown, _res: unknown, next: () => void) => next()
  )
} as unknown as Sessions<'citizen'>

function buildApp(
  redis: ReturnType<typeof fakeRedis>,
  user: express.Request['user']
) {
  const app = express()
  app.use(express.json())
  // Inject a fake user to simulate a logged-in citizen session
  app.use((req, _res, next) => {
    req.user = user
    next()
  })
  app.use(
    '/citizen/auth/passkey',
    passkeyAuthRoutes(config, sessionsMock, redis)
  )
  return app
}

// ---------------------------------------------------------------------------
// Reset mocks between tests
// ---------------------------------------------------------------------------
/* eslint-disable @typescript-eslint/unbound-method */
beforeEach(() => {
  vi.mocked(sessionsMock.login).mockReset()
  vi.mocked(service.upsertCitizenPasskeyCredential).mockReset()
  vi.mocked(service.touchCitizenPasskeyCredential).mockReset()
  vi.mocked(service.listCitizenPasskeyCredentials).mockReset()
})
/* eslint-enable @typescript-eslint/unbound-method */

// ---------------------------------------------------------------------------
// Test 1: register options → register verify persists the credential
// ---------------------------------------------------------------------------
it('register options → verify upserts the credential', async () => {
  vi.mocked(simplewebauthn.generateRegistrationOptions).mockResolvedValue({
    challenge: 'reg-challenge'
  } as Awaited<ReturnType<typeof simplewebauthn.generateRegistrationOptions>>)

  vi.mocked(simplewebauthn.verifyRegistrationResponse).mockResolvedValue({
    verified: true,
    registrationInfo: {
      credential: {
        id: 'new-cred-id',
        publicKey: new Uint8Array([1, 2, 3]),
        counter: 0
      }
    }
  } as unknown as Awaited<
    ReturnType<typeof simplewebauthn.verifyRegistrationResponse>
  >)

  vi.mocked(service.listCitizenPasskeyCredentials).mockResolvedValue([])
  vi.mocked(service.upsertCitizenPasskeyCredential).mockResolvedValue(undefined)

  const redis = fakeRedis()
  const strongUser = {
    id: '11111111-2222-3333-4444-555555555555',
    userType: 'CITIZEN_STRONG' as const,
    authType: 'dev' as const
  }
  const app = buildApp(redis, strongUser)

  const optsRes = await request(app)
    .post('/citizen/auth/passkey/register/options')
    .send({})
  expect(optsRes.status).toBe(200)
  const { token } = optsRes.body as { token: string }
  expect(typeof token).toBe('string')

  const verifyRes = await request(app)
    .post('/citizen/auth/passkey/register/verify')
    .send({
      token,
      attestation: { response: { transports: ['internal'] } }
    })
  expect(verifyRes.status).toBe(200)
  expect(service.upsertCitizenPasskeyCredential).toHaveBeenCalledTimes(1)
})

// ---------------------------------------------------------------------------
// Test 2: login options → login verify creates a passkey session
// ---------------------------------------------------------------------------
it('login options → verify calls sessions.login with passkey variant', async () => {
  const storedPublicKey = Buffer.from([4, 5, 6]).toString('base64url')

  vi.mocked(simplewebauthn.generateAuthenticationOptions).mockResolvedValue({
    challenge: 'login-challenge'
  } as Awaited<ReturnType<typeof simplewebauthn.generateAuthenticationOptions>>)

  vi.mocked(simplewebauthn.verifyAuthenticationResponse).mockResolvedValue({
    verified: true,
    authenticationInfo: { newCounter: 5 }
  } as unknown as Awaited<
    ReturnType<typeof simplewebauthn.verifyAuthenticationResponse>
  >)

  vi.mocked(service.listCitizenPasskeyCredentials).mockResolvedValue([
    {
      credentialId: 'existing-cred-id',
      publicKey: storedPublicKey,
      signCounter: 0,
      transports: ['internal'],
      label: 'iPhone',
      deviceHint: null,
      createdAt: '2026-04-15T12:00:00Z',
      lastUsedAt: null
    }
  ])
  vi.mocked(service.touchCitizenPasskeyCredential).mockResolvedValue(undefined)
  // eslint-disable-next-line @typescript-eslint/unbound-method
  vi.mocked(sessionsMock.login).mockResolvedValue(undefined)

  const redis = fakeRedis()
  const app = buildApp(redis, undefined) // no prior session

  const optsRes = await request(app)
    .post('/citizen/auth/passkey/login/options')
    .send({})
  expect(optsRes.status).toBe(200)
  const { token } = optsRes.body as { token: string }
  expect(typeof token).toBe('string')

  // userHandle = 16-byte UUID 11111111-2222-3333-4444-555555555555 as base64url
  const userHandle = Buffer.from(
    '11111111222233334444555555555555',
    'hex'
  ).toString('base64url')

  const verifyRes = await request(app)
    .post('/citizen/auth/passkey/login/verify')
    .send({
      token,
      assertion: {
        id: 'existing-cred-id',
        response: { userHandle }
      }
    })
  expect(verifyRes.status).toBe(200)
  // eslint-disable-next-line @typescript-eslint/unbound-method
  expect(sessionsMock.login).toHaveBeenCalledTimes(1)
  // eslint-disable-next-line @typescript-eslint/unbound-method
  const loggedInUser = vi.mocked(sessionsMock.login).mock.calls[0][1]
  expect(loggedInUser).toMatchObject({
    authType: 'citizen-passkey',
    userType: 'CITIZEN_WEAK',
    id: '11111111-2222-3333-4444-555555555555',
    credentialId: 'existing-cred-id'
  })
})
