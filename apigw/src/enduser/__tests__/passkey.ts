// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { AxiosResponse } from 'axios'
import { afterEach, beforeEach, describe, expect, test } from 'vitest'

import { PASSKEY_CHALLENGE_TTL_SECONDS } from '../../enduser/routes/auth-passkey-login.ts'
import { configFromEnv } from '../../shared/config.ts'
import type { CitizenUser } from '../../shared/service-client.ts'
import { GatewayTester } from '../../shared/test/gateway-tester.ts'
import { MockRedisClient } from '../../shared/test/mock-redis-client.ts'

const mockUser: CitizenUser = {
  id: '4f73e4f8-8759-46c6-9b9d-4da860138ce2'
}

const assertionRequest = '{"challenge":"stored-assertion-request"}'
const credential = '{"id":"credential-from-browser"}'

describe('Passkey login', () => {
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

  async function fetchLoginOptions(
    onTester: GatewayTester = tester
  ): Promise<{ challengeKey: string; options: unknown }> {
    onTester.nockScope.post('/system/passkey-login/options').reply(200, {
      assertionRequest,
      credentialsGet: '{"publicKey":{"challenge":"abc"}}'
    })
    const res = await onTester.client.post(
      '/api/citizen/auth/passkey-login/options',
      undefined,
      { validateStatus: () => true }
    )
    onTester.nockScope.done()
    expect(res.status).toBe(200)
    // oxlint-disable-next-line typescript/no-unsafe-return
    return res.data
  }

  async function finishLogin(
    challengeKey: string,
    onTester: GatewayTester = tester
  ): Promise<AxiosResponse> {
    return await onTester.client.post(
      '/api/citizen/auth/passkey-login/finish',
      { challengeKey, credential },
      { validateStatus: () => true }
    )
  }

  test('login stashes the assertion request and passes it to the finish leg', async () => {
    const { challengeKey, options } = await fetchLoginOptions()
    expect(options).toEqual({ publicKey: { challenge: 'abc' } })

    let capturedBody: Record<string, unknown> | undefined
    tester.nockScope
      .post('/system/passkey-login/finish', (body: Record<string, unknown>) => {
        capturedBody = body
        return true
      })
      .reply(200, mockUser)
    const res = await finishLogin(challengeKey)
    tester.nockScope.done()
    expect(res.status).toBe(200)
    expect(capturedBody).toEqual({
      assertionRequest,
      credential,
      deviceAuthHistory: []
    })
  })

  test('a challenge is single-use', async () => {
    const { challengeKey } = await fetchLoginOptions()

    tester.nockScope.post('/system/passkey-login/finish').reply(200, mockUser)
    expect((await finishLogin(challengeKey)).status).toBe(200)
    tester.nockScope.done()

    // no service call: nock would throw on an unexpected request
    expect((await finishLogin(challengeKey)).status).toBe(403)
  })

  test('a challenge expires after its TTL', async () => {
    const { challengeKey } = await fetchLoginOptions()
    redisClient.advanceTime(PASSKEY_CHALLENGE_TTL_SECONDS + 1)
    expect((await finishLogin(challengeKey)).status).toBe(403)
  })

  test('an unknown challenge key is rejected', async () => {
    expect((await finishLogin('non-existing-key')).status).toBe(403)
  })

  test('deleting a passkey logs out other weak sessions', async () => {
    // two sessions for the same user, sharing the same Redis
    const otherTester = await GatewayTester.start(
      configFromEnv(),
      'citizen',
      redisClient
    )
    otherTester.setCsrfHeader = true
    try {
      const first = await fetchLoginOptions()
      tester.nockScope.post('/system/passkey-login/finish').reply(200, mockUser)
      expect((await finishLogin(first.challengeKey)).status).toBe(200)
      tester.nockScope.done()

      const second = await fetchLoginOptions(otherTester)
      otherTester.nockScope
        .post('/system/passkey-login/finish')
        .reply(200, mockUser)
      expect((await finishLogin(second.challengeKey, otherTester)).status).toBe(
        200
      )
      otherTester.nockScope.done()

      tester.nockScope
        .delete('/citizen/passkeys/e69eba47-2637-4b62-b3fc-f0c5b0b0ce05')
        .reply(200)
      const res = await tester.client.delete(
        '/api/citizen/passkeys/e69eba47-2637-4b62-b3fc-f0c5b0b0ce05',
        { validateStatus: () => true }
      )
      tester.nockScope.done()
      expect(res.status).toBe(204)

      const status = await otherTester.client.get('/api/citizen/auth/status', {
        validateStatus: () => true
      })
      expect(status.status).toBe(200)
      expect((status.data as { loggedIn: boolean }).loggedIn).toBe(false)
    } finally {
      await otherTester.afterEach()
      await otherTester.stop()
    }
  })
})
