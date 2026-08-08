// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { z } from 'zod'

import { toRequestHandler } from '../../shared/express.ts'
import { logAuditEvent } from '../../shared/logging.ts'
import type { RedisClient } from '../../shared/redis-client.ts'
import { citizenDeletePasskey } from '../../shared/service-client.ts'
import { revokeWeakSessions } from '../../shared/session.ts'

const Params = z.object({ id: z.uuid() })

export const passkeyDelete = (redisClient: RedisClient) =>
  toRequestHandler(async (req, res) => {
    logAuditEvent(
      'evaka.citizen_passkey.delete_requested',
      req,
      'Delete passkey endpoint called'
    )
    try {
      if (!req.session.evaka) {
        res.sendStatus(401)
        return
      }
      const { id } = Params.parse(req.params)

      await citizenDeletePasskey(req, req.session.evaka.user, id)
      logAuditEvent('evaka.citizen_passkey.deleted', req, 'Passkey deleted')

      if (await revokeWeakSessions(redisClient, req.session.evaka.userIdHash)) {
        logAuditEvent(
          'evaka.citizen_passkey.logout_other_sessions',
          req,
          'Logged out other sessions'
        )
      }

      res.status(204).send()
    } catch (err) {
      logAuditEvent(
        'evaka.citizen_passkey.delete_failed',
        req,
        `Error deleting passkey. Error: ${err?.toString()}`
      )
      throw err
    }
  })
