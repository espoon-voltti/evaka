// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import cookieParser from 'cookie-parser'
import { Router } from 'express'
import passport from 'passport'
import { requireAuthentication } from '../shared/auth/index.js'
import { createSuomiFiStrategy } from './suomi-fi-saml.js'
import { csrf, csrfCookie } from '../shared/middleware/csrf.js'
import { errorHandler } from '../shared/middleware/error-handler.js'
import createSamlRouter from '../shared/routes/saml.js'
import session, {
  logoutTokenSupport,
  touchSessionMaxAge
} from '../shared/session.js'
import publicRoutes from './publicRoutes.js'
import routes from './routes.js'
import mapRoutes from './mapRoutes.js'
import authStatus from './routes/auth-status.js'
import { cacheControl } from '../shared/middleware/cache-control.js'
import { Config, sessionTimeoutMinutes } from '../shared/config.js'
import { createSamlConfig } from '../shared/saml/index.js'
import redisCacheProvider from '../shared/saml/passport-saml-cache-redis.js'
import { createDevSfiRouter } from './dev-sfi-auth.js'
import { createKeycloakCitizenSamlStrategy } from './keycloak-citizen-saml.js'
import { RedisClient } from '../shared/redis-client.js'
import { toMiddleware } from '../shared/express.js'

export function enduserGwRouter(
  config: Config,
  redisClient: RedisClient
): Router {
  const router = Router()

  const logoutTokens = logoutTokenSupport(redisClient, {
    sessionTimeoutMinutes
  })

  router.use(session('enduser', redisClient))
  router.use(touchSessionMaxAge)
  router.use(passport.session())
  router.use(cookieParser())
  router.use(toMiddleware(logoutTokens.refresh))

  router.use(
    cacheControl((req) =>
      req.path.startsWith('/citizen/child-images/')
        ? 'allow-cache'
        : 'forbid-cache'
    )
  )

  router.use(publicRoutes)
  router.use(mapRoutes)

  if (config.sfi.type === 'mock') {
    router.use('/auth/saml', createDevSfiRouter(logoutTokens))
  } else if (config.sfi.type === 'saml') {
    const suomifiSamlConfig = createSamlConfig(
      config.sfi.saml,
      redisCacheProvider(redisClient, { keyPrefix: 'suomifi-saml-resp:' })
    )
    router.use(
      '/auth/saml',
      createSamlRouter({
        logoutTokens,
        strategyName: 'suomifi',
        strategy: createSuomiFiStrategy(logoutTokens, suomifiSamlConfig),
        sessionType: 'enduser'
      })
    )
  }

  if (!config.keycloakCitizen)
    throw new Error('Missing Keycloak SAML configuration (citizen)')
  const keycloakCitizenConfig = createSamlConfig(
    config.keycloakCitizen,
    redisCacheProvider(redisClient, { keyPrefix: 'customer-saml-resp:' })
  )
  router.use(
    '/auth/evaka-customer',
    createSamlRouter({
      logoutTokens,
      strategyName: 'evaka-customer',
      strategy: createKeycloakCitizenSamlStrategy(
        logoutTokens,
        keycloakCitizenConfig
      ),
      sessionType: 'enduser'
    })
  )
  router.get('/auth/status', csrf, csrfCookie('enduser'), authStatus)
  router.use(requireAuthentication)
  router.use(csrf)
  router.use(routes)
  router.use(errorHandler(false))
  return router
}
