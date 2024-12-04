// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { SAML } from '@node-saml/node-saml'
import express from 'express'

import { Config } from '../shared/config.js'
import { csrf } from '../shared/middleware/csrf.js'
import { createProxy } from '../shared/proxy-utils.js'
import { RedisClient } from '../shared/redis-client.js'
import createSamlRouter from '../shared/routes/saml.js'
import { createSamlConfig } from '../shared/saml/index.js'
import redisCacheProvider from '../shared/saml/node-saml-cache-redis.js'
import { sessionSupport } from '../shared/session.js'

import { createDevSfiRouter } from './dev-sfi-auth.js'
import { authenticateKeycloakCitizen } from './keycloak-citizen-saml.js'
import mapRoutes from './mapRoutes.js'
import { authStatus } from './routes/auth-status.js'
import { authWeakLogin } from './routes/auth-weak-login.js'
import { authenticateSuomiFi } from './suomi-fi-saml.js'

export function enduserGwRouter(
  config: Config,
  redisClient: RedisClient
): express.Router {
  const router = express.Router()

  const sessions = sessionSupport('enduser', redisClient, config.citizen)
  const getUserHeader = (req: express.Request) => sessions.getUserHeader(req)

  if (config.sfi.type === 'mock') {
    router.use('/auth/saml', sessions.middleware, createDevSfiRouter(sessions))
  } else if (config.sfi.type === 'saml') {
    router.use(
      '/auth/saml',
      sessions.middleware,
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
    sessions.middleware,
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

  // public endpoints
  router.all(
    '/citizen/public/*',
    csrf,
    sessions.middleware,
    createProxy({ getUserHeader })
  )
  router.use('/map-api', csrf, sessions.middleware, mapRoutes)
  router.get('/auth/status', csrf, sessions.middleware, authStatus(sessions))
  router.post(
    '/auth/weak-login',
    csrf,
    sessions.middleware,
    express.json(),
    authWeakLogin(sessions, redisClient)
  )

  // authenticated endpoints
  router.all(
    '/citizen/*',
    csrf,
    sessions.middleware,
    sessions.requireAuthentication,
    createProxy({ getUserHeader })
  )
  return router
}
