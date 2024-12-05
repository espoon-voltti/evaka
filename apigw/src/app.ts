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
import { citizenAuthStatus } from './enduser/routes/auth-status.js'
import { authWeakLogin } from './enduser/routes/auth-weak-login.js'
import { authenticateSuomiFi } from './enduser/suomi-fi-saml.js'
import { authenticateAd } from './internal/ad-saml.js'
import { createDevAdRouter } from './internal/dev-ad-auth.js'
import { authenticateKeycloakEmployee } from './internal/keycloak-employee-saml.js'
import {
  checkMobileEmployeeIdToken,
  devApiE2ESignup,
  mobileDeviceSession,
  pinLoginRequestHandler,
  pinLogoutRequestHandler,
  refreshMobileSession
} from './internal/mobile-device-session.js'
import { internalAuthStatus } from './internal/routes/auth-status.js'
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
    } else if (req.url.startsWith('/internal/employee/')) {
      req.url = req.url.replace('/internal/employee/', '/employee/')
    } else if (req.url.startsWith('/internal/employee-mobile/')) {
      req.url = req.url.replace(
        '/internal/employee-mobile/',
        '/employee-mobile/'
      )
    } else if (req.url.startsWith('/internal/auth/saml/')) {
      req.url = req.url.replace('/internal/auth/saml/', '/employee/auth/ad/')
    } else if (req.url.startsWith('/internal/auth/evaka/')) {
      req.url = req.url.replace(
        '/internal/auth/evaka/',
        '/auth/keycloak-employee/'
      )
    } else if (req.url === '/internal/auth/mobile') {
      req.url = '/employee-mobile/auth/finish-pairing'
    } else if (req.url === '/internal/auth/pin-login') {
      req.url = '/employee-mobile/auth/pin-login'
    } else if (req.url === '/internal/auth/pin-logout') {
      req.url = '/employee-mobile/auth/pin-logout'
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

  const citizenSessions = sessionSupport('enduser', redisClient, config.citizen)
  const citizenProxy = createProxy({
    getUserHeader: (req) => citizenSessions.getUserHeader(req)
  })
  const internalSessions = sessionSupport(
    'employee',
    redisClient,
    config.employee
  )
  const internalProxy = createProxy({
    getUserHeader: (req) => internalSessions.getUserHeader(req)
  })

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

  router.all('/employee/auth/ad/*', (req: express.Request, _, next) => {
    // horrible hack to fix logout URL
    if (req.session?.idpProvider === 'evaka') {
      req.url = req.url.replace(
        '/employee/auth/ad/',
        '/employee/auth/keycloak/'
      )
    }
    next()
  })

  if (config.ad.type === 'mock') {
    router.use(
      '/auth/entraid',
      internalSessions.middleware,
      createDevAdRouter(internalSessions)
    )
  } else if (config.ad.type === 'saml') {
    router.use(
      '/auth/entraid',
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
    '/auth/keycloak-employee',
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
    citizenAuthStatus(citizenSessions)
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

  router.get(
    '/internal/auth/status',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    refreshMobileSession(internalSessions),
    internalAuthStatus(internalSessions)
  )
  router.get(
    '/employee/auth/status',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    internalAuthStatus(internalSessions)
  )
  router.all(
    '/employee/public/*',
    csrf,
    internalSessions.middleware,
    internalProxy
  )
  router.all(
    '/employee/*',
    csrf,
    internalSessions.middleware,
    internalSessions.requireAuthentication,
    internalProxy
  )

  router.get(
    '/employee-mobile/auth/status',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    internalAuthStatus(internalSessions)
  )
  router.post(
    '/employee-mobile/auth/finish-pairing',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    express.json(),
    mobileDeviceSession(internalSessions)
  )
  router.post(
    '/employee-mobile/auth/pin-login',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    internalSessions.requireAuthentication,
    express.json(),
    pinLoginRequestHandler(internalSessions, redisClient)
  )
  router.post(
    '/employee-mobile/auth/pin-logout',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    internalSessions.requireAuthentication,
    express.json(),
    pinLogoutRequestHandler(internalSessions, redisClient)
  )
  router.all(
    '/employee-mobile/public/*',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    internalProxy
  )
  router.all(
    '/employee-mobile/*',
    csrf,
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    internalSessions.requireAuthentication,
    internalProxy
  )

  // global error middleware
  router.use(errorHandler(false))
  return router
}
