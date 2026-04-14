// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'

import { createProxy } from '../../shared/proxy-utils.ts'
import {
  client,
  createServiceRequestHeaders
} from '../../shared/service-client.ts'
import type { Sessions } from '../../shared/session.ts'

export function passkeyCredentialsRoutes(
  sessions: Sessions<'citizen'>
): express.Router {
  const router = express.Router()

  const proxy = createProxy({
    path: (req) => req.originalUrl.replace(/^\/api/, ''),
    getUserHeader: (req) => sessions.getUserHeader(req)
  })

  // GET / → proxy to Kotlin GET /citizen/passkey/credentials
  router.get('/', proxy)

  // PATCH /:id → proxy to Kotlin PATCH /citizen/passkey/credentials/:id
  router.patch('/:id', proxy)

  // DELETE /:id → call Kotlin DELETE /citizen/passkey/credentials/:id directly,
  // then destroy the session if the deleted credential matches the current session.
  router.delete('/:id', express.json(), async (req, res) => {
    const credentialId = req.params.id
    const user = req.user

    const userHeader = sessions.getUserHeader(req)
    await client.delete(
      `/citizen/passkey/credentials/${encodeURIComponent(credentialId)}`,
      { headers: createServiceRequestHeaders(req, userHeader) }
    )

    if (
      user?.authType === 'citizen-passkey' &&
      user.credentialId === credentialId
    ) {
      await sessions.destroy(req, res)
    }

    res.sendStatus(200)
  })

  return router
}
