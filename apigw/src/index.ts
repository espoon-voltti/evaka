// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import './tracer.js'

import sourceMapSupport from 'source-map-support'
import {
  configFromEnv,
  gatewayRole,
  httpPort,
  toRedisClientOpts
} from './shared/config.js'
import { logError, loggingMiddleware, logInfo } from './shared/logging.js'
import { enduserGwRouter } from './enduser/app.js'
import { internalGwRouter } from './internal/app.js'
import * as redis from 'redis'
import csp from './shared/routes/csp.js'
import { fallbackErrorHandler } from './shared/middleware/error-handler.js'
import express from 'express'
import { trustReverseProxy } from './shared/reverse-proxy.js'
import helmet from 'helmet'
import { assertRedisConnection } from './shared/redis-client.js'
import tracing from './shared/middleware/tracing.js'
import passport from 'passport'

sourceMapSupport.install()
const config = configFromEnv()

const redisClient = redis.createClient(toRedisClientOpts(config))
redisClient.on('error', (err) =>
  logError('Redis error', undefined, undefined, err)
)
redisClient.connect().catch((err) => {
  logError('Unable to connect to redis', undefined, undefined, err)
})
// Don't prevent the app from exiting if a redis connection is alive.
redisClient.unref()

const app = express()
trustReverseProxy(app)
app.set('etag', false)

app.use(
  helmet({
    // Content-Security-Policy is set by the nginx proxy
    contentSecurityPolicy: false
  })
)
app.get('/health', (_, res) => {
  assertRedisConnection(redisClient)
    .then(() => {
      res.status(200).json({ status: 'UP' })
    })
    .catch(() => {
      res.status(503).json({ status: 'DOWN' })
    })
})
app.use(tracing)
app.use(loggingMiddleware)

passport.serializeUser<Express.User>((user, done) => done(null, user))
passport.deserializeUser<Express.User>((user, done) => done(null, user))

if (!gatewayRole || gatewayRole === 'enduser') {
  app.use('/api/application', enduserGwRouter(config, redisClient))
}
if (!gatewayRole || gatewayRole === 'internal') {
  app.use('/api/csp', csp)
  app.use('/api/internal', internalGwRouter(config, redisClient))
}
app.use(fallbackErrorHandler)

const server = app.listen(httpPort, () =>
  logInfo(
    `Evaka API Gateway (role ${gatewayRole}) listening on port ${httpPort}`
  )
)
server.keepAliveTimeout = 70 * 1000
server.headersTimeout = 75 * 1000
