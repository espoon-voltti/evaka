// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import cookieParser from 'cookie-parser'
import type { Request as ExpressRequest } from 'express'
import { z } from 'zod'

import type { EvakaSessionUser } from '../../shared/auth/index.ts'
import { clientIpKey } from '../../shared/client-ip.ts'
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

export interface WeakLoginRateLimits {
  /** Attempts per hour per username, 0 means no limit */
  perUsername: number
  /** Attempts per hour per client IP, 0 means no limit */
  perIp: number
  /** If false, exceeding perIp is only logged */
  enforcePerIp: boolean
}

async function rejectedByIpRateLimit(
  req: ExpressRequest,
  redis: RedisClient,
  { perIp, enforcePerIp }: WeakLoginRateLimits
): Promise<boolean> {
  if (perIp <= 0) return false
  const ipKey = clientIpKey(req.ip)
  const realIp = req.headers['x-real-ip']
  const xRealIpMatches =
    ipKey !== undefined &&
    typeof realIp === 'string' &&
    clientIpKey(realIp) === ipKey
  const logMismatch = () =>
    logWarn('Client IP differs from X-Real-IP', req, {
      eventCode: eventCode('client_ip_mismatch'),
      ipKey
    })

  if (ipKey === undefined) {
    logMismatch()
    return false
  }
  const { allowed, count } = await consumeRateLimit(
    redis,
    `citizen-weak-login-ip:${ipKey}`,
    perIp,
    60 * 60
  )
  if (count === 1 && !xRealIpMatches) logMismatch()
  if (count === perIp + 1) {
    logWarn('Login request exceeded IP rate limit', req, {
      eventCode: eventCode('ip_rate_limit_exceeded'),
      ipKey,
      xRealIpMatches,
      enforced: enforcePerIp
    })
  }
  return !allowed && enforcePerIp
}

export const authWeakLogin = (
  sessions: Sessions<'citizen'>,
  redis: RedisClient,
  cookieSecret: string,
  rateLimits: WeakLoginRateLimits
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

      // Checked before the username limit so that attempts rejected here don't
      // count against the usernames they target
      if (await rejectedByIpRateLimit(req, redis, rateLimits)) {
        res.sendStatus(429)
        return
      }

      if (
        rateLimits.perUsername > 0 &&
        !(
          await consumeRateLimit(
            redis,
            `citizen-weak-login:${username}`,
            rateLimits.perUsername,
            60 * 60
          )
        ).allowed
      ) {
        logWarn('Login request hit rate limit', req, {
          eventCode: eventCode('rate_limited'),
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
