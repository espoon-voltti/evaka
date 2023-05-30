// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import cookieParser from 'cookie-parser'
import express, { Router } from 'express'
import helmet from 'helmet'
import passport from 'passport'
import { requireAuthentication } from '../shared/auth'
import { createAdSamlStrategy } from './ad-saml'
import { createKeycloakEmployeeSamlStrategy } from './keycloak-employee-saml'
import {
  appCommit,
  Config,
  cookieSecret,
  enableDevApi,
  espooBiPocPassword,
  titaniaConfig
} from '../shared/config'
import setupLoggingMiddleware from '../shared/logging'
import { csrf, csrfCookie } from '../shared/middleware/csrf'
import { errorHandler } from '../shared/middleware/error-handler'
import tracing from '../shared/middleware/tracing'
import { createProxy } from '../shared/proxy-utils'
import { trustReverseProxy } from '../shared/reverse-proxy'
import createSamlRouter from '../shared/routes/saml'
import csp from '../shared/routes/csp'
import session, {
  refreshLogoutToken,
  touchSessionMaxAge
} from '../shared/session'
import mobileDeviceSession, {
  checkMobileEmployeeIdToken,
  devApiE2ESignup,
  pinLoginRequestHandler,
  pinLogoutRequestHandler,
  refreshMobileSession
} from './mobile-device-session'
import authStatus from './routes/auth-status'
import expressBasicAuth from 'express-basic-auth'
import { cacheControl } from '../shared/middleware/cache-control'
import { createSamlConfig } from '../shared/saml'
import redisCacheProvider from '../shared/saml/passport-saml-cache-redis'
import { createDevAdRouter } from './dev-ad-auth'
import { assertRedisConnection, RedisClient } from '../shared/redis-client'

export default function internalGwApp(
  config: Config,
  redisClient: RedisClient
) {
  const app = express()
  trustReverseProxy(app)
  app.set('etag', false)

  app.use(
    cacheControl((req) =>
      req.path.startsWith('/api/internal/child-images/')
        ? 'allow-cache'
        : 'forbid-cache'
    )
  )

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
  app.use(session('employee', redisClient))
  app.use(touchSessionMaxAge)
  app.use(cookieParser(cookieSecret))
  app.use(passport.initialize())
  app.use(passport.session())
  passport.serializeUser<Express.User>((user, done) => done(null, user))
  passport.deserializeUser<Express.User>((user, done) => done(null, user))
  app.use(refreshLogoutToken())
  setupLoggingMiddleware(app)

  app.use('/api/csp', csp)

  function internalApiRouter() {
    const router = Router()
    router.all('/system/*', (_, res) => res.sendStatus(404))

    const integrationUsers = {
      ...(titaniaConfig && {
        [titaniaConfig.username]: titaniaConfig.password
      }),
      ...(espooBiPocPassword && { 'espoo-bi-poc': espooBiPocPassword })
    }
    router.use('/integration', expressBasicAuth({ users: integrationUsers }))
    router.all('/integration/*', createProxy())

    router.all('/auth/*', (req: express.Request, res, next) => {
      if (req.session?.idpProvider === 'evaka') {
        req.url = req.url.replace('saml', 'evaka')
      }
      next()
    })

    if (config.ad.type === 'mock') {
      router.use('/auth/saml', createDevAdRouter())
    } else if (config.ad.type === 'saml') {
      router.use(
        '/auth/saml',
        createSamlRouter({
          strategyName: 'ead',
          strategy: createAdSamlStrategy(
            config.ad,
            createSamlConfig(
              config.ad.saml,
              redisCacheProvider(redisClient, { keyPrefix: 'ad-saml-resp:' })
            )
          ),
          sessionType: 'employee'
        })
      )
    }

    if (!config.keycloakEmployee)
      throw new Error('Missing Keycloak SAML configuration (employee)')
    const keycloakEmployeeConfig = createSamlConfig(
      config.keycloakEmployee,
      redisCacheProvider(redisClient, { keyPrefix: 'keycloak-saml-resp:' })
    )
    router.use(
      '/auth/evaka',
      createSamlRouter({
        strategyName: 'evaka',
        strategy: createKeycloakEmployeeSamlStrategy(keycloakEmployeeConfig),
        sessionType: 'employee'
      })
    )

    if (enableDevApi) {
      router.use(
        '/dev-api',
        createProxy({ path: ({ path }) => `/dev-api${path}` })
      )

      router.get('/auth/mobile-e2e-signup', devApiE2ESignup)
    }

    router.post('/auth/mobile', express.json(), mobileDeviceSession)

    router.use(checkMobileEmployeeIdToken(redisClient))

    router.get(
      '/auth/status',
      refreshMobileSession,
      csrf,
      csrfCookie('employee'),
      authStatus
    )
    router.all('/public/*', createProxy())
    router.get('/version', (_, res) => {
      res.send({ commitId: appCommit })
    })
    router.use(requireAuthentication)
    router.use(csrf)
    router.post(
      '/auth/pin-login',
      express.json(),
      pinLoginRequestHandler(redisClient)
    )
    router.post(
      '/auth/pin-logout',
      express.json(),
      pinLogoutRequestHandler(redisClient)
    )

    router.use(createProxy())
    return router
  }

  app.use('/api/internal', internalApiRouter())
  app.use(errorHandler(true))
  return app
}
