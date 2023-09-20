// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import { trustReverseProxy } from './reverse-proxy.js'
import helmet from 'helmet'
import { assertRedisConnection, RedisClient } from './redis-client.js'
import tracing from './middleware/tracing.js'
import passport from 'passport'
import { loggingMiddleware } from './logging.js'

export function configureApp(redisClient: RedisClient, app: express.Express) {
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
  passport.serializeUser<Express.User>((user, done) => done(null, user))
  passport.deserializeUser<Express.User>((user, done) => done(null, user))
  app.use(loggingMiddleware)
}
