// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express from 'express'
import { Router } from 'express'
import bodyParser from 'body-parser'
import cookieParser from 'cookie-parser'
import {
  Issuer,
  Strategy,
  StrategyVerifyCallbackUserInfo,
  UserinfoResponse
} from 'openid-client'
import setupLoggingMiddleware from '../shared/logging'
import { enableDevApi } from '../shared/config'
import { errorHandler } from '../shared/middleware/error-handler'
import csp from '../shared/routes/csp'
import { authenticate } from '../shared/auth'
import userDetails from '../shared/routes/auth/status'
import { createSamlAuthEndpoints } from '../shared/routes/auth/espoo-ad'
import session, { refreshLogoutToken } from '../shared/session'
import passport from 'passport'
import { csrf } from '../shared/middleware/csrf'
import { trustReverseProxy } from '../shared/reverse-proxy'
import { createProxy } from '../shared/proxy-utils'
import nocache from 'nocache'
import helmet from 'helmet'
import tracing from '../shared/middleware/tracing'
import { getOrCreateEmployee } from '../shared/service/pis'
import { SamlUser } from '../shared/routes/auth/saml/types'

const app = express()
trustReverseProxy(app)
app.set('etag', false)
app.use(nocache())
app.use(
  helmet({
    // Content-Security-Policy is set by the nginx proxy
    contentSecurityPolicy: false
  })
)
app.get('/health', (req, res) => res.status(200).json({ status: 'UP' }))
app.use(tracing)
app.use(bodyParser.json({ limit: '8mb' }))
app.use(cookieParser())

Issuer.discover(
  'http://localhost:8080/auth/realms/Evaka/.well-known/openid-configuration'
).then((issuer) => {
  const client = new issuer.Client({
    client_id: 'evaka',
    client_secret: 'c7397b07-4cd0-454f-9654-23b55c9cbebb',
    redirect_uris: ['http://localhost:9093/api/internal/auth/oidc/callback'],
    post_logout_redirect_uris: ['http://localhost:3000/logout/callback'], // todo
    token_endpoint_auth_method: 'client_secret_post'
  })

  app.use(session('employee'))
  app.use(passport.initialize())
  app.use(passport.session())

  passport.serializeUser((user, done) => done(null, user))
  passport.deserializeUser((user, done) => done(null, user))

  async function verifyProfile(profile: UserinfoResponse): Promise<SamlUser> {
    const person = await getOrCreateEmployee({
      aad: profile.sub,
      firstName: profile.given_name ?? '',
      lastName: profile.family_name ?? '',
      email: profile.email ?? ''
    })
    return {
      id: person.id,
      roles: person.roles
    }
  }

  const strategyCallback: StrategyVerifyCallbackUserInfo<object> = (
    tokenSet,
    userinfo,
    done
  ) => {
    verifyProfile(userinfo)
      .then((user) => done(null, user))
      .catch(done)
  }
  passport.use('oidc', new Strategy({ client }, strategyCallback))

  app.use(refreshLogoutToken('employee'))
  setupLoggingMiddleware(app)

  app.use('/api/csp', csp)
  app.use('/api/internal', internalApiRouter())
})

function scheduledApiRouter() {
  const router = Router()
  router.all('*', (req, res) => res.sendStatus(404))
  return router
}

function internalApiRouter() {
  const router = Router()
  router.use('/scheduled', scheduledApiRouter())
  router.use(createSamlAuthEndpoints('employee'))

  // oidc auth endpoints
  router.get('/auth/oidc/login', (req, res, next) => {
    passport.authenticate('oidc')(req, res, next)
  })
  router.get('/auth/oidc/callback', (req, res, next) => {
    passport.authenticate('oidc', {
      successRedirect: '/employee',
      failureRedirect: '/'
    })(req, res, next)
  })

  if (enableDevApi) {
    router.use(
      '/dev-api',
      createProxy({ path: ({ path }) => `/dev-api${path}` })
    )
  }

  router.use(userDetails('employee'))
  router.use(authenticate)
  router.use(csrf)
  router.post('/attachments', createProxy({ multipart: true }))
  router.use(createProxy())
  router.use(errorHandler(true))
  return router
}

export default app
