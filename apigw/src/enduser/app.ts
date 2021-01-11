// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express, { Router } from 'express'
import bodyParser from 'body-parser'
import cookieParser from 'cookie-parser'
import passport from 'passport'
import publicRoutes from './publicRoutes'
import routes from './routes'
import { createAuthEndpoints } from '../shared/routes/auth/suomi-fi'
import { errorHandler } from '../shared/middleware/error-handler'
import { requireAuthentication } from '../shared/auth'
import session, { refreshLogoutToken } from '../shared/session'
import setupLoggingMiddleware from '../shared/logging'
import { csrf, csrfCookie } from '../shared/middleware/csrf'
import { trustReverseProxy } from '../shared/reverse-proxy'
import nocache from 'nocache'
import helmet from 'helmet'
import tracing from '../shared/middleware/tracing'
import authStatus from './routes/auth-status'

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
app.use(bodyParser.json())
app.use(cookieParser())
app.use(session('enduser'))
app.use(passport.initialize())
app.use(passport.session())
passport.serializeUser<Express.User>((user, done) => done(null, user))
passport.deserializeUser<Express.User>((user, done) => done(null, user))
app.use(refreshLogoutToken('enduser'))
setupLoggingMiddleware(app)

function apiRouter() {
  const router = Router()

  router.use(publicRoutes)
  router.use(createAuthEndpoints('enduser'))
  router.get('/auth/status', csrf, csrfCookie('enduser'), authStatus)
  router.use(requireAuthentication)
  router.use(csrf)
  router.use(routes)
  return router
}

app.use('/api/application', apiRouter())
app.use(errorHandler(false))

export default app
