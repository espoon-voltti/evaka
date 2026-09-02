// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { afterEach, beforeEach, describe, expect, test } from 'vitest'

import { configFromEnv } from '../../shared/config.ts'
import type { CitizenUser } from '../../shared/service-client.ts'
import { GatewayTester } from '../../shared/test/gateway-tester.ts'
import { MockRedisClient } from '../../shared/test/mock-redis-client.ts'

const mockUser: CitizenUser = {
  id: '4f73e4f8-8759-46c6-9b9d-4da860138ce2'
}

describe('Weak login credentials', () => {
  let redisClient: MockRedisClient
  let tester: GatewayTester

  beforeEach(async () => {
    redisClient = new MockRedisClient()
    tester = await GatewayTester.start(configFromEnv(), 'citizen', redisClient)
    tester.setCsrfHeader = true
  })
  afterEach(async () => {
    await tester?.afterEach()
    await tester?.stop()
  })

  async function weakLogin(onTester: GatewayTester): Promise<void> {
    onTester.nockScope.post('/system/citizen-weak-login').reply(200, mockUser)
    const res = await onTester.client.post(
      '/api/citizen/auth/weak-login',
      { username: 'user@example.com', password: 'password' },
      { validateStatus: () => true }
    )
    onTester.nockScope.done()
    expect(res.status).toBe(200)
  }

  // runs the body with a second weak session for the same user, sharing the same Redis
  async function withSecondWeakSession(
    f: (otherTester: GatewayTester) => Promise<void>
  ): Promise<void> {
    const otherTester = await GatewayTester.start(
      configFromEnv(),
      'citizen',
      redisClient
    )
    otherTester.setCsrfHeader = true
    try {
      await weakLogin(tester)
      await weakLogin(otherTester)
      await f(otherTester)
    } finally {
      await otherTester.afterEach()
      await otherTester.stop()
    }
  }

  async function assertLoggedOut(onTester: GatewayTester): Promise<void> {
    const status = await onTester.client.get('/api/citizen/auth/status', {
      validateStatus: () => true
    })
    expect(status.status).toBe(200)
    expect((status.data as { loggedIn: boolean }).loggedIn).toBe(false)
  }

  test('deleting credentials requires a session', async () => {
    const res = await tester.client.delete(
      '/api/citizen/personal-data/weak-login-credentials',
      { validateStatus: () => true }
    )
    expect(res.status).toBe(401)
  })

  test('deleting credentials logs out other weak sessions', async () => {
    await withSecondWeakSession(async (otherTester) => {
      tester.nockScope
        .delete('/citizen/personal-data/weak-login-credentials')
        .reply(200)
      const res = await tester.client.delete(
        '/api/citizen/personal-data/weak-login-credentials',
        { validateStatus: () => true }
      )
      tester.nockScope.done()
      expect(res.status).toBe(204)

      await assertLoggedOut(otherTester)
    })
  })

  test('updating credentials requires a session', async () => {
    const res = await tester.client.put(
      '/api/citizen/personal-data/weak-login-credentials',
      { password: 'aifiefaeC3io?dee' },
      { validateStatus: () => true }
    )
    expect(res.status).toBe(401)
  })

  test('updating credentials logs out other weak sessions', async () => {
    await withSecondWeakSession(async (otherTester) => {
      tester.nockScope
        .put('/citizen/personal-data/weak-login-credentials')
        .reply(200)
      const res = await tester.client.put(
        '/api/citizen/personal-data/weak-login-credentials',
        { password: 'aifiefaeC3io?dee' },
        { validateStatus: () => true }
      )
      tester.nockScope.done()
      expect(res.status).toBe(204)

      await assertLoggedOut(otherTester)
    })
  })
})
