// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'

import { EvakaSessionUser, login } from '../../shared/auth/index.js'
import { toRequestHandler } from '../../shared/express.js'
import { logAuditEvent } from '../../shared/logging.js'
import { RedisClient } from '../../shared/redis-client.js'
import { citizenWeakLogin } from '../../shared/service-client.js'

const Request = z.object({
  username: z.string().min(1).max(128),
  password: z.string().min(1).max(128)
})

const eventCode = (name: string) => `evaka.citizen_weak.${name}`

export const authWeakLogin = (redis: RedisClient) =>
  toRequestHandler(async (req, res) => {
    logAuditEvent(eventCode('sign_in_requested'), req, 'Login endpoint called')
    try {
      const body = Request.parse(req.body)

      // Apply rate limit
      const key = 'citizen-weak-login:' + body.username
      const value = req.traceId ?? ''
      const previous = await redis.set(key, value, {
        EX: 1, // expiry in seconds
        GET: true, // return previous value
        NX: true // only set if key doesn't exist
      })
      if (previous) throw new Error('Login rate limit for user triggered')

      const { id } = await citizenWeakLogin(body)
      const user: EvakaSessionUser = {
        id,
        userType: 'CITIZEN_WEAK',
        globalRoles: [],
        allScopedRoles: []
      }
      await login(req, user)
      logAuditEvent(eventCode('sign_in'), req, 'User logged in successfully')
      res.sendStatus(200)
    } catch (err) {
      logAuditEvent(
        eventCode('sign_in_failed'),
        req,
        `Error logging user in. Error: ${err?.toString()}`
      )
      if (!res.headersSent) {
        res.sendStatus(403)
      } else {
        throw err
      }
    }
  })
