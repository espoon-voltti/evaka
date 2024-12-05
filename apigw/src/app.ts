// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import cookieParser from 'cookie-parser'
import express from 'express'
import expressBasicAuth from 'express-basic-auth'

import { createDevSfiRouter } from './enduser/dev-sfi-auth.js'
import { authenticateKeycloakCitizen } from './enduser/keycloak-citizen-saml.js'
import mapRoutes from './enduser/mapRoutes.js'
import { authStatus } from './enduser/routes/auth-status.js'
import { authWeakLogin } from './enduser/routes/auth-weak-login.js'
import { authenticateSuomiFi } from './enduser/suomi-fi-saml.js'
import { internalGwRouter } from './internal/app.js'
import { devApiE2ESignup } from './internal/mobile-device-session.js'
import { integrationUserHeader } from './shared/auth/index.js'
import {
  appCommit,
  Config,
  enableDevApi,
  titaniaConfig
} from './shared/config.js'
import { cacheControl } from './shared/middleware/cache-control.js'
import { csrf } from './shared/middleware/csrf.js'
import { errorHandler } from './shared/middleware/error-handler.js'
import { createProxy } from './shared/proxy-utils.js'
import { RedisClient } from './shared/redis-client.js'
import { handleCspReport } from './shared/routes/csp.js'
import createSamlRouter from './shared/routes/saml.js'
import { createSamlConfig } from './shared/saml/index.js'
import redisCacheProvider from './shared/saml/node-saml-cache-redis.js'
import { sessionSupport } from './shared/session.js'

export function apiRouter(config: Config, redisClient: RedisClient) {
  const router = express.Router()
  router.use((req, _, next) => {
    if (req.url === '/application/version' || req.url === '/internal/version') {
      req.url = '/version'
    } else if (req.url.startsWith('/internal/integration/')) {
      req.url = req.url.replace('/internal/integration/', '/integration/')
    } else if (req.url.startsWith('/application/citizen/')) {
      req.url = req.url.replace('/application/citizen/', '/citizen/')
    } else if (req.url.startsWith('/application/map-api/')) {
      req.url = req.url.replace(
        '/application/map-api/',
        '/citizen/public/map-api/'
      )
    } else if (req.url.startsWith('/application/auth/saml/')) {
      req.url = req.url.replace('/application/auth/saml/', '/citizen/auth/sfi/')
    } else if (req.url.startsWith('/application/auth/evaka-customer/')) {
      req.url = req.url.replace(
        '/application/auth/evaka-customer/',
        '/citizen/auth/keycloak/'
      )
    } else if (req.url.startsWith('/application/auth/')) {
      req.url = req.url.replace('/application/auth/', '/citizen/auth/')
    }
    next()
  })
  router.use(
    cacheControl((req) =>
      req.path.startsWith('/api/citizen/child-images/') ||
      req.path.startsWith('/api/employee-mobile/child-images/')
        ? 'allow-cache'
        : 'forbid-cache'
    )
  )

  router.post(
    '/csp/report',
    express.json({ type: 'application/csp-report' }),
    handleCspReport
  )
  router.get('/version', (_, res) => {
    res.send({ commitId: appCommit })
  })

  const citizenSessions = sessionSupport('enduser', redisClient, config.citizen)
  const citizenProxy = createProxy({
    getUserHeader: (req) => citizenSessions.getUserHeader(req)
  })
  const internalSessions = sessionSupport(
    'employee',
    redisClient,
    config.employee
  )

  if (config.sfi.type === 'mock') {
    router.use(
      '/citizen/auth/sfi/',
      citizenSessions.middleware,
      createDevSfiRouter(citizenSessions)
    )
  } else if (config.sfi.type === 'saml') {
    router.use(
      '/citizen/auth/sfi/',
      citizenSessions.middleware,
      createSamlRouter({
        sessions: citizenSessions,
        strategyName: 'suomifi',
        saml: new SAML(
          createSamlConfig(
            config.sfi.saml,
            redisCacheProvider(redisClient, { keyPrefix: 'suomifi-saml-resp:' })
          )
        ),
        authenticate: authenticateSuomiFi,
        defaultPageUrl: '/'
      })
    )
  }

  if (!config.keycloakCitizen)
    throw new Error('Missing Keycloak SAML configuration (citizen)')
  router.use(
    '/citizen/auth/keycloak/',
    citizenSessions.middleware,
    createSamlRouter({
      sessions: citizenSessions,
      strategyName: 'evaka-customer',
      saml: new SAML(
        createSamlConfig(
          config.keycloakCitizen,
          redisCacheProvider(redisClient, { keyPrefix: 'customer-saml-resp:' })
        )
      ),
      authenticate: authenticateKeycloakCitizen,
      defaultPageUrl: '/'
    })
  )

  if (enableDevApi) {
    router.get(
      '/dev-api/auth/mobile-e2e-signup',
      internalSessions.middleware,
      cookieParser(config.employee.cookieSecret),
      devApiE2ESignup(internalSessions)
    )
    router.use(
      '/dev-api',
      createProxy({
        getUserHeader: () => undefined
      })
    )
  }

  router.use(
    '/citizen/public/map-api',
    csrf,
    citizenSessions.middleware,
    mapRoutes
  )
  router.all(
    '/citizen/public/*',
    csrf,
    citizenSessions.middleware,
    citizenProxy
  )
  router.get(
    '/citizen/auth/status',
    csrf,
    citizenSessions.middleware,
    authStatus(citizenSessions)
  )
  router.post(
    '/citizen/auth/weak-login',
    csrf,
    citizenSessions.middleware,
    express.json(),
    authWeakLogin(citizenSessions, redisClient)
  )
  router.all(
    '/citizen/*',
    csrf,
    citizenSessions.middleware,
    citizenSessions.requireAuthentication,
    citizenProxy
  )
  router.use(
    '/internal',
    internalGwRouter(config, redisClient, { internalSessions })
  )

  const integrationUsers = {
    ...(titaniaConfig && {
      [titaniaConfig.username]: titaniaConfig.password
    })
  }
  router.all(
    '/integration/*',
    expressBasicAuth({ users: integrationUsers }),
    createProxy({ getUserHeader: (_) => integrationUserHeader })
  )

  // global error middleware
  router.use(errorHandler(false))
  return router
}
