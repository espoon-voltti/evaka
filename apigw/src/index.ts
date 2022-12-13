// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'source-map-support/register'
import { gatewayRole, httpPort } from './shared/config'
import './tracer'
import { logInfo } from './shared/logging'
import enduserGwApp from './enduser/app'
import internalGwApp from './internal/app'
import { createRedisClient } from './shared/redis-client'

const redisClient = createRedisClient()

if (!gatewayRole || gatewayRole === 'enduser') {
  const app = enduserGwApp(redisClient)
  app.listen(httpPort.enduser, () =>
    logInfo(
      `Evaka Application API Gateway listening on port ${httpPort.enduser}`
    )
  )
}
if (!gatewayRole || gatewayRole === 'internal') {
  const app = internalGwApp(redisClient)
  const server = app.listen(httpPort.internal, () =>
    logInfo(`Evaka Internal API Gateway listening on port ${httpPort.internal}`)
  )

  server.keepAliveTimeout = 70 * 1000
  server.headersTimeout = 75 * 1000
}
