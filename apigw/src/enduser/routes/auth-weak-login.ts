// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import cookieParser from 'cookie-parser'
import { getHours } from 'date-fns/getHours'
import { z } from 'zod'

import type { EvakaSessionUser } from '../../shared/auth/index.ts'
import {
  filterValidDeviceAuthHistory,
  setDeviceAuthHistoryCookie
} from '../../shared/device-cookies.ts'
import { toRequestHandler } from '../../shared/express.ts'
import { logAuditEvent, logWarn } from '../../shared/logging.ts'
import type { RedisClient } from '../../shared/redis-client.ts'
import { citizenWeakLogin } from '../../shared/service-client.ts'
import type { Sessions } from '../../shared/session.ts'

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
  redis: RedisClient,
  cookieSecret: string
) => [
  cookieParser(cookieSecret),
  toRequestHandler(async (req, res) => {
    logAuditEvent(eventCode('sign_in_requested'), req, 'Login endpoint called')
    try {
      const { username, password } = Request.parse(req.body)

      const deviceAuthHistory = filterValidDeviceAuthHistory(
        req.signedCookies,
        (cookieName, hash) => {
          // TODO pitäisikö nämä hylätyt expiroida?
          logWarn('Invalid device cookie signature detected', req, {
            cookieName,
            hash
          })
        }
      )

      if (loginAttemptsPerHour > 0) {
        // Apply rate limit (attempts per hour)
        // Reference: Redis Rate Limiting Best Practices
        // https://redis.io/glossary/rate-limiting/
        const hour = getHours(new Date())
        const key = `citizen-weak-login:${username}:${hour}`
        const value = Number.parseInt((await redis.get(key)) ?? '', 10)
        if (Number.isNaN(value) || value < loginAttemptsPerHour) {
          // expire in 1 hour, so there's no old entry when the hours value repeats the next day
          const expirySeconds = 60 * 60
          await redis.multi().incr(key).expire(key, expirySeconds).exec()
        } else {
          logWarn('Login request hit rate limit', req, {
            username
          })
          res.sendStatus(429)
          return
        }
      }

      const { id } = await citizenWeakLogin(req, {
        username,
        password,
        deviceAuthHistory
      })
      const user: EvakaSessionUser = {
        id,
        authType: 'citizen-weak',
        userType: 'CITIZEN_WEAK'
      }
      await sessions.login(req, user)
      logAuditEvent(eventCode('sign_in'), req, 'User logged in successfully')

      // Set device cookie if it's a new browser
      setDeviceAuthHistoryCookie(res, user.id)

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
]
