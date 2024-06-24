// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  describe,
  beforeAll,
  afterEach,
  afterAll,
  beforeEach,
  expect,
  it
} from '@jest/globals'

import { configFromEnv } from '../../shared/config.js'
import { CitizenUser } from '../../shared/service-client.js'
import { GatewayTester } from '../../shared/test/gateway-tester.js'
import { AuthStatus } from '../routes/auth-status.js'

const mockUser: CitizenUser = {
  id: '4f73e4f8-8759-46c6-9b9d-4da860138ce2'
}

describe('CSRF middleware and cookie handling in enduser-gw', () => {
  let tester: GatewayTester
  beforeAll(async () => {
    tester = await GatewayTester.start(configFromEnv(), 'enduser')
  })
  beforeEach(async () => tester.login(mockUser))
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  async function setupAntiCsrfToken() {
    tester.nockScope.get(`/system/citizen/${mockUser.id}`).reply(200, mockUser)
    const response = await tester.client.get('/api/application/auth/status')
    const authStatus = response.data as AuthStatus
    tester.nockScope.done()

    expect(authStatus.antiCsrfToken).toBeTruthy()
    tester.antiCsrfToken = authStatus.antiCsrfToken
  }

  it('should fail a POST to a proxied API when there is no CSRF token', async () => {
    const res = await tester.client.post(
      '/api/application/citizen/applications',
      undefined,
      {
        validateStatus: () => true
      }
    )
    expect(res.status).toBe(403)
  })
  it('should pass GET to a proxied API when there is no CSRF token', async () => {
    tester.nockScope.get('/citizen/applications').reply(200)
    const res = await tester.client.get('/api/application/citizen/applications')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should pass a POST to a proxied API after CSRF token has been set up by the auth/status endpoint', async () => {
    await setupAntiCsrfToken()

    tester.nockScope.post('/citizen/applications').reply(200)
    const res = await tester.client.post(
      '/api/application/citizen/applications'
    )
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should not check CSRF if a session is not available', async () => {
    await setupAntiCsrfToken()
    await tester.expireSession()

    const res = await tester.client.post(
      '/api/application/citizen/applications',
      undefined,
      {
        validateStatus: () => true
      }
    )
    expect(res.status).toBe(401)
  })
})
