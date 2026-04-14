// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { toRequestHandler } from '../../shared/express.ts'
import { getMobileSession } from '../../shared/mobile-session.ts'
import type { RedisClient } from '../../shared/redis-client.ts'

export const authMobileStatus = (redis: RedisClient) =>
  toRequestHandler(async (req, res) => {
    const header = req.header('authorization') ?? ''
    const token = header.replace(/^Bearer\s+/i, '').trim()
    if (!token) {
      res.status(401).json({ loggedIn: false })
      return
    }
    const session = await getMobileSession(redis, token)
    if (!session) {
      res.status(401).json({ loggedIn: false })
      return
    }
    res.json({
      loggedIn: true,
      user: { id: session.userId, authType: 'citizen-mobile-weak' },
      expiresAt: session.expiresAt
    })
  })
