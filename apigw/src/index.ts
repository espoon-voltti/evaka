// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import './tracer.ts'

import * as redis from '@redis/client'
import express from 'express'
import helmet from 'helmet'

import { apiRouter } from './app.ts'
import { configFromEnv, httpPort, toRedisClientOpts } from './shared/config.ts'
import {
  logError,
  loggingMiddleware,
  logInfo,
  logWarn
} from './shared/logging.ts'
import { fallbackErrorHandler } from './shared/middleware/error-handler.ts'
import tracing from './shared/middleware/tracing.ts'
import { assertRedisConnection } from './shared/redis-client.ts'
import { trustReverseProxy } from './shared/reverse-proxy.ts'

const config = configFromEnv()

const deprecatedEnvVariables = ['COOKIE_SECRET', 'SFI_MOCK', 'DEV_LOGIN']
deprecatedEnvVariables.forEach((name) => {
  if (process.env[name] !== undefined) {
    logWarn(`Deprecated environment variable ${name} was specified`)
  }
})
const redisClient = redis.createClient(toRedisClientOpts(config))
redisClient.on('error', (err) =>
  // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
  logError('Redis error', undefined, undefined, err)
)
redisClient.connect().catch((err) => {
  // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
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
app.use('/api/', apiRouter(config, redisClient))
app.use(fallbackErrorHandler)

const server = app.listen(httpPort, () =>
  logInfo(`Evaka API Gateway listening on port ${httpPort}`)
)
server.keepAliveTimeout = 70 * 1000
server.headersTimeout = 75 * 1000
