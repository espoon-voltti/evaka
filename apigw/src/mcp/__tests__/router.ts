// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'

import { configFromEnv } from '../../shared/config.ts'
import { GatewayTester } from '../../shared/test/gateway-tester.ts'

type Headers = Record<string, string | string[] | undefined>

describe('MCP proxy', () => {
  let tester: GatewayTester
  beforeAll(async () => {
    tester = await GatewayTester.start(configFromEnv(), 'employee')
  })
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  async function proxiedHeaders(clientHeaders: Headers): Promise<Headers> {
    let forwarded: Headers = {}
    tester.nockScope.post('/mcp').reply(function () {
      forwarded = Object.fromEntries(
        Object.entries(this.req.headers).map(([key, value]) => [
          key.toLowerCase(),
          value
        ])
      )
      return [200, { jsonrpc: '2.0', id: 1, result: {} }]
    })
    const res = await tester.client.post(
      '/api/mcp',
      { jsonrpc: '2.0', id: 1, method: 'ping' },
      { headers: clientHeaders, validateStatus: () => true }
    )
    expect(res.status).toBe(200)
    tester.nockScope.done()
    return forwarded
  }

  // evaka-service trusts X-User from any request that carries a valid apigw JWT, so a client must
  // never be able to smuggle it through the proxy
  it('strips a forged X-User header and forwards the bearer token under its own header', async () => {
    const forwarded = await proxiedHeaders({
      authorization: 'Bearer client-token',
      'x-user': JSON.stringify({
        type: 'employee',
        id: '8fc11215-6d55-4059-bd59-038bfa36f294',
        globalRoles: ['ADMIN'],
        allScopedRoles: []
      }),
      'x-evaka-mcp-authorization': 'Bearer forged-token'
    })
    expect(forwarded['x-user']).toBeUndefined()
    expect(forwarded['x-evaka-mcp-authorization']).toBe('Bearer client-token')
    expect(forwarded.authorization).toMatch(/^Bearer /)
    expect(forwarded.authorization).not.toBe('Bearer client-token')
  })

  it('does not forward a forged MCP authorization header when the client sends no bearer token', async () => {
    const forwarded = await proxiedHeaders({
      'x-evaka-mcp-authorization': 'Bearer forged-token'
    })
    expect(forwarded['x-evaka-mcp-authorization']).toBeUndefined()
    expect(forwarded.authorization).toMatch(/^Bearer /)
  })

  it('does not require a session or a CSRF header', async () => {
    tester.nockScope
      .post('/mcp')
      .reply(200, { jsonrpc: '2.0', id: 1, result: {} })
    const res = await tester.client.post(
      '/api/mcp',
      { jsonrpc: '2.0', id: 1, method: 'ping' },
      { validateStatus: () => true }
    )
    expect(res.status).toBe(200)
    tester.nockScope.done()
  })
})
