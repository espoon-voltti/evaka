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

describe('Citizen auth status', () => {
  let tester: GatewayTester

  beforeEach(async () => {
    tester = await GatewayTester.start(
      configFromEnv(),
      'citizen',
      new MockRedisClient()
    )
    tester.setCsrfHeader = true
  })
  afterEach(async () => {
    await tester?.afterEach()
    await tester?.stop()
  })

  async function weakLogin(): Promise<void> {
    tester.nockScope.post('/system/citizen-weak-login').reply(200, mockUser)
    const res = await tester.client.post(
      '/api/citizen/auth/weak-login',
      { username: 'user@example.com', password: 'password' },
      { validateStatus: () => true }
    )
    tester.nockScope.done()
    expect(res.status).toBe(200)
  }

  async function getStatus(): Promise<{ loggedIn: boolean }> {
    const res = await tester.client.get('/api/citizen/auth/status', {
      validateStatus: () => true
    })
    expect(res.status).toBe(200)
    return res.data as { loggedIn: boolean }
  }

  test('a session whose person no longer exists is ended and reported as logged out', async () => {
    await weakLogin()

    // The person was removed behind the session, e.g. by a database reset
    tester.nockScope.get(`/system/citizen/${mockUser.id}`).reply(404)
    expect((await getStatus()).loggedIn).toBe(false)
    tester.nockScope.done()

    // The session is gone, so the person is not looked up again
    expect((await getStatus()).loggedIn).toBe(false)
    tester.nockScope.done()
  })
})
