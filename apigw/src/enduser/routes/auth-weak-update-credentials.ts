// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'

import { toRequestHandler } from '../../shared/express.ts'
import { logAuditEvent } from '../../shared/logging.ts'
import type { RedisClient } from '../../shared/redis-client.ts'
import { citizenWeakLoginCredentialsUpdate } from '../../shared/service-client.ts'

const Request = z.object({
  username: z
    .string()
    .min(1)
    .max(128)
    .transform((email) => email.toLowerCase())
    .nullable(),
  password: z.string().min(1).max(128).nullable()
})

export const authWeakUpdateCredentials = (redisClient: RedisClient) =>
  toRequestHandler(async (req, res) => {
    logAuditEvent(
      'evaka.citizen_weak.credentials_update_requested',
      req,
      'Update weak login credentials endpoint called'
    )
    try {
      if (!req.session.evaka) {
        res.sendStatus(401)
        return
      }
      const { username, password } = Request.parse(req.body)
      const user = req.session.evaka.user
      const userIdHash = req.session.evaka.userIdHash

      await citizenWeakLoginCredentialsUpdate(req, user, {
        username,
        password
      })
      logAuditEvent(
        'evaka.citizen_weak.credentials_updated_successfully',
        req,
        'Weak login credentials updated'
      )

      const sessionIds = await redisClient.sMembers(`usess:${userIdHash}`)
      if (sessionIds.length > 0) {
        await redisClient.del(
          sessionIds.map((sessionId) => `sess:${sessionId}`)
        )
        await redisClient.sRem(`usess:${userIdHash}`, sessionIds)
        logAuditEvent(
          'evaka.citizen_weak.logout_other_sessions',
          req,
          'Logged out other sessions'
        )
      }

      res.status(204).send()
    } catch (err) {
      logAuditEvent(
        'evaka.citizen_weak.credentials_update_failed',
        req,
        `Error updating weak login credentials. Error: ${err?.toString()}`
      )
      throw err
    }
  })
