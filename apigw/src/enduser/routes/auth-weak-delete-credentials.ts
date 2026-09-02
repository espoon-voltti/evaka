// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { toRequestHandler } from '../../shared/express.ts'
import { logAuditEvent } from '../../shared/logging.ts'
import type { RedisClient } from '../../shared/redis-client.ts'
import { citizenWeakLoginCredentialsDelete } from '../../shared/service-client.ts'
import { revokeWeakSessions } from '../../shared/session.ts'

export const authWeakDeleteCredentials = (redisClient: RedisClient) =>
  toRequestHandler(async (req, res) => {
    logAuditEvent(
      'evaka.citizen_weak.credentials_delete_requested',
      req,
      'Delete weak login credentials endpoint called'
    )
    try {
      if (!req.session.evaka) {
        res.sendStatus(401)
        return
      }

      await citizenWeakLoginCredentialsDelete(req, req.session.evaka.user)
      logAuditEvent(
        'evaka.citizen_weak.credentials_deleted',
        req,
        'Weak login credentials deleted'
      )

      if (await revokeWeakSessions(redisClient, req.session.evaka.userIdHash)) {
        logAuditEvent(
          'evaka.citizen_weak.logout_other_sessions',
          req,
          'Logged out other sessions'
        )
      }

      res.status(204).send()
    } catch (err) {
      logAuditEvent(
        'evaka.citizen_weak.credentials_delete_failed',
        req,
        `Error deleting weak login credentials. Error: ${err?.toString()}`
      )
      throw err
    }
  })
