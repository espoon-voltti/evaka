// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import cookieParser from 'cookie-parser'
import express from 'express'
import expressBasicAuth from 'express-basic-auth'

import { createDevSfiRouter } from './enduser/dev-sfi-auth.ts'
import mapRoutes from './enduser/mapRoutes.ts'
import { citizenAuthStatus } from './enduser/routes/auth-status.ts'
import { authWeakLogin } from './enduser/routes/auth-weak-login.ts'
import {
  createCitizenSuomiFiIntegration,
  createEmployeeSuomiFiIntegration
} from './enduser/suomi-fi-saml.ts'
import { createSamlAdIntegration } from './internal/ad-saml.ts'
import { createDevAdRouter } from './internal/dev-ad-auth.ts'
import { createDevEmployeeSfiRouter } from './internal/dev-sfi-auth.ts'
import {
  checkMobileEmployeeIdToken,
  devApiE2ESignup,
  finishPairing,
  pinLoginRequestHandler,
  pinLogoutRequestHandler,
  refreshMobileSession
} from './internal/mobile-device-session.ts'
import { internalAuthStatus } from './internal/routes/auth-status.ts'
import { integrationUserHeader } from './shared/auth/index.ts'
import type { Config } from './shared/config.ts'
import { appCommit, enableDevApi, titaniaConfig } from './shared/config.ts'
import { toRequestHandler } from './shared/express.ts'
import { cacheControl } from './shared/middleware/cache-control.ts'
import { csrf } from './shared/middleware/csrf.ts'
import { errorHandler } from './shared/middleware/error-handler.ts'
import { createProxy } from './shared/proxy-utils.ts'
import type { RedisClient } from './shared/redis-client.ts'
import { handleCspReport } from './shared/routes/csp.ts'
import type { SamlIntegration } from './shared/routes/saml.ts'
import { validateRelayStateUrl } from './shared/saml/index.ts'
import { sessionSupport } from './shared/session.ts'

export function apiRouter(config: Config, redisClient: RedisClient) {
  const router = express.Router()
  router.use((req, _, next) => {
    if (req.url.startsWith('/internal/integration/titania/')) {
      // The old /internal/integration/titania/ prefix is still used by external systems
      req.url = req.url.replace(
        '/internal/integration/titania/',
        '/integration/titania/'
      )
    } else if (req.url.startsWith('/internal/auth/saml/')) {
      // Employee AD is still configured to use the old prefix
      req.url = req.url.replace('/internal/auth/saml/', '/employee/auth/ad/')
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
