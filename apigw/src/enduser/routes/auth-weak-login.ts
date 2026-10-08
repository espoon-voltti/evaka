// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import cookieParser from 'cookie-parser'
import { z } from 'zod'

import type { EvakaSessionUser } from '../../shared/auth/index.ts'
import {
  filterValidDeviceAuthHistory,
  setDeviceAuthHistoryCookie
} from '../../shared/device-cookies.ts'
import { toRequestHandler } from '../../shared/express.ts'
import { logAuditEvent, logWarn } from '../../shared/logging.ts'
import { consumeRateLimit } from '../../shared/rate-limit.ts'
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
          logWarn('Invalid device cookie signature detected', req, {
            eventCode: eventCode('invalid_device_cookie_signature'),
            cookieName,
            hash
          })
        }
      )

      if (
        loginAttemptsPerHour > 0 &&
        !(
          await consumeRateLimit(
            redis,
            `citizen-weak-login:${username}`,
            loginAttemptsPerHour,
            60 * 60
          )
        ).allowed
      ) {
        logWarn('Login request hit rate limit', req, {
          username
        })
        res.sendStatus(429)
        return
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

      setDeviceAuthHistoryCookie(res, user.id, cookieSecret)

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
