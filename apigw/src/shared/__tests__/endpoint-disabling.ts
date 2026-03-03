// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  afterAll,
  afterEach,
  beforeAll,
  describe,
  expect,
  it,
  jest
} from '@jest/globals'
import type express from 'express'

import { configFromEnv } from '../config.ts'
import {
  matchPath,
  createEndpointDisablingMiddleware
} from '../middleware/endpoint-disabling.ts'
import { GatewayTester } from '../test/gateway-tester.ts'
import { MockRedisClient } from '../test/mock-redis-client.ts'

describe('matchPath', () => {
  describe('exact matches', () => {
    it('matches identical paths', () => {
      expect(matchPath('/citizen/absences', '/citizen/absences')).toBe(true)
    })
    it('does not match different paths', () => {
      expect(matchPath('/citizen/absences', '/citizen/other')).toBe(false)
    })
    it('does not match when path has extra segments', () => {
      expect(matchPath('/citizen/absences', '/citizen/absences/123')).toBe(
        false
      )
    })
    it('does not match when pattern has extra segments', () => {
      expect(matchPath('/citizen/absences/123', '/citizen/absences')).toBe(
        false
      )
    })
    it('matches root path', () => {
      expect(matchPath('/', '/')).toBe(true)
    })
  })

  describe('single wildcard *', () => {
    it('matches one segment', () => {
      expect(matchPath('/citizen/absences/*', '/citizen/absences/123')).toBe(
        true
      )
    })
    it('does not match zero segments', () => {
      expect(matchPath('/citizen/absences/*', '/citizen/absences')).toBe(false)
    })
    it('does not match multiple segments', () => {
      expect(
        matchPath('/citizen/absences/*', '/citizen/absences/123/foo')
      ).toBe(false)
    })
    it('works in the middle of a pattern', () => {
      expect(matchPath('/citizen/*/decisions', '/citizen/abc/decisions')).toBe(
        true
      )
    })
    it('does not match empty segment in the middle', () => {
      expect(matchPath('/citizen/*/decisions', '/citizen//decisions')).toBe(
        false
      )
    })
    it('works with multiple wildcards', () => {
      expect(
        matchPath('/citizen/*/absences/*', '/citizen/abc/absences/123')
      ).toBe(true)
    })
  })

  describe('double wildcard **', () => {
    it('matches zero segments at end', () => {
      expect(matchPath('/citizen/**', '/citizen')).toBe(true)
    })
    it('matches one segment at end', () => {
      expect(matchPath('/citizen/**', '/citizen/absences')).toBe(true)
    })
    it('matches many segments at end', () => {
      expect(matchPath('/citizen/**', '/citizen/absences/123/foo')).toBe(true)
    })
    it('works in the middle of a pattern', () => {
      expect(
        matchPath('/citizen/**/decisions', '/citizen/abc/def/decisions')
      ).toBe(true)
    })
    it('matches zero segments in the middle', () => {
      expect(matchPath('/citizen/**/decisions', '/citizen/decisions')).toBe(
        true
      )
    })
    it('works with * and ** combined', () => {
      expect(matchPath('/citizen/**/foo/*', '/citizen/a/b/foo/x')).toBe(true)
    })
    it('does not match when trailing segment is wrong', () => {
      expect(matchPath('/citizen/**/decisions', '/citizen/abc/other')).toBe(
        false
      )
    })
  })

  describe('edge cases', () => {
    it('empty pattern matches empty path', () => {
      expect(matchPath('', '')).toBe(true)
    })
    it('empty pattern does not match non-empty path', () => {
      expect(matchPath('', '/foo')).toBe(false)
    })
    it('** alone matches everything', () => {
      expect(matchPath('**', '/any/path/at/all')).toBe(true)
    })
    it('** alone matches empty path', () => {
      expect(matchPath('**', '')).toBe(true)
    })
  })
})

describe('endpointDisablingMiddleware', () => {
  it('returns 503 when endpoint is disabled', async () => {
    const redis = new MockRedisClient()
    await redis.sAdd('disabled-endpoints', 'GET /citizen/test')
    const { middleware, refreshCache, cleanup } =
      createEndpointDisablingMiddleware(redis)
    await refreshCache()

    const req = { method: 'GET', path: '/citizen/test' } as express.Request
    const statusFn = jest.fn().mockReturnThis()
    const jsonFn = jest.fn().mockReturnThis()
    const res = {
      status: statusFn,
      json: jsonFn
    } as unknown as express.Response
    const next = jest.fn()

    middleware(req, res, next)
    expect(statusFn).toHaveBeenCalledWith(503)
    expect(jsonFn).toHaveBeenCalledWith(
      expect.objectContaining({ errorCode: 'ENDPOINT_DISABLED' })
    )
    expect(next).not.toHaveBeenCalled()
    cleanup()
  })

  it('calls next when endpoint is not disabled', async () => {
    const redis = new MockRedisClient()
    const { middleware, refreshCache, cleanup } =
      createEndpointDisablingMiddleware(redis)
    await refreshCache()

    const req = { method: 'GET', path: '/citizen/test' } as express.Request
    const res = {} as express.Response
    const next = jest.fn()

    middleware(req, res, next)
    expect(next).toHaveBeenCalled()
    cleanup()
  })

  it('calls next when method does not match', async () => {
    const redis = new MockRedisClient()
    await redis.sAdd('disabled-endpoints', 'POST /citizen/test')
    const { middleware, refreshCache, cleanup } =
      createEndpointDisablingMiddleware(redis)
    await refreshCache()

    const req = { method: 'GET', path: '/citizen/test' } as express.Request
    const res = {} as express.Response
    const next = jest.fn()

    middleware(req, res, next)
    expect(next).toHaveBeenCalled()
    cleanup()
  })

  it('returns 503 when wildcard method matches', async () => {
    const redis = new MockRedisClient()
    await redis.sAdd('disabled-endpoints', '* /citizen/test')
    const { middleware, refreshCache, cleanup } =
      createEndpointDisablingMiddleware(redis)
    await refreshCache()

    const req = { method: 'GET', path: '/citizen/test' } as express.Request
    const statusFn = jest.fn().mockReturnThis()
    const jsonFn = jest.fn().mockReturnThis()
    const res = {
      status: statusFn,
      json: jsonFn
    } as unknown as express.Response
    const next = jest.fn()

    middleware(req, res, next)
    expect(statusFn).toHaveBeenCalledWith(503)
    expect(next).not.toHaveBeenCalled()
    cleanup()
  })
})

describe('endpoint disabling integration via GatewayTester', () => {
  let tester: GatewayTester

  beforeAll(async () => {
    // Pre-seed the mock Redis with a disabled endpoint entry BEFORE starting
    // the gateway. The middleware fires `void refreshCache()` on creation,
    // and since MockRedisClient resolves synchronously, the cache is populated
    // before the HTTP server listen callback fires.
    const redis = new MockRedisClient()
    await redis.sAdd('disabled-endpoints', 'GET /citizen/public/blocked')
    tester = await GatewayTester.start(configFromEnv(), 'citizen', redis)
    // Allow the initial async refreshCache() microtask to settle
    await new Promise((resolve) => setTimeout(resolve, 0))
  })
  afterEach(async () => tester.afterEach())
  afterAll(async () => tester?.stop())

  it('returns 503 for a request matching a disabled endpoint', async () => {
    const res = await tester.client.get('/api/citizen/public/blocked', {
      validateStatus: () => true
    })
    expect(res.status).toBe(503)
    expect(res.data).toEqual(
      expect.objectContaining({ errorCode: 'ENDPOINT_DISABLED' })
    )
  })

  it('proxies a request that does not match any disabled endpoint', async () => {
    tester.nockScope.get('/citizen/public/allowed').reply(200, { ok: true })
    const res = await tester.client.get('/api/citizen/public/allowed', {
      validateStatus: () => true
    })
    tester.nockScope.done()
    expect(res.status).toBe(200)
    expect(res.data).toEqual({ ok: true })
  })
})
