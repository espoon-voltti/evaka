// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import expressBasicAuth from 'express-basic-auth'

import { enduserGwRouter } from './enduser/app.js'
import { internalGwRouter } from './internal/app.js'
import { integrationUserHeader } from './shared/auth/index.js'
import { appCommit, Config, titaniaConfig } from './shared/config.js'
import { cacheControl } from './shared/middleware/cache-control.js'
import { errorHandler } from './shared/middleware/error-handler.js'
import { createProxy } from './shared/proxy-utils.js'
import { RedisClient } from './shared/redis-client.js'
import { handleCspReport } from './shared/routes/csp.js'
import { sessionSupport } from './shared/session.js'

export function apiRouter(config: Config, redisClient: RedisClient) {
  const router = express.Router()
  router.use((req, _, next) => {
    if (req.url === '/application/version' || req.url === '/internal/version') {
      req.url = '/version'
    } else if (req.url.startsWith('/internal/integration/')) {
      req.url = req.url.replace('/internal/integration/', '/integration/')
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

  const citizenSessions = sessionSupport('enduser', redisClient, config.citizen)
  const internalSessions = sessionSupport(
    'employee',
    redisClient,
    config.employee
  )

  router.use(
    '/application',
    enduserGwRouter(config, redisClient, { citizenSessions })
  )
  router.use(
    '/internal',
    internalGwRouter(config, redisClient, { internalSessions })
  )

  const integrationUsers = {
    ...(titaniaConfig && {
      [titaniaConfig.username]: titaniaConfig.password
    })
  }
  router.all(
    '/integration/*',
    expressBasicAuth({ users: integrationUsers }),
    createProxy({ getUserHeader: (_) => integrationUserHeader })
  )

  // global error middleware
  router.use(errorHandler(false))
  return router
}
