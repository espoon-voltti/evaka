// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'

import { enduserGwRouter } from './enduser/app.js'
import { internalGwRouter } from './internal/app.js'
import { appCommit, Config } from './shared/config.js'
import { cacheControl } from './shared/middleware/cache-control.js'
import { errorHandler } from './shared/middleware/error-handler.js'
import { RedisClient } from './shared/redis-client.js'
import { handleCspReport } from './shared/routes/csp.js'

export function apiRouter(config: Config, redisClient: RedisClient) {
  const router = express.Router()
  router.use((req, _, next) => {
    if (req.url === '/application/version' || req.url === '/internal/version') {
      req.url = '/version'
    }
    next()
  })
  router.use(
    cacheControl((req) =>
      req.path.startsWith('/api/citizen/child-images/') ||
      req.path.startsWith('/api/employee-mobile/child-images/')
        ? 'allow-cache'
        : 'forbid-cache'
    )
  )

  router.post(
    '/csp/report',
    express.json({ type: 'application/csp-report' }),
    handleCspReport
  )
  router.get('/version', (_, res) => {
    res.send({ commitId: appCommit })
  })

  router.use('/application', enduserGwRouter(config, redisClient))
  router.use('/internal', internalGwRouter(config, redisClient))

  // global error middleware
  router.use(errorHandler(false))
  return router
}
