// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { randomUUID } from 'node:crypto'

import cookieParser from 'cookie-parser'
import { z } from 'zod'

import type { EvakaSessionUser } from '../../shared/auth/index.ts'
import {
  filterValidDeviceAuthHistory,
  setDeviceAuthHistoryCookie
} from '../../shared/device-cookies.ts'
import { toRequestHandler } from '../../shared/express.ts'
import { logAuditEvent, logWarn } from '../../shared/logging.ts'
import type { RedisClient } from '../../shared/redis-client.ts'
import {
  citizenPasskeyLogin,
  citizenPasskeyLoginOptions
} from '../../shared/service-client.ts'
import type { Sessions } from '../../shared/session.ts'

const challengeKey = (id: string) => `passkey-login:${id}`

export const PASSKEY_CHALLENGE_TTL_SECONDS = 5 * 60

const eventCode = (name: string) => `evaka.citizen_passkey.${name}`

export const authPasskeyLoginOptions = (redis: RedisClient) =>
  toRequestHandler(async (req, res) => {
    const { assertionRequest, credentialsGet } =
      await citizenPasskeyLoginOptions(req)
    const key = randomUUID()
    await redis.set(challengeKey(key), assertionRequest, {
      EX: PASSKEY_CHALLENGE_TTL_SECONDS
    })
    res.json({
      challengeKey: key,
      options: JSON.parse(credentialsGet) as unknown
    })
  })

const FinishRequest = z.object({
  challengeKey: z.string().min(1).max(128),
  /** JSON serialization of the PublicKeyCredential returned by the browser */
  credential: z.string().min(1)
})

export const authPasskeyLoginFinish = (
  sessions: Sessions<'citizen'>,
  redis: RedisClient,
  cookieSecret: string
) => [
  cookieParser(cookieSecret),
  toRequestHandler(async (req, res) => {
    logAuditEvent(eventCode('sign_in_requested'), req, 'Login endpoint called')
    try {
      const { challengeKey: key, credential } = FinishRequest.parse(req.body)

      // single-use: no other login attempt may use the same challenge
      const assertionRequest = await redis.getDel(challengeKey(key))
      if (assertionRequest === null) {
        logAuditEvent(
          eventCode('sign_in_failed'),
          req,
          'Unknown or expired passkey challenge'
        )
        res.sendStatus(403)
        return
      }

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

      const { id } = await citizenPasskeyLogin(req, {
        assertionRequest,
        credential,
        deviceAuthHistory
      })
      const user: EvakaSessionUser = {
        id,
        authType: 'citizen-passkey',
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
