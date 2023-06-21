// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sourceMapSupport from 'source-map-support'
import {
  configFromEnv,
  gatewayRole,
  httpPort,
  toRedisClientOpts
} from './shared/config.js'
import './tracer.js'
import { logError, logInfo } from './shared/logging.js'
import enduserGwApp from './enduser/app.js'
import internalGwApp from './internal/app.js'
import * as redis from 'redis'

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

if (!gatewayRole || gatewayRole === 'enduser') {
  const app = enduserGwApp(config, redisClient)
  app.listen(httpPort.enduser, () =>
    logInfo(
      `Evaka Application API Gateway listening on port ${httpPort.enduser}`
    )
  )
}
if (!gatewayRole || gatewayRole === 'internal') {
  const app = internalGwApp(config, redisClient)
  const server = app.listen(httpPort.internal, () =>
    logInfo(`Evaka Internal API Gateway listening on port ${httpPort.internal}`)
  )

  server.keepAliveTimeout = 70 * 1000
  server.headersTimeout = 75 * 1000
}
