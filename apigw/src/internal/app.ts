// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import cookieParser from 'cookie-parser'
import express from 'express'
import expressBasicAuth from 'express-basic-auth'

import { integrationUserHeader } from '../shared/auth/index.js'
import { Config, enableDevApi, titaniaConfig } from '../shared/config.js'
import { csrf } from '../shared/middleware/csrf.js'
import { errorHandler } from '../shared/middleware/error-handler.js'
import { createProxy } from '../shared/proxy-utils.js'
import { RedisClient } from '../shared/redis-client.js'
import createSamlRouter from '../shared/routes/saml.js'
import { createSamlConfig } from '../shared/saml/index.js'
import redisCacheProvider from '../shared/saml/node-saml-cache-redis.js'
import { sessionSupport } from '../shared/session.js'

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
  redisClient: RedisClient
): express.Router {
  const router = express.Router()

  const sessions = sessionSupport('employee', redisClient, config.employee)
  const getUserHeader = (req: express.Request) => sessions.getUserHeader(req)

  // middlewares
  router.use(sessions.middleware)
  router.use(cookieParser(config.employee.cookieSecret))
  router.use(checkMobileEmployeeIdToken(sessions, redisClient))

  router.all('/system/*', (_, res) => res.sendStatus(404))

  const integrationUsers = {
    ...(titaniaConfig && {
      [titaniaConfig.username]: titaniaConfig.password
    })
  }
  router.use('/integration', expressBasicAuth({ users: integrationUsers }))
  router.all(
    '/integration/*',
    createProxy({ getUserHeader: (_) => integrationUserHeader })
  )

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
      sessions,
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

    router.get('/auth/mobile-e2e-signup', devApiE2ESignup(sessions))
  }

  // CSRF checks apply to all the API endpoints that frontend uses
  router.use(csrf)

  // public endpoints
  router.all('/employee/public/*', createProxy({ getUserHeader }))
  router.all('/employee-mobile/public/*', createProxy({ getUserHeader }))
  router.get(
    '/auth/status',
    refreshMobileSession(sessions),
    authStatus(sessions)
  )
  router.post('/auth/mobile', express.json(), mobileDeviceSession(sessions))

  // authenticated endpoints
  router.use(sessions.requireAuthentication)
  router.post(
    '/auth/pin-login',
    express.json(),
    pinLoginRequestHandler(sessions, redisClient)
  )
  router.post(
    '/auth/pin-logout',
    express.json(),
    pinLogoutRequestHandler(sessions, redisClient)
  )
  router.all('/employee/*', createProxy({ getUserHeader }))
  router.all('/employee-mobile/*', createProxy({ getUserHeader }))

  // global error middleware
  router.use(errorHandler(true))
  return router
}
