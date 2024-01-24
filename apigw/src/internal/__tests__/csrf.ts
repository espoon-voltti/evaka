// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { GatewayTester } from '../../shared/test/gateway-tester.js'
import { EmployeeUser } from '../../shared/service-client.js'
import { configFromEnv } from '../../shared/config.js'
import { AuthStatus } from '../routes/auth-status.js'

const mockUser: EmployeeUser = {
  id: '8fc11215-6d55-4059-bd59-038bfa36f294',
  firstName: '',
  lastName: '',
  globalRoles: ['SERVICE_WORKER'],
  allScopedRoles: []
}

describe('CSRF middleware and cookie handling in internal-gw', () => {
  let tester: GatewayTester
  beforeAll(async () => {
    tester = await GatewayTester.start(configFromEnv(), 'employee')
  })
  beforeEach(async () => tester.login(mockUser))
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  async function setupAntiCsrfToken() {
    tester.nockScope.get(`/system/employee/${mockUser.id}`).reply(200, mockUser)
    const response = await tester.client.get('/api/internal/auth/status')
    const authStatus = response.data as AuthStatus
    tester.nockScope.done()

    expect(authStatus.antiCsrfToken).toBeTruthy()
    tester.antiCsrfToken = authStatus.antiCsrfToken
  }

  it('should fail POST to a proxied API when there is no CSRF token', async () => {
    const res = await tester.client.post(
      '/api/internal/some-proxied-api',
      undefined,
      {
        validateStatus: () => true
      }
    )
    expect(res.status).toBe(403)
  })
  it('should pass GET to a proxied API when there is no CSRF token', async () => {
    tester.nockScope.get('/some-proxied-api').reply(200)
    const res = await tester.client.get('/api/internal/some-proxied-api')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should pass POST to a proxied API after CSRF token has been set up by the auth/status endpoint', async () => {
    await setupAntiCsrfToken()

    tester.nockScope.post('/some-proxied-api').reply(200)
    const res = await tester.client.post('/api/internal/some-proxied-api')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should not check CSRF if a session is not available', async () => {
    await setupAntiCsrfToken()
    await tester.expireSession()

    const res = await tester.client.post(
      '/api/internal/some-proxied-api',
      undefined,
      {
        validateStatus: () => true
      }
    )
    expect(res.status).toBe(401)
  })
})
