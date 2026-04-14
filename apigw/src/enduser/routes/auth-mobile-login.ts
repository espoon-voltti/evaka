// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getHours } from 'date-fns/getHours'
import { z } from 'zod'

import { toRequestHandler } from '../../shared/express.ts'
import { logAuditEvent, logWarn } from '../../shared/logging.ts'
import { createMobileSession } from '../../shared/mobile-session.ts'
import type { RedisClient } from '../../shared/redis-client.ts'
import { citizenWeakLogin } from '../../shared/service-client.ts'

const Body = z.object({
  username: z.string().min(1).max(128).transform((s) => s.toLowerCase()),
  password: z.string().min(1).max(128)
})

const eventCode = (name: string) => `evaka.citizen_mobile.${name}`

export const authMobileLogin = (redis: RedisClient, loginAttemptsPerHour: number) =>
  toRequestHandler(async (req, res) => {
    logAuditEvent(eventCode('sign_in_requested'), req, 'Mobile login endpoint called')
    try {
      const { username, password } = Body.parse(req.body)

      if (loginAttemptsPerHour > 0) {
        const hour = getHours(new Date())
        const key = `citizen-mobile-login:${username}:${hour}`
        const value = Number.parseInt((await redis.get(key)) ?? '', 10)
        if (Number.isNaN(value) || value < loginAttemptsPerHour) {
          const expirySeconds = 60 * 60
          await redis.multi().incr(key).expire(key, expirySeconds).exec()
        } else {
          logWarn('Mobile login request hit rate limit', req, { username })
          res.sendStatus(429)
          return
        }
      }

      const { id } = await citizenWeakLogin(req, {
        username,
        password,
        deviceAuthHistory: []
      })
      const { token, expiresAt } = await createMobileSession(redis, id)
      logAuditEvent(eventCode('sign_in'), req, 'Mobile user logged in successfully')

      res.status(200).json({
        token,
        expiresAt,
        user: { id, authType: 'citizen-mobile-weak' }
      })
    } catch (err) {
      logAuditEvent(eventCode('sign_in_failed'), req, `Mobile login error: ${err?.toString()}`)
      if (err instanceof z.ZodError) {
        res.sendStatus(400)
      } else {
        res.sendStatus(403)
      }
    }
  })
