// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { toRequestHandler } from '../../shared/express.ts'
import { logAuditEvent } from '../../shared/logging.ts'
import { deleteMobileSession } from '../../shared/mobile-session.ts'
import type { RedisClient } from '../../shared/redis-client.ts'

const eventCode = (name: string) => `evaka.citizen_mobile.${name}`

export const authMobileLogout = (redis: RedisClient) =>
  toRequestHandler(async (req, res) => {
    const header = req.header('authorization') ?? ''
    const token = header.replace(/^Bearer\s+/i, '').trim()
    if (token) await deleteMobileSession(redis, token)
    logAuditEvent(eventCode('sign_out'), req, 'Mobile user logged out')
    res.sendStatus(204)
  })
