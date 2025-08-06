// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { getHours } from 'date-fns/getHours'
import { z } from 'zod'

import type { EvakaSessionUser } from '../../shared/auth/index.js'
import { toRequestHandler } from '../../shared/express.js'
import { logAuditEvent, logWarn } from '../../shared/logging.js'
import type { RedisClient } from '../../shared/redis-client.js'
import { citizenWeakLogin } from '../../shared/service-client.js'
import type { Sessions } from '../../shared/session.js'

const Request = z.object({
  username: z
    .string()
    .min(1)
    .max(128)
    .transform((email) => email.toLowerCase()),
  password: z.string().min(1).max(128)
})

const eventCode = (name: string) => `evaka.citizen_weak.${name}`

export const authWeakLogin = (
  sessions: Sessions<'citizen'>,
  loginAttemptsPerHour: number,
  redis: RedisClient
) =>
  toRequestHandler(async (req, res) => {
    logAuditEvent(eventCode('sign_in_requested'), req, 'Login endpoint called')
    try {
      const body = Request.parse(req.body)

      if (loginAttemptsPerHour > 0) {
        // Apply rate limit (attempts per hour)
        // Reference: Redis Rate Limiting Best Practices
        // https://redis.io/glossary/rate-limiting/
        const hour = getHours(new Date())
        const key = `citizen-weak-login:${body.username}:${hour}`
        const value = Number.parseInt((await redis.get(key)) ?? '', 10)
        if (Number.isNaN(value) || value < loginAttemptsPerHour) {
          // expire in 1 hour, so there's no old entry when the hours value repeats the next day
          const expirySeconds = 60 * 60
          await redis.multi().incr(key).expire(key, expirySeconds).exec()
        } else {
          logWarn('Login request hit rate limit', req, {
            username: body.username
          })
          res.sendStatus(429)
          return
        }
      }

      const { id } = await citizenWeakLogin(req, body)
      const user: EvakaSessionUser = {
        id,
        authType: 'citizen-weak',
        userType: 'CITIZEN_WEAK'
      }
      await sessions.login(req, user)
      logAuditEvent(eventCode('sign_in'), req, 'User logged in successfully')
      res.sendStatus(200)
    } catch (err) {
      logAuditEvent(
        eventCode('sign_in_failed'),
        req,
        `Error logging user in. Error: ${err?.toString()}`
      )
      if (!res.headersSent) {
        if (err instanceof z.ZodError) {
          res.sendStatus(400)
        } else {
          res.sendStatus(403)
        }
      } else {
        throw err
      }
    }
  })
