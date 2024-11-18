// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import cookieParser from 'cookie-parser'
import express from 'express'
import passport from 'passport'

import { requireAuthentication } from '../shared/auth/index.js'
import { appCommit, Config } from '../shared/config.js'
import { cacheControl } from '../shared/middleware/cache-control.js'
import { csrf } from '../shared/middleware/csrf.js'
import { errorHandler } from '../shared/middleware/error-handler.js'
import { createProxy } from '../shared/proxy-utils.js'
import { RedisClient } from '../shared/redis-client.js'
import createSamlRouter from '../shared/routes/saml.js'
import { createSamlConfig } from '../shared/saml/index.js'
import redisCacheProvider from '../shared/saml/node-saml-cache-redis.js'
import { sessionSupport } from '../shared/session.js'

import { createDevSfiRouter } from './dev-sfi-auth.js'
import { authenticateKeycloakCitizen } from './keycloak-citizen-saml.js'
import mapRoutes from './mapRoutes.js'
import authStatus from './routes/auth-status.js'
import { authenticateSuomiFi } from './suomi-fi-saml.js'

export function enduserGwRouter(
  config: Config,
  redisClient: RedisClient
): express.Router {
  const router = express.Router()

  const sessions = sessionSupport('enduser', redisClient, config.citizen)

  router.use(sessions.middleware)
  router.use(passport.session())
  router.use(cookieParser())

  router.use(
    cacheControl((req) =>
      req.path.startsWith('/citizen/child-images/')
        ? 'allow-cache'
        : 'forbid-cache'
    )
  )

  router.get('/version', (_, res) => {
    res.send({ commitId: appCommit })
  })
  router.all('/citizen/public/*', createProxy())
  router.use(mapRoutes)

  if (config.sfi.type === 'mock') {
    router.use('/auth/saml', createDevSfiRouter(sessions))
  } else if (config.sfi.type === 'saml') {
    router.use(
      '/auth/saml',
      createSamlRouter({
        sessions,
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
    '/auth/evaka-customer',
    createSamlRouter({
      sessions,
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
  router.get('/auth/status', authStatus)
  router.use(requireAuthentication)
  router.use(csrf)
  router.all('/citizen/*', createProxy())
  router.use(errorHandler(false))
  return router
}
