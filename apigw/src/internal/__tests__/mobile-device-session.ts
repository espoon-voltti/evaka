// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { GatewayTester } from '../../shared/test/gateway-tester'
import app from '../app'
import { AuthenticatedUser } from '../../shared/service-client'
import { mobileLongTermCookieName } from '../mobile-device-session'
import { sessionCookie } from '../../shared/session'

const pairingId = '009da566-19ca-432e-ad2d-3041481b5bae'
const mobileDeviceId = '7f81ec05-657a-4d18-8196-67f4c8a33989'

const mockUser: AuthenticatedUser = {
  id: mobileDeviceId,
  roles: []
}

describe('Mobile device pairing process', () => {
  let tester: GatewayTester
  beforeAll(async () => {
    tester = await GatewayTester.start(app, 'employee')
  })
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  async function finishPairing() {
    tester.nockScope
      .post(`/system/pairings/${pairingId}/validation`)
      .reply(200, {
        mobileDeviceId
      })
    await tester.client.post('/api/internal/auth/mobile', {
      id: pairingId,
      challengeKey: 'challenge',
      responseKey: 'response'
    })
    tester.nockScope.done()

    expect(await tester.getCookie(mobileLongTermCookieName)).toBeTruthy()
    expect(await tester.getCookie(sessionCookie('employee'))).toBeTruthy()
  }

  it('creates an active session', async () => {
    await finishPairing()

    tester.nockScope.get('/some-proxied-api').reply(200)
    const res = await tester.client.get('/api/internal/some-proxied-api')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('creates a long-term token that refreshes active session when /auth/status is called', async () => {
    await finishPairing()

    await tester.expireSession()
    expect(await tester.getCookie(sessionCookie('employee'))).toBeUndefined()

    tester.nockScope.get(`/system/mobile-devices/${mobileDeviceId}`).reply(200)
    tester.nockScope.get(`/employee/${mobileDeviceId}`).reply(200, mockUser)
    const res = await tester.client.get('/api/internal/auth/status')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('creates a long-term token that refreshes active session when any internal API is called', async () => {
    await finishPairing()

    await tester.expireSession()
    expect(await tester.getCookie(sessionCookie('employee'))).toBeUndefined()

    tester.nockScope.get(`/system/mobile-devices/${mobileDeviceId}`).reply(200)
    tester.nockScope.get('/some-proxied-api').reply(200)
    const res = await tester.client.get('/api/internal/some-proxied-api')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
})
