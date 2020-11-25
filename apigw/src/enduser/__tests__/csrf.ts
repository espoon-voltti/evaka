// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { GatewayTester } from '../../shared/test/gateway-tester'
import app from '../app'
import { csrfCookieName } from '../../shared/middleware/csrf'
import { AuthenticatedUser } from '../../shared/service/pis'

const mockUser: AuthenticatedUser = {
  id: '4f73e4f8-8759-46c6-9b9d-4da860138ce2',
  roles: ['ENDUSER']
}

describe('CSRF middleware and cookie handling in enduser-gw', () => {
  let tester: GatewayTester
  beforeAll(async () => {
    tester = await GatewayTester.start(app, 'enduser')
  })
  beforeEach(async () => tester.login(mockUser))
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  async function setupCsrfToken() {
    tester.nockScope.get(`/persondetails/uuid/${mockUser.id}`).reply(200, {
      id: mockUser.id
    })
    await tester.client.get('/api/application/auth/status')
    tester.nockScope.done()

    const csrfToken = await tester.getCookie(csrfCookieName('enduser'))
    expect(csrfToken).toBeTruthy()
  }

  it('should fail a POST to a proxied API when there is no CSRF token', async () => {
    const res = await tester.client.post(
      '/api/application/enduser/v2/applications',
      undefined,
      {
        validateStatus: () => true
      }
    )
    expect(res.status).toBe(403)
  })
  it('should pass GET to a proxied API when there is no CSRF token', async () => {
    tester.nockScope.get('/enduser/v2/applications').reply(200)
    const res = await tester.client.get(
      '/api/application/enduser/v2/applications'
    )
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should pass a POST to a proxied API after CSRF token has been set up by the auth/status endpoint', async () => {
    await setupCsrfToken()

    tester.nockScope.post('/enduser/v2/applications').reply(200)
    const res = await tester.client.post(
      '/api/application/enduser/v2/applications'
    )
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should not check CSRF if a session is not available', async () => {
    await setupCsrfToken()
    await tester.expireSession()

    const res = await tester.client.post(
      '/api/application/enduser/v2/applications',
      undefined,
      {
        validateStatus: () => true
      }
    )
    expect(res.status).toBe(401)
  })
})
