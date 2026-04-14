// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import http from 'node:http'

import axios from 'axios'
import express from 'express'
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest'

import { createMobileSession } from '../../shared/mobile-session.ts'
import { MockRedisClient } from '../../shared/test/mock-redis-client.ts'
import { mobileAuthMiddleware } from '../mobile-auth-middleware.ts'

describe('mobileAuthMiddleware', () => {
  const redis = new MockRedisClient()
  let server: http.Server
  let baseUrl: string

  beforeAll(async () => {
    const app = express()
    app.get('/protected', mobileAuthMiddleware(redis), (req, res) => {
      res.status(200).json((req as any).user)
    })
    await new Promise<void>((resolve) => {
      server = app.listen(0, () => resolve())
    })
    const addr = server.address() as { port: number }
    baseUrl = `http://localhost:${addr.port}`
  })

  afterAll(async () => {
    await new Promise<void>((resolve, reject) =>
      server.close((err) => (err ? reject(err) : resolve()))
    )
  })

  it('returns 401 when no Authorization header is present', async () => {
    const res = await axios.get(`${baseUrl}/protected`, {
      validateStatus: () => true
    })
    expect(res.status).toBe(401)
  })

  it('returns 401 when token is invalid / not in Redis', async () => {
    const res = await axios.get(`${baseUrl}/protected`, {
      headers: { Authorization: 'Bearer invalid-token-xyz' },
      validateStatus: () => true
    })
    expect(res.status).toBe(401)
  })

  it('allows request and sets req.user correctly when token is valid', async () => {
    const { token } = await createMobileSession(redis, 'citizen-abc')

    const res = await axios.get(`${baseUrl}/protected`, {
      headers: { Authorization: `Bearer ${token}` },
      validateStatus: () => true
    })

    expect(res.status).toBe(200)
    expect(res.data).toMatchObject({
      id: 'citizen-abc',
      authType: 'citizen-mobile-weak',
      userType: 'CITIZEN_MOBILE_WEAK'
    })
  })
})
