// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import http from 'node:http'

import axios from 'axios'
import express from 'express'
import { afterAll, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

import { MockRedisClient } from '../../shared/test/mock-redis-client.ts'
import { getMobileSession } from '../../shared/mobile-session.ts'
import { authMobileLogin } from '../routes/auth-mobile-login.ts'

vi.mock('../../shared/service-client.ts', () => ({
  citizenWeakLogin: vi.fn()
}))

import { citizenWeakLogin } from '../../shared/service-client.ts'

describe('authMobileLogin', () => {
  const redis = new MockRedisClient()
  let server: http.Server
  let baseUrl: string

  beforeAll(async () => {
    const app = express()
    app.use(express.json())
    // loginAttemptsPerHour = 0 disables rate limiting
    app.post('/login', authMobileLogin(redis, 0))
    await new Promise<void>((resolve) => {
      server = app.listen(0, () => resolve())
    })
    const addr = server.address() as { port: number }
    baseUrl = `http://localhost:${addr.port}`
  })

  beforeEach(() => {
    vi.resetAllMocks()
  })

  afterAll(async () => {
    await new Promise<void>((resolve, reject) =>
      server.close((err) => (err ? reject(err) : resolve()))
    )
  })

  it('happy path: returns 200 with token, expiresAt, and user; Redis has exactly one session entry', async () => {
    vi.mocked(citizenWeakLogin).mockResolvedValue({ id: 'citizen-123' })

    const res = await axios.post(
      `${baseUrl}/login`,
      { username: 'test@example.com', password: 'secret' },
      { validateStatus: () => true }
    )

    expect(res.status).toBe(200)
    expect(typeof res.data.token).toBe('string')
    expect(res.data.token.length).toBeGreaterThan(0)
    expect(res.data.expiresAt).toBeGreaterThan(Date.now())
    expect(res.data.user).toEqual({
      id: 'citizen-123',
      authType: 'citizen-mobile-weak'
    })

    // Verify the token resolves to a real session in Redis
    const session = await getMobileSession(redis, res.data.token as string)
    expect(session).not.toBeNull()
    expect(session?.userId).toBe('citizen-123')
  })

  it('invalid credentials: citizenWeakLogin rejects → returns 403', async () => {
    vi.mocked(citizenWeakLogin).mockRejectedValue(new Error('Unauthorized'))

    const res = await axios.post(
      `${baseUrl}/login`,
      { username: 'bad@example.com', password: 'wrong' },
      { validateStatus: () => true }
    )

    expect(res.status).toBe(403)
  })
})
