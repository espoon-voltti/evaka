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

import { configFromEnv } from '../../shared/config.js'
import { EmployeeUser } from '../../shared/service-client.js'
import { GatewayTester } from '../../shared/test/gateway-tester.js'

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

  it('should fail POST to a proxied API when there is no CSRF header', async () => {
    const res = await tester.client.post(
      '/api/employee/some-proxied-api',
      undefined,
      {
        validateStatus: () => true
      }
    )
    expect(res.status).toBe(403)
  })
  it('should pass GET to a proxied API when there is no CSRF token', async () => {
    tester.nockScope.get('/employee/some-proxied-api').reply(200)
    const res = await tester.client.get('/api/employee/some-proxied-api')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
  it('should pass POST to a proxied API when CSRF header is present', async () => {
    tester.setCsrfHeader = true
    tester.nockScope.post('/employee/some-proxied-api').reply(200)
    const res = await tester.client.post('/api/employee/some-proxied-api')
    tester.nockScope.done()
    expect(res.status).toBe(200)
  })
})
