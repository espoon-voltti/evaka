// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import cookieParser from 'cookie-parser'
import express from 'express'

import { Config } from '../shared/config.js'
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

  router.all('/auth/*', (req: express.Request, res, next) => {
    if (req.session?.idpProvider === 'evaka') {
      req.url = req.url.replace('saml', 'evaka')
    }
    next()
  })

  if (config.ad.type === 'mock') {
    router.use(
      '/auth/saml',
      internalSessions.middleware,
      createDevAdRouter(internalSessions)
    )
  } else if (config.ad.type === 'saml') {
    router.use(
      '/auth/saml',
      internalSessions.middleware,
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
    internalSessions.middleware,
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

  // public endpoints
  router.all(
    '/employee/public/*',
    csrf,
    internalSessions.middleware,
    createProxy({ getUserHeader })
  )
  router.all(
    '/employee-mobile/public/*',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    createProxy({ getUserHeader })
  )
  router.get(
    '/auth/status',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    refreshMobileSession(internalSessions),
    authStatus(internalSessions)
  )
  router.post(
    '/auth/mobile',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    express.json(),
    mobileDeviceSession(internalSessions)
  )

  // authenticated endpoints
  router.post(
    '/auth/pin-login',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    internalSessions.requireAuthentication,
    express.json(),
    pinLoginRequestHandler(internalSessions, redisClient)
  )
  router.post(
    '/auth/pin-logout',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    internalSessions.requireAuthentication,
    express.json(),
    pinLogoutRequestHandler(internalSessions, redisClient)
  )
  router.all(
    '/employee/*',
    csrf,
    internalSessions.middleware,
    internalSessions.requireAuthentication,
    createProxy({ getUserHeader })
  )
  router.all(
    '/employee-mobile/*',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    internalSessions.requireAuthentication,
    createProxy({ getUserHeader })
  )
  return router
}
