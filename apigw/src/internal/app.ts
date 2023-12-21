// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import cookieParser from 'cookie-parser'
import express from 'express'
import passport from 'passport'
import { requireAuthentication } from '../shared/auth/index.js'
import { createAdSamlStrategy } from './ad-saml.js'
import { createKeycloakEmployeeSamlStrategy } from './keycloak-employee-saml.js'
import {
  appCommit,
  Config,
  enableDevApi,
  titaniaConfig
} from '../shared/config.js'
import { csrf, csrfCookie } from '../shared/middleware/csrf.js'
import { errorHandler } from '../shared/middleware/error-handler.js'
import { createProxy } from '../shared/proxy-utils.js'
import createSamlRouter from '../shared/routes/saml.js'
import { sessionSupport } from '../shared/session.js'
import mobileDeviceSession, {
  checkMobileEmployeeIdToken,
  devApiE2ESignup,
  pinLoginRequestHandler,
  pinLogoutRequestHandler,
  refreshMobileSession
} from './mobile-device-session.js'
import authStatus from './routes/auth-status.js'
import expressBasicAuth from 'express-basic-auth'
import { cacheControl } from '../shared/middleware/cache-control.js'
import { createSamlConfig } from '../shared/saml/index.js'
import redisCacheProvider from '../shared/saml/passport-saml-cache-redis.js'
import { createDevAdRouter } from './dev-ad-auth.js'
import { RedisClient } from '../shared/redis-client.js'

export function internalGwRouter(
  config: Config,
  redisClient: RedisClient
): express.Router {
  const router = express.Router()

  const sessions = sessionSupport('employee', redisClient, config.employee)

  router.use(sessions.middleware)
  router.use(passport.session())
  router.use(cookieParser(config.employee.cookieSecret))

  router.use(
    cacheControl((req) =>
      req.path.startsWith('/child-images/') ? 'allow-cache' : 'forbid-cache'
    )
  )

  router.all('/system/*', (_, res) => res.sendStatus(404))

  const integrationUsers = {
    ...(titaniaConfig && {
      [titaniaConfig.username]: titaniaConfig.password
    })
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
    router.use('/auth/saml', createDevAdRouter(sessions))
  } else if (config.ad.type === 'saml') {
    router.use(
      '/auth/saml',
      createSamlRouter({
        sessions,
        strategyName: 'ead',
        strategy: createAdSamlStrategy(
          sessions,
          config.ad,
          createSamlConfig(
            config.ad.saml,
            redisCacheProvider(redisClient, { keyPrefix: 'ad-saml-resp:' })
          )
        )
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
      sessions,
      strategyName: 'evaka',
      strategy: createKeycloakEmployeeSamlStrategy(
        sessions,
        keycloakEmployeeConfig
      )
    })
  )

  if (enableDevApi) {
    router.use('/dev-api', createProxy({ path: ({ url }) => `/dev-api${url}` }))

    router.get('/auth/mobile-e2e-signup', devApiE2ESignup)
  }

  router.post('/auth/mobile', express.json(), mobileDeviceSession)

  router.use(checkMobileEmployeeIdToken(redisClient))

  router.get(
    '/auth/status',
    refreshMobileSession,
    csrf,
    csrfCookie('employee'),
    authStatus(sessions)
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
  router.use(errorHandler(true))
  return router
}
