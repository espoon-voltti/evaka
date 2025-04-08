// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import cookieParser from 'cookie-parser'
import express from 'express'
import expressBasicAuth from 'express-basic-auth'

import { createDevSfiRouter } from './enduser/dev-sfi-auth.js'
import mapRoutes from './enduser/mapRoutes.js'
import { citizenAuthStatus } from './enduser/routes/auth-status.js'
import { authWeakLogin } from './enduser/routes/auth-weak-login.js'
import {
  createCitizenSuomiFiIntegration,
  createEmployeeSuomiFiIntegration
} from './enduser/suomi-fi-saml.js'
import { createSamlAdIntegration } from './internal/ad-saml.js'
import { createDevAdRouter } from './internal/dev-ad-auth.js'
import { createDevEmployeeSfiRouter } from './internal/dev-sfi-auth.js'
import {
  checkMobileEmployeeIdToken,
  devApiE2ESignup,
  finishPairing,
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
import { toRequestHandler } from './shared/express.js'
import { cacheControl } from './shared/middleware/cache-control.js'
import { csrf } from './shared/middleware/csrf.js'
import { errorHandler } from './shared/middleware/error-handler.js'
import { createProxy } from './shared/proxy-utils.js'
import { RedisClient } from './shared/redis-client.js'
import { handleCspReport } from './shared/routes/csp.js'
import { SamlIntegration } from './shared/routes/saml.js'
import { validateRelayStateUrl } from './shared/saml/index.js'
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
    } else if (req.url === '/application/auth/status') {
      req.url = '/citizen/auth/status'
    } else if (req.url === '/application/auth/weak-login') {
      req.url = '/citizen/auth/weak-login'
    } else if (req.url.startsWith('/internal/employee/')) {
      req.url = req.url.replace('/internal/employee/', '/employee/')
    } else if (req.url.startsWith('/internal/employee-mobile/')) {
      req.url = req.url.replace(
        '/internal/employee-mobile/',
        '/employee-mobile/'
      )
    } else if (req.url.startsWith('/internal/auth/saml/')) {
      req.url = req.url.replace('/internal/auth/saml/', '/employee/auth/ad/')
    } else if (req.url === '/internal/auth/mobile') {
      req.url = '/employee-mobile/auth/finish-pairing'
    } else if (req.url === '/internal/auth/pin-login') {
      req.url = '/employee-mobile/auth/pin-login'
    } else if (req.url === '/internal/auth/pin-logout') {
      req.url = '/employee-mobile/auth/pin-logout'
    } else if (req.url === '/internal/dev-api') {
      req.url = '/dev-api/'
    }
    next()
  })
  router.use(
    cacheControl((req) =>
      req.path.startsWith('/citizen/child-images/') ||
      req.path.startsWith('/employee-mobile/child-images/')
        ? 'allow-cache'
        : 'forbid-cache'
    )
  )

  router.post(
    '/csp',
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
    '/integration/{*rest}',
    expressBasicAuth({ users: integrationUsers }),
    createProxy({ getUserHeader: (_) => integrationUserHeader })
  )

  const citizenSessions = sessionSupport('citizen', redisClient, config.citizen)
  const citizenProxy = createProxy({
    getUserHeader: (req) => citizenSessions.getUserHeader(req)
  })
  const employeeSessions = sessionSupport(
    'employee',
    redisClient,
    config.employee
  )
  const employeeProxy = createProxy({
    getUserHeader: (req) => employeeSessions.getUserHeader(req)
  })
  const employeeMobileSessions = sessionSupport(
    'employee-mobile',
    redisClient,
    config.employee
  )
  const employeeMobileProxy = createProxy({
    getUserHeader: (req) => employeeMobileSessions.getUserHeader(req)
  })

  let citizenSfiIntegration: SamlIntegration | undefined
  if (config.sfi.type === 'mock') {
    router.use('/citizen/auth/sfi', createDevSfiRouter(citizenSessions))
  } else if (config.sfi.type === 'saml') {
    citizenSfiIntegration = createCitizenSuomiFiIntegration(
      citizenSessions,
      config.sfi.saml,
      redisClient
    )
    router.use('/citizen/auth/sfi', citizenSfiIntegration.router)
  }

  let adIntegration: SamlIntegration | undefined
  if (config.ad.type === 'mock') {
    router.use('/employee/auth/ad', createDevAdRouter(employeeSessions))
  } else if (config.ad.type === 'saml') {
    adIntegration = createSamlAdIntegration(
      employeeSessions,
      config.ad,
      redisClient
    )
    router.use('/employee/auth/ad', adIntegration.router)
  }

  let employeeSfiIntegration: SamlIntegration | undefined
  if (config.sfi.type === 'mock') {
    router.use(
      '/employee/auth/sfi',
      createDevEmployeeSfiRouter(employeeSessions)
    )
  } else if (config.sfi.type === 'saml') {
    employeeSfiIntegration = createEmployeeSuomiFiIntegration(
      employeeSessions,
      config.sfi.saml,
      redisClient
    )
    router.use('/employee/auth/sfi', employeeSfiIntegration.router)
  }

  router.use(
    '/application/auth/saml',
    express.urlencoded({ extended: false }),
    (req, res, next) => {
      const relayStateUrl = validateRelayStateUrl(req)
      const hasEmployeeRelayStateUrl =
        relayStateUrl?.pathname === '/employee' ||
        relayStateUrl?.pathname.startsWith('/employee/')

      if (hasEmployeeRelayStateUrl) {
        if (employeeSfiIntegration)
          return employeeSfiIntegration.router(req, res, next)
      } else {
        if (citizenSfiIntegration)
          return citizenSfiIntegration.router(req, res, next)
      }
      res.sendStatus(404)
    }
  )

  if (enableDevApi) {
    router.get(
      '/dev-api/auth/mobile-e2e-signup',
      employeeMobileSessions.middleware,
      cookieParser(config.employee.cookieSecret),
      devApiE2ESignup(employeeMobileSessions)
    )
    router.all(
      '/dev-api/{*rest}',
      createProxy({
        getUserHeader: () => undefined
      })
    )
  }

  router.get(
    '/citizen/auth/logout',
    citizenSessions.middleware,
    toRequestHandler(async (req, res) => {
      const user = citizenSessions.getUser(req)
      switch (user?.authType) {
        case 'sfi':
          if (citizenSfiIntegration)
            return citizenSfiIntegration.logout(req, res)
          break
        case 'citizen-weak':
        case 'dev':
        case undefined:
          // no need for special handling
          break
        case 'ad':
        case 'employee-mobile':
          // should not happen, but we'll still destroy the session normally
          break
      }
      await citizenSessions.destroy(req, res)
      res.redirect('/')
    })
  )
  router.get(
    '/employee/auth/logout',
    employeeSessions.middleware,
    toRequestHandler(async (req, res) => {
      const user = employeeSessions.getUser(req)
      switch (user?.authType) {
        case 'ad':
          if (adIntegration) return adIntegration.logout(req, res)
          break
        case 'sfi':
          if (employeeSfiIntegration)
            return employeeSfiIntegration.logout(req, res)
          break
        case 'dev':
          // no need for special handling
          break
        case 'citizen-weak':
        case 'employee-mobile':
          // should not happen, but we'll still destroy the session normally
          break
      }
      await employeeSessions.destroy(req, res)
      res.redirect('/employee')
    })
  )

  // CSRF checks apply to all the API endpoints that frontend uses
  router.use(csrf)

  router.use('/citizen', citizenSessions.middleware)
  router.get('/citizen/auth/status', citizenAuthStatus(citizenSessions))
  router.post(
    '/citizen/auth/weak-login',
    express.json(),
    authWeakLogin(
      citizenSessions,
      config.citizen.weakLoginRateLimit,
      redisClient
    )
  )
  router.all('/citizen/auth/{*rest}', (_, res) => res.redirect('/'))
  router.use('/citizen/public/map-api', mapRoutes)
  router.all('/citizen/public/{*rest}', citizenProxy)
  router.all(
    '/citizen/{*rest}',
    citizenSessions.requireAuthentication,
    citizenProxy
  )

  const internalSessions = sessionSupport(
    'employee-mobile',
    redisClient,
    config.employee
  )
  router.get(
    '/internal/auth/status',
    internalSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(internalSessions, redisClient),
    refreshMobileSession(internalSessions),
    internalAuthStatus(internalSessions)
  )

  router.use('/employee/', employeeSessions.middleware)
  router.get('/employee/auth/status', internalAuthStatus(employeeSessions))
  router.all('/employee/auth/{*rest}', (_, res) => res.redirect('/employee'))
  router.all('/employee/public/{*rest}', employeeProxy)
  router.all(
    '/employee/{*rest}',
    employeeSessions.requireAuthentication,
    employeeProxy
  )

  router.use(
    '/employee-mobile/',
    employeeMobileSessions.middleware,
    cookieParser(config.employee.cookieSecret),
    checkMobileEmployeeIdToken(employeeMobileSessions, redisClient)
  )
  router.get(
    '/employee-mobile/auth/status',
    refreshMobileSession(employeeMobileSessions),
    internalAuthStatus(employeeMobileSessions)
  )
  router.post(
    '/employee-mobile/auth/finish-pairing',
    express.json(),
    finishPairing(employeeMobileSessions)
  )
  router.post(
    '/employee-mobile/auth/pin-login',
    employeeMobileSessions.requireAuthentication,
    express.json(),
    pinLoginRequestHandler(employeeMobileSessions, redisClient)
  )
  router.post(
    '/employee-mobile/auth/pin-logout',
    employeeMobileSessions.requireAuthentication,
    express.json(),
    pinLogoutRequestHandler(employeeMobileSessions, redisClient)
  )
  router.all('/employee-mobile/auth/{*rest}', (_, res) =>
    res.redirect('/employee/mobile')
  )
  router.all('/employee-mobile/public/{*rest}', employeeMobileProxy)
  router.all(
    '/employee-mobile/{*rest}',
    employeeMobileSessions.requireAuthentication,
    employeeMobileProxy
  )

  // global error middleware
  router.use(errorHandler(false))
  return router
}
