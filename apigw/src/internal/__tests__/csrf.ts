// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { GatewayTester } from '../../shared/test/gateway-tester'
import app from '../app'
import { csrfCookieName } from '../../shared/middleware/csrf'
import { EmployeeUser } from '../../shared/service-client'

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
    tester = await GatewayTester.start(app, 'employee')
  })
  beforeEach(async () => tester.login(mockUser))
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  async function setupCsrfToken() {
    tester.nockScope.get(`/system/employee/${mockUser.id}`).reply(200, mockUser)
    await tester.client.get('/api/internal/auth/status')
    tester.nockScope.done()

    const csrfToken = await tester.getCookie(csrfCookieName('employee'))
    expect(csrfToken).toBeTruthy()
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
    await setupCsrfToken()

    tester.nockScope.post('/some-proxied-api').reply(200)
    const res = await tester.client.post('/api/internal/some-proxied-api')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should not check CSRF if a session is not available', async () => {
    await setupCsrfToken()
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
