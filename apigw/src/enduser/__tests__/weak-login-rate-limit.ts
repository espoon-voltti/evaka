// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

import { configFromEnv } from '../../shared/config.ts'
import { GatewayTester } from '../../shared/test/gateway-tester.ts'

describe('Weak login rate limit', () => {
  let tester: GatewayTester

  beforeEach(async () => {
    const config = configFromEnv()
    tester = await GatewayTester.start(
      { ...config, citizen: { ...config.citizen, weakLoginRateLimit: 2 } },
      'citizen'
    )
    tester.setCsrfHeader = true
  })
  afterEach(async () => {
    vi.useRealTimers()
    await tester?.afterEach()
    await tester?.stop()
  })

  async function attempt(username: string): Promise<number> {
    const res = await tester.client.post(
      '/api/citizen/auth/weak-login',
      { username, password: 'wrong' },
      { validateStatus: () => true }
    )
    return res.status
  }

  test('rejects attempts over the limit without calling the service', async () => {
    tester.nockScope.post('/system/citizen-weak-login').times(2).reply(403)

    expect(await attempt('user@example.com')).toBe(403)
    expect(await attempt('user@example.com')).toBe(403)
    expect(await attempt('user@example.com')).toBe(429)
    tester.nockScope.done()
  })

  test('counts each username separately', async () => {
    tester.nockScope.post('/system/citizen-weak-login').times(3).reply(403)

    expect(await attempt('user@example.com')).toBe(403)
    expect(await attempt('user@example.com')).toBe(403)
    expect(await attempt('other@example.com')).toBe(403)
    tester.nockScope.done()
  })

  test('limit does not reset at the turn of the clock hour', async () => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-10-05T10:59:00'))
    tester.nockScope.post('/system/citizen-weak-login').times(2).reply(403)

    expect(await attempt('user@example.com')).toBe(403)
    expect(await attempt('user@example.com')).toBe(403)
    vi.setSystemTime(new Date('2026-10-05T11:00:00'))
    expect(await attempt('user@example.com')).toBe(429)
    tester.nockScope.done()
  })
})
