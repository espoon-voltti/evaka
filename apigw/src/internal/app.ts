// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import cookieParser from 'cookie-parser'
import express from 'express'

import { Config, enableDevApi } from '../shared/config.js'
import { csrf } from '../shared/middleware/csrf.js'
import { createProxy } from '../shared/proxy-utils.js'
import { RedisClient } from '../shared/redis-client.js'
import createSamlRouter from '../shared/routes/saml.js'
import { createSamlConfig } from '../shared/saml/index.js'
import redisCacheProvider from '../shared/saml/node-saml-cache-redis.js'
import { Sessions } from '../shared/session.js'

import { authenticateAd } from './ad-saml.js'
import { createDevAdRouter } from './dev-ad-auth.js'
import { authenticateKeycloakEmployee } from './keycloak-employee-saml.js'
import {
  checkMobileEmployeeIdToken,
  devApiE2ESignup,
  mobileDeviceSession,
  pinLoginRequestHandler,
  pinLogoutRequestHandler,
  refreshMobileSession
} from './mobile-device-session.js'
import authStatus from './routes/auth-status.js'

export function internalGwRouter(
  config: Config,
  redisClient: RedisClient,
  { internalSessions }: { internalSessions: Sessions }
): express.Router {
  const router = express.Router()

  const getUserHeader = (req: express.Request) =>
    internalSessions.getUserHeader(req)

  // middlewares
  router.use(internalSessions.middleware)
  router.use(cookieParser(config.employee.cookieSecret))
  router.use(checkMobileEmployeeIdToken(internalSessions, redisClient))

  router.all('/auth/*', (req: express.Request, res, next) => {
    if (req.session?.idpProvider === 'evaka') {
      req.url = req.url.replace('saml', 'evaka')
    }
    next()
  })

  if (config.ad.type === 'mock') {
    router.use('/auth/saml', createDevAdRouter(internalSessions))
  } else if (config.ad.type === 'saml') {
    router.use(
      '/auth/saml',
      createSamlRouter({
        sessions: internalSessions,
        strategyName: 'ead',
        saml: new SAML(
          createSamlConfig(
            config.ad.saml,
            redisCacheProvider(redisClient, { keyPrefix: 'ad-saml-resp:' })
          )
        ),
        authenticate: authenticateAd(config.ad),
        defaultPageUrl: '/employee'
      })
    )
  }

  if (!config.keycloakEmployee)
    throw new Error('Missing Keycloak SAML configuration (employee)')
  router.use(
    '/auth/evaka',
    createSamlRouter({
      sessions: internalSessions,
      strategyName: 'evaka',
      saml: new SAML(
        createSamlConfig(
          config.keycloakEmployee,
          redisCacheProvider(redisClient, { keyPrefix: 'keycloak-saml-resp:' })
        )
      ),
      authenticate: authenticateKeycloakEmployee,
      defaultPageUrl: '/employee'
    })
  )

  if (enableDevApi) {
    router.use(
      '/dev-api',
      createProxy({
        getUserHeader: () => undefined,
        path: ({ url }) => `/dev-api${url}`
      })
    )

    router.get('/auth/mobile-e2e-signup', devApiE2ESignup(internalSessions))
  }

  // CSRF checks apply to all the API endpoints that frontend uses
  router.use(csrf)

  // public endpoints
  router.all('/employee/public/*', createProxy({ getUserHeader }))
  router.all('/employee-mobile/public/*', createProxy({ getUserHeader }))
  router.get(
    '/auth/status',
    refreshMobileSession(internalSessions),
    authStatus(internalSessions)
  )
  router.post(
    '/auth/mobile',
    express.json(),
    mobileDeviceSession(internalSessions)
  )

  // authenticated endpoints
  router.use(internalSessions.requireAuthentication)
  router.post(
    '/auth/pin-login',
    express.json(),
    pinLoginRequestHandler(internalSessions, redisClient)
  )
  router.post(
    '/auth/pin-logout',
    express.json(),
    pinLogoutRequestHandler(internalSessions, redisClient)
  )
  router.all('/employee/*', createProxy({ getUserHeader }))
  router.all('/employee-mobile/*', createProxy({ getUserHeader }))
  return router
}
