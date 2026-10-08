// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

import { configFromEnv } from '../../shared/config.ts'
import { logWarn } from '../../shared/logging.ts'
import { GatewayTester } from '../../shared/test/gateway-tester.ts'

vi.mock('../../shared/logging.ts', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../../shared/logging.ts')>()),
  logWarn: vi.fn()
}))

// X-Forwarded-For as it arrives at apigw: client, public ALB, nginx
const forwardedFor = (clientIp: string) => `${clientIp}, 10.0.1.1, 10.0.2.2`

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
    vi.mocked(logWarn).mockClear()
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
    expect(logWarn).toHaveBeenCalledWith(
      'Login request hit rate limit',
      expect.anything(),
      expect.objectContaining({ eventCode: 'evaka.citizen_weak.rate_limited' })
    )
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

describe('Weak login IP rate limit', () => {
  let tester: GatewayTester

  async function start(enforce: boolean) {
    const config = configFromEnv()
    tester = await GatewayTester.start(
      {
        ...config,
        citizen: {
          ...config.citizen,
          weakLoginRateLimit: 2,
          weakLoginIpRateLimit: 2,
          weakLoginIpRateLimitEnforce: enforce
        }
      },
      'citizen'
    )
    tester.setCsrfHeader = true
  }
  afterEach(async () => {
    vi.mocked(logWarn).mockClear()
    await tester?.afterEach()
    await tester?.stop()
  })

  async function attempt(
    clientIp: string,
    username: string,
    realIp: string = clientIp
  ): Promise<number> {
    const res = await tester.client.post(
      '/api/citizen/auth/weak-login',
      { username, password: 'wrong' },
      {
        headers: {
          'x-forwarded-for': forwardedFor(clientIp),
          'x-real-ip': realIp
        },
        validateStatus: () => true
      }
    )
    return res.status
  }

  function clientIpMismatchWarnings() {
    return vi
      .mocked(logWarn)
      .mock.calls.filter(
        ([, , meta]) =>
          meta?.eventCode === 'evaka.citizen_weak.client_ip_mismatch'
      )
  }

  function ipLimitWarnings() {
    return vi
      .mocked(logWarn)
      .mock.calls.filter(
        ([, , meta]) =>
          meta?.eventCode === 'evaka.citizen_weak.ip_rate_limit_exceeded'
      )
  }

  test('rejects attempts over the limit from one IP across different usernames without calling the service', async () => {
    await start(true)
    tester.nockScope.post('/system/citizen-weak-login').times(2).reply(403)

    expect(await attempt('203.0.113.1', 'a@example.com')).toBe(403)
    expect(await attempt('203.0.113.1', 'b@example.com')).toBe(403)
    expect(await attempt('203.0.113.1', 'c@example.com')).toBe(429)
    tester.nockScope.done()
  })

  test('counts each IP separately', async () => {
    await start(true)
    tester.nockScope.post('/system/citizen-weak-login').times(3).reply(403)

    expect(await attempt('203.0.113.1', 'a@example.com')).toBe(403)
    expect(await attempt('203.0.113.1', 'b@example.com')).toBe(403)
    expect(await attempt('203.0.113.2', 'c@example.com')).toBe(403)
    tester.nockScope.done()
  })

  test('attempts rejected by the IP limit do not count against the username', async () => {
    await start(true)
    tester.nockScope.post('/system/citizen-weak-login').times(3).reply(403)

    expect(await attempt('203.0.113.1', 'victim@example.com')).toBe(403)
    expect(await attempt('203.0.113.1', 'other@example.com')).toBe(403)
    expect(await attempt('203.0.113.1', 'victim@example.com')).toBe(429)
    // victim's username count is still 1, so one more attempt is allowed
    expect(await attempt('203.0.113.2', 'victim@example.com')).toBe(403)
    tester.nockScope.done()
  })

  test('logs once per window when an enforced limit is exceeded', async () => {
    await start(true)
    tester.nockScope.post('/system/citizen-weak-login').times(2).reply(403)

    for (const username of ['a', 'b', 'c', 'd']) {
      await attempt('203.0.113.1', `${username}@example.com`)
    }

    expect(ipLimitWarnings()).toEqual([
      [
        'Login request exceeded IP rate limit',
        expect.anything(),
        {
          eventCode: 'evaka.citizen_weak.ip_rate_limit_exceeded',
          ipKey: '203.0.113.1',
          xRealIpMatches: true,
          enforced: true
        }
      ]
    ])
  })

  test('only logs when the limit is not enforced', async () => {
    await start(false)
    tester.nockScope.post('/system/citizen-weak-login').times(4).reply(403)

    for (const username of ['a', 'b', 'c', 'd']) {
      expect(await attempt('203.0.113.1', `${username}@example.com`)).toBe(403)
    }
    tester.nockScope.done()

    expect(ipLimitWarnings()).toEqual([
      [
        'Login request exceeded IP rate limit',
        expect.anything(),
        {
          eventCode: 'evaka.citizen_weak.ip_rate_limit_exceeded',
          ipKey: '203.0.113.1',
          xRealIpMatches: true,
          enforced: false
        }
      ]
    ])
  })

  test('logs whether X-Real-IP agrees with the client IP', async () => {
    await start(false)
    tester.nockScope.post('/system/citizen-weak-login').times(3).reply(403)

    for (const username of ['a', 'b', 'c']) {
      await attempt('203.0.113.1', `${username}@example.com`, '10.0.2.2')
    }

    expect(ipLimitWarnings()[0][2]).toMatchObject({ xRealIpMatches: false })
  })

  test('logs once per window when X-Real-IP disagrees with the client IP', async () => {
    await start(false)
    tester.nockScope.post('/system/citizen-weak-login').times(2).reply(403)

    await attempt('203.0.113.1', 'a@example.com', '10.0.2.2')
    await attempt('203.0.113.1', 'b@example.com', '10.0.2.2')

    expect(clientIpMismatchWarnings()).toEqual([
      [
        'Client IP differs from X-Real-IP',
        expect.anything(),
        {
          eventCode: 'evaka.citizen_weak.client_ip_mismatch',
          ipKey: '203.0.113.1'
        }
      ]
    ])
  })

  test('does not log a mismatch when X-Real-IP agrees with the client IP', async () => {
    await start(false)
    tester.nockScope.post('/system/citizen-weak-login').reply(403)

    await attempt('203.0.113.1', 'a@example.com')

    expect(clientIpMismatchWarnings()).toEqual([])
  })

  test('logs a mismatch when the client IP is not a valid address', async () => {
    await start(true)
    tester.nockScope.post('/system/citizen-weak-login').reply(403)

    // e.g. a load balancer appending the client port to X-Forwarded-For
    expect(
      await attempt('203.0.113.1:54321', 'a@example.com', '203.0.113.1')
    ).toBe(403)

    expect(clientIpMismatchWarnings()).toEqual([
      [
        'Client IP differs from X-Real-IP',
        expect.anything(),
        {
          eventCode: 'evaka.citizen_weak.client_ip_mismatch',
          ipKey: undefined
        }
      ]
    ])
  })
})
