// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { RequestHandler } from 'express'

import { getMobileSession } from '../shared/mobile-session.ts'
import type { RedisClient } from '../shared/redis-client.ts'

export const mobileAuthMiddleware =
  (redis: RedisClient): RequestHandler =>
  async (req, res, next) => {
    const header = req.header('authorization') ?? ''
    const token = header.replace(/^Bearer\s+/i, '').trim()
    if (!token) return res.sendStatus(401)
    const session = await getMobileSession(redis, token)
    if (!session) return res.sendStatus(401)
    ;(req as any).user = {
      id: session.userId,
      authType: 'citizen-mobile-weak',
      userType: 'CITIZEN_MOBILE_WEAK'
    }
    next()
  }
