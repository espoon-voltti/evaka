// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  afterAll,
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  it
} from '@jest/globals'

import { configFromEnv } from '../../shared/config.ts'
import type { CitizenUser } from '../../shared/service-client.ts'
import { GatewayTester } from '../../shared/test/gateway-tester.ts'

const mockUser: CitizenUser = {
  id: '4f73e4f8-8759-46c6-9b9d-4da860138ce2'
}

describe('CSRF middleware and cookie handling in enduser-gw', () => {
  let tester: GatewayTester
  beforeAll(async () => {
    tester = await GatewayTester.start(configFromEnv(), 'citizen')
  })
  beforeEach(async () => tester.login(mockUser))
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  it('should fail a POST to a proxied API when there is no CSRF token', async () => {
    const res = await tester.client.post(
      '/api/citizen/applications',
      undefined,
      {
        validateStatus: () => true
      }
    )
    expect(res.status).toBe(403)
  })
  it('should pass GET to a proxied API when there is no CSRF token', async () => {
    tester.nockScope.get('/citizen/applications').reply(200)
    const res = await tester.client.get('/api/citizen/applications')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should pass POST to a proxied API when CSRF header is present', async () => {
    tester.setCsrfHeader = true
    tester.nockScope.post('/citizen/applications').reply(200)
    const res = await tester.client.post('/api/citizen/applications')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
})
