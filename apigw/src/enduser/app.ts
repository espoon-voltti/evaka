// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express, { Router } from 'express'
import bodyParser from 'body-parser'
import cookieParser from 'cookie-parser'
import passport from 'passport'
import redis from 'redis'
import publicRoutes from './publicRoutes'
import routes from './routes'
import { errorHandler } from '../shared/middleware/error-handler'
import { requireAuthentication } from '../shared/auth'
import session, { refreshLogoutToken } from '../shared/session'
import setupLoggingMiddleware, { logError } from '../shared/logging'
import { csrf, csrfCookie } from '../shared/middleware/csrf'
import { trustReverseProxy } from '../shared/reverse-proxy'
import nocache from 'nocache'
import helmet from 'helmet'
import tracing from '../shared/middleware/tracing'
import authStatus from './routes/auth-status'
import createSamlRouter from '../shared/routes/auth/saml'
import createSuomiFiStrategy from '../shared/auth/suomi-fi-saml'
import {
  nodeEnv,
  redisHost,
  redisPort,
  redisDisableSecurity,
  redisTlsServerName,
  redisPassword
} from '../shared/config'
import createEvakaCustomerSamlStrategy from '../shared/auth/customer-saml'

const app = express()
trustReverseProxy(app)
app.set('etag', false)
app.use(nocache())
app.use(
  helmet({
    // Content-Security-Policy is set by the nginx proxy
    contentSecurityPolicy: false
  })
)
app.get('/health', (req, res) => res.status(200).json({ status: 'UP' }))
app.use(tracing)
app.use(bodyParser.json())
app.use(cookieParser())
app.use(session('enduser'))
app.use(passport.initialize())
app.use(passport.session())
passport.serializeUser<Express.User>((user, done) => done(null, user))
passport.deserializeUser<Express.User>((user, done) => done(null, user))
app.use(refreshLogoutToken('enduser'))
setupLoggingMiddleware(app)

function apiRouter() {
  const redisClient =
    nodeEnv !== 'test'
      ? redis.createClient({
          host: redisHost,
          port: redisPort,
          ...(!redisDisableSecurity && {
            tls: { servername: redisTlsServerName },
            password: redisPassword
          })
        })
      : undefined

  if (redisClient) {
    redisClient.on('error', (err) =>
      logError('Redis error', undefined, undefined, err)
    )

    // don't prevent the app from exiting if a redis connection is alive
    redisClient.unref()
  }

  const router = Router()

  router.use(publicRoutes)
  router.use(
    createSamlRouter({
      strategyName: 'suomifi',
      strategy: createSuomiFiStrategy(redisClient),
      sessionType: 'enduser',
      pathIdentifier: 'saml'
    })
  )
  router.use(
    createSamlRouter({
      strategyName: 'evaka-customer',
      strategy: createEvakaCustomerSamlStrategy(redisClient),
      sessionType: 'enduser',
      pathIdentifier: 'evaka-customer'
    })
  )
  router.get('/auth/status', csrf, csrfCookie('enduser'), authStatus)
  router.use(requireAuthentication)
  router.use(csrf)
  router.use(routes)
  return router
}

app.use('/api/application', apiRouter())
app.use(errorHandler(false))

export default app
