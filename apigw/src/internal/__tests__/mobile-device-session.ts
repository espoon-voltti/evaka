// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  describe,
  beforeAll,
  afterEach,
  afterAll,
  expect,
  it
} from '@jest/globals'
import { v4 as uuid } from 'uuid'

import { appCommit, configFromEnv } from '../../shared/config.js'
import { UUID } from '../../shared/service-client.js'
import { sessionCookie } from '../../shared/session.js'
import { GatewayTester } from '../../shared/test/gateway-tester.js'
import { mobileLongTermCookieName } from '../mobile-device-session.js'

const pairingId = '009da566-19ca-432e-ad2d-3041481b5bae'
const mobileDeviceId = '7f81ec05-657a-4d18-8196-67f4c8a33989'

describe('Mobile device pairing process', () => {
  let tester: GatewayTester
  beforeAll(async () => {
    tester = await GatewayTester.start(configFromEnv(), 'employee')
  })
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  async function finishPairing(): Promise<UUID> {
    const token = uuid()
    tester.nockScope
      .post(`/system/pairings/${pairingId}/validation`)
      .reply(200, {
        id: mobileDeviceId,
        longTermToken: token
      })
    await tester.client.post('/api/internal/auth/mobile', {
      id: pairingId,
      challengeKey: 'challenge',
      responseKey: 'response'
    })
    tester.nockScope.done()

    expect(await tester.getCookie(mobileLongTermCookieName)).toBeTruthy()
    expect(await tester.getCookie(sessionCookie('employee'))).toBeTruthy()
    return token
  }

  it('creates an active session', async () => {
    await finishPairing()

    tester.nockScope.get('/employee-mobile/some-proxied-api').reply(200)
    const res = await tester.client.get(
      '/api/internal/employee-mobile/some-proxied-api'
    )
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('creates a long-term token that refreshes active session when /auth/status is called', async () => {
    const longTermToken = await finishPairing()

    await tester.expireSession()
    expect(await tester.getCookie(sessionCookie('employee'))).toBeUndefined()

    tester.nockScope
      .get(`/system/mobile-identity/${longTermToken}`)
      .reply(200, { id: mobileDeviceId, longTermToken })
    tester.nockScope.post(`/system/mobile-devices/${mobileDeviceId}`).reply(200)
    const res = await tester.client.get('/api/internal/auth/status')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it("expires the long-term token if backend doesn't recognize it during refresh", async () => {
    const longTermToken = await finishPairing()
    await tester.expireSession()
    tester.nockScope.get(`/system/mobile-identity/${longTermToken}`).reply(404)
    const res = await tester.client.get('/api/internal/auth/status')
    tester.nockScope.done()
    expect(res.data).toStrictEqual({ loggedIn: false, apiVersion: appCommit })
    expect(await tester.getCookie(mobileLongTermCookieName)).toBeUndefined()
  })
  it("expires the session if /auth/status can't find the mobile device", async () => {
    await finishPairing()
    tester.nockScope.post(`/system/mobile-devices/${mobileDeviceId}`).reply(404)
    const res = await tester.client.get('/api/internal/auth/status')
    expect(res.data).toStrictEqual({ loggedIn: false, apiVersion: appCommit })
    expect(await tester.getCookie(sessionCookie('employee'))).toBeUndefined()
  })
})
